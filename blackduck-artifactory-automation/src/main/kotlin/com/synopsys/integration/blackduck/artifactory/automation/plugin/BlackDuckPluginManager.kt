package com.synopsys.integration.blackduck.artifactory.automation.plugin

import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.BlackDuckPluginApiService
import com.synopsys.integration.blackduck.artifactory.automation.docker.DockerService
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig
import com.synopsys.integration.log.IntLogger
import com.synopsys.integration.log.Slf4jIntLogger
import org.slf4j.LoggerFactory
import java.io.File

class BlackDuckPluginManager(
    private val containerHash: String,
    pluginZipFile: File,
    blackDuckServerConfig: BlackDuckServerConfig,
    pluginLoggingLevel: String,
    private val blackDuckPluginService: BlackDuckPluginService,
    private val blackDuckPluginApiService: BlackDuckPluginApiService,
    dockerService: DockerService
) {
    private val logger: IntLogger = Slf4jIntLogger(LoggerFactory.getLogger(this.javaClass))
    private val propertiesFile: File
    private val logbackXmlFile: File

    init {
        val pluginOutputDirectory = blackDuckPluginService.installPlugin(containerHash, pluginZipFile)
        propertiesFile = File(pluginOutputDirectory, "lib/blackDuckPlugin.properties")
        logbackXmlFile = File(pluginOutputDirectory, "logback.xml")

        logger.info("Rewriting properties.")
        blackDuckPluginService.initializeProperties(containerHash, propertiesFile, blackDuckServerConfig)

        logger.info("Updating logback.xml for logger purposes.")

        val logbackXmlLocation = "${blackDuckPluginService.artifactoryEtcDirectory}/logback.xml"
        dockerService.downloadFile(containerHash, logbackXmlFile, logbackXmlLocation).waitFor()
        blackDuckPluginService.updateLogbackXml(logbackXmlFile, pluginLoggingLevel)
        dockerService.uploadFile(containerHash, logbackXmlFile, logbackXmlLocation).waitFor()

        logger.info("Starting Artifactory container.")
        dockerService.startArtifactory(containerHash).waitFor()

        blackDuckPluginService.fixPermissions(containerHash, blackDuckPluginService.dockerPluginsDirectory)
        blackDuckPluginService.fixPermissions(containerHash, logbackXmlLocation, "0644")
    }

    fun updateProperties(vararg propertyPairs: Pair<ConfigurationProperty, String>) {
        blackDuckPluginService.updateProperties(containerHash, propertiesFile, *propertyPairs)
        blackDuckPluginService.fixPermissions(containerHash, blackDuckPluginService.dockerPluginsDirectory)
        blackDuckPluginApiService.reloadPlugin()
    }

}