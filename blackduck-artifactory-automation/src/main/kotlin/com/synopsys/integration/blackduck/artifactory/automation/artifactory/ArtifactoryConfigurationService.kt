package com.synopsys.integration.blackduck.artifactory.automation.artifactory

import com.synopsys.integration.blackduck.artifactory.automation.ArtifactoryConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.system.ImportExportApiService
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.system.ImportSettings
import com.synopsys.integration.blackduck.artifactory.automation.docker.DockerService
import java.io.File

class ArtifactoryConfigurationService(private val artifactoryConfiguration: ArtifactoryConfiguration, private val importExportApiService: ImportExportApiService, private val dockerService: DockerService) {
    fun importSettings() {
        val backupDirectory = File(artifactoryConfiguration.configImportDirectory)
        val importLocation = "/opt/jfrog/artifactory/etc/blackducksoftware"
        dockerService.uploadFile(backupDirectory, importLocation)
        dockerService.chownFile("artifactory", "artifactory", importLocation)
        dockerService.chmodFile("0755", importLocation)
        val importSettings = ImportSettings(importPath = File(importLocation, backupDirectory.name).absolutePath)
        importExportApiService.importSettings(importSettings)
    }
}