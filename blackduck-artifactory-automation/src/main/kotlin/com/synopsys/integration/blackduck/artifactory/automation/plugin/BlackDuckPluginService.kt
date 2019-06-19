package com.synopsys.integration.blackduck.artifactory.automation.plugin

import com.synopsys.integration.blackduck.artifactory.automation.docker.DockerService
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty
import com.synopsys.integration.blackduck.artifactory.configuration.GeneralProperty
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig
import com.synopsys.integration.exception.IntegrationException
import com.synopsys.integration.log.Slf4jIntLogger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class BlackDuckPluginService(private val dockerService: DockerService) {
    private val logger = Slf4jIntLogger(LoggerFactory.getLogger(javaClass))

    val artifactoryEtcDirectory = "/opt/jfrog/artifactory/etc"
    val dockerPluginsDirectory = "$artifactoryEtcDirectory/plugins"

    fun installPlugin(containerHash: String, zipFile: File): File {
        logger.info("Shutting down Artifactory container.")
        dockerService.stopArtifactory(containerHash).waitFor()

        logger.info("Unzipping plugin.")
        val unzippedPluginDirectory = unzipFile(zipFile, File(zipFile.parentFile, "output"))

        logger.info("Uploading plugin files.")
        unzippedPluginDirectory.listFiles()
            .filter { !it.startsWith(".") }
            .forEach {
                dockerService.uploadFile(containerHash, it, dockerPluginsDirectory).waitFor()
            }

        return unzippedPluginDirectory
    }

    fun updateLogbackXml(xmlFile: File, loggingLevel: String) {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val document = dBuilder.parse(xmlFile)

        val synopsysLoggerElement = document.createElement("logger")
        synopsysLoggerElement.setAttribute("name", "com.synopsys")
        val synopsysLoggingLevelElement = document.createElement("level")
        synopsysLoggingLevelElement.setAttribute("value", loggingLevel)
        synopsysLoggerElement.appendChild(synopsysLoggingLevelElement)

        val blackduckLoggerElement = document.createElement("logger")
        blackduckLoggerElement.setAttribute("name", "com.blackducksoftware")
        val blackDuckLoggingLevelElement = document.createElement("level")
        blackDuckLoggingLevelElement.setAttribute("value", loggingLevel)
        blackduckLoggerElement.appendChild(blackDuckLoggingLevelElement)

        document.documentElement.appendChild(synopsysLoggerElement)
        document.documentElement.appendChild(blackduckLoggerElement)

        val transformer: Transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        transformer.transform(DOMSource(document), StreamResult(xmlFile))
    }

    fun initializeProperties(containerHash: String, propertiesFile: File, blackDuckServerConfig: BlackDuckServerConfig) {
        val credentialsOptional = blackDuckServerConfig.credentials
        var username = ""
        var password = ""
        credentialsOptional.ifPresent { credentials ->
            credentials.username.ifPresent { username = it }
            credentials.password.ifPresent { password = it }
        }

        updateProperties(
            containerHash,
            propertiesFile,
            Pair(GeneralProperty.URL, blackDuckServerConfig.blackDuckUrl.toString()),
            Pair(GeneralProperty.USERNAME, username),
            Pair(GeneralProperty.PASSWORD, password),
            Pair(GeneralProperty.API_TOKEN, blackDuckServerConfig.apiToken.orElse("")),
            Pair(GeneralProperty.TIMEOUT, blackDuckServerConfig.timeout.toString()),
            Pair(GeneralProperty.PROXY_HOST, blackDuckServerConfig.proxyInfo.host.orElse("")),
            Pair(GeneralProperty.PROXY_PORT, blackDuckServerConfig.proxyInfo.port.toString()),
            Pair(GeneralProperty.PROXY_USERNAME, blackDuckServerConfig.proxyInfo.username.orElse("")),
            Pair(GeneralProperty.PROXY_PASSWORD, blackDuckServerConfig.proxyInfo.password.orElse("")),
            Pair(GeneralProperty.TRUST_CERT, blackDuckServerConfig.isAlwaysTrustServerCertificate.toString())
        )
    }

    fun updateProperties(containerHash: String, propertiesFile: File, vararg propertyPairs: Pair<ConfigurationProperty, String>) {
        val properties = Properties()
        val propertiesInputStream = propertiesFile.inputStream()
        properties.load(propertiesInputStream)
        propertiesInputStream.close()

        propertyPairs.forEach {
            properties[it.first.key] = it.second
        }

        properties.store(FileOutputStream(propertiesFile), "Modified automation properties")

        dockerService.uploadFile(containerHash, propertiesFile, "$dockerPluginsDirectory/lib/").waitFor()
    }

    fun fixPermissions(containerHash: String, location: String, permission: String = "0755") {
        logger.info("Fixing permissions.")
        dockerService.chownFile(containerHash, "com/synopsys/integration/blackduck/artifactory/automation/artifactorys/integration/blackduck/artifactory/automation/artifactory",
            "com/synopsys/integration/blackduck/artifactory/automation/artifactorys/integration/blackduck/artifactory/automation/artifactory", location).waitFor()
        dockerService.chmodFile(containerHash, permission, location).waitFor()
    }

    private fun unzipFile(zipFile: File, outputDirectory: File): File {
        outputDirectory.deleteRecursively()
        val process = ProcessBuilder()
            .command("unzip", "-o", zipFile.canonicalPath, "-d", outputDirectory.canonicalPath)
            .inheritIO()
            .start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw IntegrationException("unzip returned a non-zero exit code: $exitCode")
        }

        return File(outputDirectory, zipFile.name.replace(".zip", ""))
    }
}