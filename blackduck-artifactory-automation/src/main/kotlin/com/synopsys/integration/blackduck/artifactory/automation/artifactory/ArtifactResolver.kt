package com.synopsys.integration.blackduck.artifactory.automation.artifactory

import com.synopsys.integration.blackduck.artifactory.automation.ArtifactoryConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.Repository
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.artifacts.ArtifactRetrievalApiService
import com.synopsys.integration.blackduck.artifactory.automation.docker.DockerService

class ArtifactResolver(private val artifactRetrievalApiService: ArtifactRetrievalApiService, private val dockerService: DockerService, private val artifactoryConfiguration: ArtifactoryConfiguration) {

    fun resolveBowerArtifact() {

    }

    fun resolvePyPiArtifact(repository: Repository, packageName: String, packageVersion: String? = null) {
        val version = if (packageVersion != null) "==$packageVersion" else ""
        dockerService.runDockerImage("pip3", "install", "$packageName$version", "--index-url=http://127.0.0.1:${artifactoryConfiguration.port}/artifactory/api/pypi/${repository.key}/simple").waitFor()
    }
}