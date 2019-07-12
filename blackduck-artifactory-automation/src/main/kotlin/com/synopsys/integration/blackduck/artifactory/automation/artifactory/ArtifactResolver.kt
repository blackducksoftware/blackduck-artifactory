package com.synopsys.integration.blackduck.artifactory.automation.artifactory

import com.synopsys.integration.bdio.model.Forge
import com.synopsys.integration.bdio.model.externalid.ExternalId
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory
import com.synopsys.integration.blackduck.artifactory.automation.ArtifactoryConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.TestablePackage
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.Repository
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.artifacts.ArtifactRetrievalApiService
import com.synopsys.integration.blackduck.artifactory.automation.docker.DockerService
import kotlin.reflect.KFunction3

class ArtifactResolver(private val artifactRetrievalApiService: ArtifactRetrievalApiService, private val dockerService: DockerService, private val artifactoryConfiguration: ArtifactoryConfiguration) {
    fun resolvePyPiArtifact(repository: Repository, externalId: ExternalId) {
        dockerService.runDockerImage("pip3", "install", "${externalId.name}==${externalId.version}", "--index-url=http://127.0.0.1:${artifactoryConfiguration.port}/artifactory/api/pypi/${repository.key}/simple").waitFor()
    }
}

object Resolvers {
    private val externalIdFactory: ExternalIdFactory = ExternalIdFactory()

    val PYPI_RESOLVER = Resolver(
        ArtifactResolver::resolvePyPiArtifact,
        listOf(
            TestablePackage(
                "f7/d2/e07d3ebb2bd7af696440ce7e754c59dd546ffe1bbe732c8ab68b9c834e61/cycler-0.10.0-py2.py3-none-any.whl",
                externalIdFactory.createNameVersionExternalId(Forge.PYPI, "Cycler", "0.10.0")
            )
        )
    )
}

data class Resolver(val resolverFunction: KFunction3<ArtifactResolver, Repository, ExternalId, Unit>, val testablePackages: List<TestablePackage>)
