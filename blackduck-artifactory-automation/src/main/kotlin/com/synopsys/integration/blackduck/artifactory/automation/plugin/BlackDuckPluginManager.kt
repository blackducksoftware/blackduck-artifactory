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

    fun installPlugin(containerHash: String) {
        blackDuckPluginService.installPlugin(containerHash, pluginZipFile, pluginOutputDirectory)

        logger.info("Rewriting properties.")
        blackDuckPluginService.initializeProperties(containerHash, propertiesFile, blackDuckServerConfig)

        logger.info("Updating logback.xml for logging purposes.")

        val logbackXmlLocation = "${blackDuckPluginService.artifactoryEtcDirectory}/logback.xml"
        dockerService.downloadFile(containerHash, logbackXmlFile, logbackXmlLocation).waitFor()
        blackDuckPluginService.updateLogbackXml(logbackXmlFile, artifactoryConfiguration.pluginLoggingLevel)
        dockerService.uploadFile(containerHash, logbackXmlFile, logbackXmlLocation).waitFor()

        logger.info("Starting Artifactory container.")
        dockerService.startArtifactory(containerHash).waitFor()

        blackDuckPluginService.fixPermissions(containerHash, blackDuckPluginService.dockerPluginsDirectory)
        blackDuckPluginService.fixPermissions(containerHash, logbackXmlLocation, "0644")
    }

    fun getProperties(containerHash: String): Properties {
        val tempFile = createTempPropertiesFile()
        dockerService.downloadFile(containerHash, tempFile, blackDuckPluginService.propertiesFile).waitFor()
        val properties = Properties()
        val propertiesInputStream = tempFile.inputStream()
        properties.load(propertiesInputStream)
        propertiesInputStream.close()

        return properties
    }

    fun setProperties(containerHash: String, properties: Properties) {
        val tempFile = createTempPropertiesFile()
        val outputStream = FileOutputStream(tempFile)
        properties.store(outputStream, "Generated properties file")
        outputStream.close()
        dockerService.uploadFile(containerHash, tempFile, blackDuckPluginService.propertiesFile).waitFor()
        blackDuckPluginService.fixPermissions(containerHash, blackDuckPluginService.dockerPluginsDirectory)
        blackDuckPluginApiService.reloadPlugin()
    }

    private fun createTempPropertiesFile(): File {
        return File.createTempFile("artifactory-automation-", "-properties")
    }
}