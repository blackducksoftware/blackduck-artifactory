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
        dockerService.buildTestDockerfile(PackageType.Defaults.BOWER)
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

    fun resolveCranArtifact(repository: Repository, externalId: ExternalId) {
        dockerService.buildTestDockerfile(PackageType.Defaults.CRAN)
        dockerService.runDockerImage(PackageType.Defaults.CRAN.dockerImageTag!!, "r", "-e",
            "install.packages('${externalId.name}', version = '${externalId.version}', repos = 'http://127.0.0.1:${artifactoryConfiguration.port}/artifactory/${repository.key}')").waitFor()
    }

    fun resolveGemsArtifact(repository: Repository, externalId: ExternalId) {
        // gem install packaging --version '0.99.35' --source http://<server:port>/artifactory/api/gems/<remote-repo-key>
        dockerService.buildTestDockerfile(PackageType.Defaults.GEMS)
        dockerService.runDockerImage(PackageType.Defaults.GEMS.dockerImageTag!!, "gem", "install", externalId.name, "--version", externalId.version, "--clear-sources", "--source",
            "http://127.0.0.1:${artifactoryConfiguration.port}/artifactory/api/gems/${repository.key}", "-V").waitFor()
    }

    fun resolveNpmArtifact(repository: Repository, externalId: ExternalId) {
        dockerService.buildTestDockerfile(PackageType.Defaults.NPM)
        dockerService.runDockerImage(PackageType.Defaults.NPM.dockerImageTag!!, "npm", "install", "${externalId.name}@${externalId.version}", "-g", "--registry",
            "http://127.0.0.1:${artifactoryConfiguration.port}/artifactory/api/npm/${repository.key}").waitFor()
    }

    fun resolveNugetArtifact(repository: Repository, externalId: ExternalId) {
        // Nuget is special and cannot find all the executables it needs when run through a java process.
        // The solution is to put the command in the Dockerfile and have them run during the build.
        val packagesJsonResource = this.javaClass.getResourceAsStream("/nuget/Dockerfile") ?: throw FileNotFoundException("Failed to find nuget Dockerfile file in resources.")
        val packageJsonText = packagesJsonResource.convertToString()
            .replace(serverReplacement, "127.0.0.1:${artifactoryConfiguration.port}")
            .replace(repoKeyReplacement, repository.key)
            .replace(dependencyNameReplacement, externalId.name)
            .replace(dependencyVersionReplacement, externalId.version)

        val outputFile = File("/tmp/artifactory-automation/nuget/Dockerfile")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(packageJsonText)
        println("Dockerfile: ${outputFile.absolutePath}")

        dockerService.buildDockerfile(outputFile, outputFile.parentFile, PackageType.Defaults.NUGET.dockerImageTag!!)
    }

    fun resolvePyPiArtifact(repository: Repository, externalId: ExternalId) {
        dockerService.buildTestDockerfile(PackageType.Defaults.PYPI)
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

    val CRAN_RESOLVER = Resolver(
        ArtifactResolver::resolveCranArtifact,
        listOf(
            TestablePackage("fortunes_1.5-4.tar.gz", externalIdFactory.createNameVersionExternalId(SupportedPackageType.CRAN.forge, "fortunes", "1.5-4"))
        )
    )

    val GEMS_RESOLVER = Resolver(
        ArtifactResolver::resolveGemsArtifact,
        listOf(
            TestablePackage("packaging-0.99.35.gem", externalIdFactory.createNameVersionExternalId(SupportedPackageType.GEMS.forge, "packaging", "0.99.35"))
        )
    )

    val NPM_RESOLVER = Resolver(
        ArtifactResolver::resolveNpmArtifact,
        listOf(
            TestablePackage("lodash-4.17.15.tgz", externalIdFactory.createNameVersionExternalId(SupportedPackageType.NPM.forge, "lodash", "4.17.15"), "lodash/lodash:4.17.15")
        )
    )

    val NUGET_RESOLVER = Resolver(
        ArtifactResolver::resolveNugetArtifact,
        listOf(
            TestablePackage("bootstrap.4.1.3.nupkg", externalIdFactory.createNameVersionExternalId(SupportedPackageType.NPM.forge, "bootstrap", "4.1.3"))
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
