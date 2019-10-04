package com.synopsys.integration.blackduck.artifactory.automation

import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.isClientError
import com.github.kittinunf.fuel.core.isServerError
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.system.SystemApiService
import com.synopsys.integration.blackduck.artifactory.automation.docker.DockerService
import com.synopsys.integration.blackduck.artifactory.automation.plugin.BlackDuckPluginManager
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig
import com.synopsys.integration.exception.IntegrationException
import com.synopsys.integration.log.IntLogger
import com.synopsys.integration.log.Slf4jIntLogger
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class Application(
        dockerService: DockerService,
        blackDuckServerConfig: BlackDuckServerConfig,
        artifactoryConfiguration: ArtifactoryConfiguration,
        blackDuckPluginManager: BlackDuckPluginManager,
        systemApiService: SystemApiService
) {
    private val logger: IntLogger = Slf4jIntLogger(LoggerFactory.getLogger(this.javaClass))

    var containerId: String

    init {
        if (!blackDuckServerConfig.canConnect(logger)) {
            throw IntegrationException("Failed to connect the Black Duck server at ${blackDuckServerConfig.blackDuckUrl}.")
        }

        val imageTag = "artifactory-automation-${artifactoryConfiguration.version}"
        if (artifactoryConfiguration.manageArtifactory) {
            logger.info("Loading Artifactory license.")
            val artifactoryLicenseFile = artifactoryConfiguration.licenseFile
            if (!artifactoryLicenseFile.exists()) {
                throw IntegrationException("You have chosen to let automation manage Artifactory, but a the ARTIFACTORY_LICENSE_PATH supplied at ${artifactoryLicenseFile.absolutePath} does not exist.")
            }
            val licenseText = FileInputStream(artifactoryLicenseFile)
                    .convertToString()
                    .replace("\n", "")
                    .replace(" ", "")

            logger.info("Validating plugin zip file.")
            val pluginZipFile = artifactoryConfiguration.pluginZipFile
            if (pluginZipFile.exists()) {
                logger.info("Plugin zip file found at ${pluginZipFile.canonicalPath}")
            } else {
                throw IntegrationException("You have chosen to let automation manage Artifactory, but the plugin zip file at ${pluginZipFile.absolutePath} does not exist.")
            }

            val artifactoryVersion = artifactoryConfiguration.version
            logger.info("Installing and starting Artifactory version: $artifactoryVersion")
            containerId = dockerService.installAndStartArtifactory(artifactoryVersion, artifactoryConfiguration.port)
            logger.info("Artifactory container: $containerId")

            logger.info("Waiting for Artifactory startup.")
            systemApiService.waitForSuccessfulStartup()

            logger.info("Applying Artifactory license.")
            systemApiService.applyLicense(licenseText)

            logger.info("Installing plugin.")
            blackDuckPluginManager.installPlugin()
            systemApiService.waitForSuccessfulStartup()

            logger.info("Successfully installed the plugin.")
            println(dockerService.getArtifactoryLogs().convertToString())
        } else {
            containerId = imageTag
            logger.info("Skipping Artifactory installation.")

            logger.info("Installing plugin.")
            blackDuckPluginManager.installPlugin()
            systemApiService.waitForSuccessfulStartup()
        }
    }
}

fun InputStream.convertToString(encoding: Charset = StandardCharsets.UTF_8): String = IOUtils.toString(this, encoding)

fun Response.validate(): Response {
    if (this.isClientError || this.isServerError || this.statusCode < 0) {
        throw IntegrationException("Status Code: ${this.statusCode}, Content: ${this}")
    }
    return this
}

fun Response.validateDontThrow(): Boolean {
    return !(this.isClientError || this.isServerError || this.statusCode < 0)
}