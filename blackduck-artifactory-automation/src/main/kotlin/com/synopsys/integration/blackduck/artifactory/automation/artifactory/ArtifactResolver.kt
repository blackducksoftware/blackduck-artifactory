package com.synopsys.integration.blackduck.artifactory.automation.artifactory

import com.synopsys.integration.bdio.model.Forge
import com.synopsys.integration.bdio.model.externalid.ExternalId
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory
import com.synopsys.integration.blackduck.artifactory.automation.ArtifactoryConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.TestablePackage
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.Repository
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.artifacts.ArtifactRetrievalApiService
import com.synopsys.integration.blackduck.artifactory.automation.convertToString
import com.synopsys.integration.blackduck.artifactory.automation.docker.DockerService
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType
import java.io.File
import java.io.FileNotFoundException
import kotlin.reflect.KFunction3

class ArtifactResolver(private val artifactRetrievalApiService: ArtifactRetrievalApiService, private val dockerService: DockerService, private val artifactoryConfiguration: ArtifactoryConfiguration) {
    private val serverReplacement = "<server>"
    private val repoKeyReplacement = "<repo-key>"
    private val dependencyNameReplacement = "<dependency-name>"
    private val dependencyVersionReplacement = "<dependency-version>"

    fun resolveBowerArtifact(repository: Repository, externalId: ExternalId) {
        dockerService.buildTestDockerfile(PackageType.Defaults.BOWER, File(""))
        dockerService.runDockerImage(
            PackageType.Defaults.BOWER.dockerImageTag!!,
            "bower", "install", "${externalId.name}#${externalId.version}", "--allow-root", "--config.registry=http://127.0.0.1:${artifactoryConfiguration.port}/artifactory/api/bower/${repository.key}"
        ).waitFor()
    }

    fun resolveComposerArtifact(repository: Repository, externalId: ExternalId) {
        val packagesJsonResource = this.javaClass.getResourceAsStream("/composer/composer.json") ?: throw FileNotFoundException("Failed to find composer.json file in resources.")
        val packageJsonText = packagesJsonResource.convertToString()
            .replace(serverReplacement, "127.0.0.1:${artifactoryConfiguration.port}")
            .replace(repoKeyReplacement, repository.key)
            .replace(dependencyNameReplacement, externalId.name)
            .replace(dependencyVersionReplacement, externalId.version)

        val outputFile = File("/tmp/artifactory-automation/composer/composer.json")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(packageJsonText)
        println("composer.json: ${outputFile.absolutePath}")

        dockerService.buildTestDockerfile(PackageType.Defaults.COMPOSER, outputFile.parentFile)

        dockerService.runDockerImage(PackageType.Defaults.COMPOSER.dockerImageTag!!, "php", "composer.phar", "install", directory = outputFile.parentFile).waitFor()
    }

    fun resolvePyPiArtifact(repository: Repository, externalId: ExternalId) {
        dockerService.buildTestDockerfile(PackageType.Defaults.PYPI, File(""))
        dockerService.runDockerImage(
            PackageType.Defaults.PYPI.dockerImageTag!!,
            "pip3", "install", "${externalId.name}==${externalId.version}", "--index-url=http://127.0.0.1:${artifactoryConfiguration.port}/artifactory/api/pypi/${repository.key}/simple"
        ).waitFor()
    }
}

object Resolvers {
    private val externalIdFactory: ExternalIdFactory = ExternalIdFactory()

    val BOWER_RESOLVER = Resolver(
        ArtifactResolver::resolveBowerArtifact,
        listOf(
            // qtip2 currently does not match in the KB due to an uppercase T in the KB and lowercase from artifactory.
            // TestablePackage("bower-v2.2.1.tar.gz", externalIdFactory.createNameVersionExternalId(SupportedPackageType.BOWER.forge, "qtip2", "2.2.1")),
            TestablePackage("bower-angular-v1.7.8.tar.gz", externalIdFactory.createNameVersionExternalId(SupportedPackageType.BOWER.forge, "angular", "1.7.8")),
            TestablePackage("angular-ui-router-bower-1.0.22.tar.gz", externalIdFactory.createNameVersionExternalId(SupportedPackageType.BOWER.forge, "angular-ui-router", "1.0.22"))
        )
    )

    val COMPOSER_RESOLVER = Resolver(
        ArtifactResolver::resolveComposerArtifact,
        listOf(
            TestablePackage("log-6c001f1daafa3a3ac1d8ff69ee4db8e799a654dd.zip", externalIdFactory.createNameVersionExternalId(SupportedPackageType.COMPOSER.forge, "psr/log", "1.1.0")),
            TestablePackage("http-message-f6561bf28d520154e4b0ec72be95418abe6d9363.zip", externalIdFactory.createNameVersionExternalId(SupportedPackageType.COMPOSER.forge, "psr/http-message", "1.0.1"))
        )
    )

    val PYPI_RESOLVER = Resolver(
        ArtifactResolver::resolvePyPiArtifact,
        listOf(
            TestablePackage("cycler-0.10.0-py2.py3-none-any.whl", externalIdFactory.createNameVersionExternalId(SupportedPackageType.PYPI.forge, "Cycler", "0.10.0")),
            TestablePackage("Click-7.0-py2.py3-none-any.whl", externalIdFactory.createNameVersionExternalId(Forge.PYPI, "click", "7.0")),
            TestablePackage("Flask-1.0.3-py2.py3-none-any.whl", externalIdFactory.createNameVersionExternalId(Forge.PYPI, "Flask", "1.0.3")),
            TestablePackage("youtube_dl-2019.5.11-py2.py3-none-any.whl", externalIdFactory.createNameVersionExternalId(Forge.PYPI, "youtube_dl", "2019.5.11"))
        )
    )
}

data class Resolver(val resolverFunction: KFunction3<ArtifactResolver, Repository, ExternalId, Unit>, val testablePackages: List<TestablePackage>)
