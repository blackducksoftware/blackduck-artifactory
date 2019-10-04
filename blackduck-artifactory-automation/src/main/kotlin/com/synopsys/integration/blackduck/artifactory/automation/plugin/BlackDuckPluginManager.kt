package com.synopsys.integration.blackduck.artifactory.automation.plugin

import com.synopsys.integration.blackduck.artifactory.automation.ArtifactoryConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.docker.DockerService
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig
import com.synopsys.integration.log.IntLogger
import com.synopsys.integration.log.Slf4jIntLogger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.util.*

class BlackDuckPluginManager(
        private val artifactoryConfiguration: ArtifactoryConfiguration,
        private val blackDuckServerConfig: BlackDuckServerConfig,
        private val blackDuckPluginService: BlackDuckPluginService,
        private val blackDuckPluginApiService: BlackDuckPluginApiService,
        private val dockerService: DockerService
) {
    private val logger: IntLogger = Slf4jIntLogger(LoggerFactory.getLogger(this.javaClass))

    private val pluginZipFile: File = artifactoryConfiguration.pluginZipFile
    private val pluginOutputDirectory: File
    private val unzippedDirectory: File
    private val propertiesFile: File
    private val logbackXmlFile: File

    init {
        pluginOutputDirectory = File(pluginZipFile.parentFile, "output")
        unzippedDirectory = File(pluginOutputDirectory, pluginZipFile.nameWithoutExtension)
        propertiesFile = File(unzippedDirectory, "lib/blackDuckPlugin.properties")
        logbackXmlFile = File(unzippedDirectory, "logback.xml")
    }

    fun installPlugin() {
        blackDuckPluginService.installPlugin(pluginZipFile, pluginOutputDirectory)

        logger.info("Rewriting properties.")
        blackDuckPluginService.initializeProperties(propertiesFile, blackDuckServerConfig)

        logger.info("Updating logback.xml for logging purposes.")

        val logbackXmlLocation = "${blackDuckPluginService.artifactoryEtcDirectory}/logback.xml"
        dockerService.downloadFile(logbackXmlFile, logbackXmlLocation)
        blackDuckPluginService.updateLogbackXml(logbackXmlFile, artifactoryConfiguration.pluginLoggingLevel)
        dockerService.uploadFile(logbackXmlFile, logbackXmlLocation)

        logger.info("Starting Artifactory container.")
        dockerService.startArtifactory()

        blackDuckPluginService.fixPermissions(blackDuckPluginService.dockerPluginsDirectory)
        blackDuckPluginService.fixPermissions(logbackXmlLocation, "0644")
    }

    fun getProperties(): Properties {
        val tempFile = createTempPropertiesFile()
        dockerService.downloadFile(tempFile, blackDuckPluginService.propertiesFile)
        val properties = Properties()
        val propertiesInputStream = tempFile.inputStream()
        properties.load(propertiesInputStream)
        propertiesInputStream.close()

        return properties
    }

    fun setProperties(properties: Properties) {
        val tempFile = createTempPropertiesFile()
        val outputStream = FileOutputStream(tempFile)
        properties.store(outputStream, "Generated properties file")
        outputStream.close()
        dockerService.uploadFile(tempFile, blackDuckPluginService.propertiesFile)
        blackDuckPluginService.fixPermissions(blackDuckPluginService.dockerPluginsDirectory)
        blackDuckPluginApiService.reloadPlugin()
    }

    private fun createTempPropertiesFile(): File {
        return File.createTempFile("artifactory-automation-", "-properties")
    }
}