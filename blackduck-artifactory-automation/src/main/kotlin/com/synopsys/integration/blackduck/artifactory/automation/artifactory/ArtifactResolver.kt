package com.synopsys.integration.blackduck.artifactory.automation.artifactory

import com.synopsys.integration.bdio.model.Forge
import com.synopsys.integration.bdio.model.externalid.ExternalId
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory
import com.synopsys.integration.blackduck.artifactory.automation.ArtifactoryConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.TestablePackage
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.artifacts.ArtifactRetrievalApiService
import com.synopsys.integration.blackduck.artifactory.automation.convertToString
import com.synopsys.integration.blackduck.artifactory.automation.docker.DockerService
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType
import java.io.File
import java.io.FileNotFoundException
import kotlin.reflect.KFunction3

class ArtifactResolver(private val artifactRetrievalApiService: ArtifactRetrievalApiService, private val dockerService: DockerService, private val artifactoryConfiguration: ArtifactoryConfiguration) {
    private val usernameReplacement = "<username>"
    private val passwordReplacement = "<password>"
    private val serverReplacement = "<server>"
    private val repoKeyReplacement = "<repo-key>"
    private val dependencyNameReplacement = "<dependency-name>"
    private val dependencyVersionReplacement = "<dependency-version>"

    fun resolveBowerArtifact(repositoryKey: String, externalId: ExternalId) {
        dockerService.buildTestDockerfile(PackageType.Defaults.BOWER)
        dockerService.runDockerImage(
                PackageType.Defaults.BOWER.dockerImageTag!!,
                "bower", "install", "${externalId.name}#${externalId.version}", "--allow-root", "--config.registry=http://127.0.0.1:${artifactoryConfiguration.port}/artifactory/api/bower/${repositoryKey}"
        )
    }

    fun resolveComposerArtifact(repositoryKey: String, externalId: ExternalId) {
        val packagesJsonResource = this.javaClass.getResourceAsStream("/composer/composer.json") ?: throw FileNotFoundException("Failed to find composer.json file in resources.")
        val packageJsonText = packagesJsonResource.convertToString().replaceArtifactoryData(repositoryKey, externalId)

        val outputFile = File("/tmp/artifactory-automation/composer/composer.json")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(packageJsonText)
        println("composer.json: ${outputFile.absolutePath}")

        dockerService.buildTestDockerfile(PackageType.Defaults.COMPOSER, outputFile.parentFile)
        dockerService.runDockerImage(PackageType.Defaults.COMPOSER.dockerImageTag!!, "php", "composer.phar", "install", directory = outputFile.parentFile)
    }

    fun resolveCondaArtifact(repositoryKey: String, externalId: ExternalId) {
        dockerService.buildTestDockerfile(PackageType.Defaults.CONDA)
        dockerService.runDockerImage(PackageType.Defaults.CONDA.dockerImageTag!!, "conda", "install", "--override-channels", "--channel", "http://127.0.0.1:${artifactoryConfiguration.port}/artifactory/api/conda/$repositoryKey",
                "${externalId.name}=${externalId.version}")
    }

    fun resolveCranArtifact(repositoryKey: String, externalId: ExternalId) {
        dockerService.buildTestDockerfile(PackageType.Defaults.CRAN)
        dockerService.runDockerImage(PackageType.Defaults.CRAN.dockerImageTag!!, "r", "-e",
                "install.packages('${externalId.name}', version = '${externalId.version}', repos = 'http://127.0.0.1:${artifactoryConfiguration.port}/artifactory/$repositoryKey')")
    }

    fun resolveGemsArtifact(repositoryKey: String, externalId: ExternalId) {
        // gem install packaging --version '0.99.35' --source http://<server:port>/artifactory/api/gems/<remote-repo-key>
        dockerService.buildTestDockerfile(PackageType.Defaults.GEMS)
        dockerService.runDockerImage(PackageType.Defaults.GEMS.dockerImageTag!!, "gem", "install", externalId.name, "--version", externalId.version, "--clear-sources", "--source",
                "http://127.0.0.1:${artifactoryConfiguration.port}/artifactory/api/gems/${repositoryKey}", "-V")
    }

    fun resolveGoArtifact(repositoryKey: String, externalId: ExternalId) {
        val outputDirectory = File("/tmp/artifactory-automation/go")
        outputDirectory.mkdirs()

        val goDockerfileResource = this.javaClass.getResourceAsStream("/go/Dockerfile") ?: throw FileNotFoundException("Failed to find go Dockerfile file in resources.")
        val goDockerfileText = goDockerfileResource.convertToString().replaceArtifactoryData(repositoryKey, externalId)

        val dockerfile = File(outputDirectory, "Dockerfile")
        dockerfile.writeText(goDockerfileText)
        println("Dockerfile: ${dockerfile.absolutePath}")

        val goModResource = this.javaClass.getResourceAsStream("/go/go.mod") ?: throw FileNotFoundException("Failed to find go.mod file in resources.")
        val goModText = goModResource.convertToString().replaceArtifactoryData(repositoryKey, externalId)

        val goModFile = File(outputDirectory, "go.mod")
        goModFile.writeText(goModText)
        println("composer.json: ${goModFile.absolutePath}")

        val helloGoResource = this.javaClass.getResourceAsStream("/go/hello.go") ?: throw FileNotFoundException("Failed to find go.mod file in resources.")
        val helloGoText = helloGoResource.convertToString()
        val helloGoFile = File(outputDirectory, "hello.go")
        helloGoFile.writeText(helloGoText)

        dockerService.buildDockerfile(dockerfile, outputDirectory, PackageType.Defaults.GO.dockerImageTag!!, noCache = true)
    }

    fun resolveMavenGradleArtifact(repositoryKey: String, externalId: ExternalId) {
        val group = externalId.group.replace(".", "/")
        val name = externalId.name
        val version = externalId.version
        artifactRetrievalApiService.retrieveArtifact(repositoryKey, "$group/$name/$version/$name-$version.jar")
    }

    fun resolveNpmArtifact(repositoryKey: String, externalId: ExternalId) {
        dockerService.buildTestDockerfile(PackageType.Defaults.NPM)
        dockerService.runDockerImage(PackageType.Defaults.NPM.dockerImageTag!!, "npm", "install", "${externalId.name}@${externalId.version}", "-g", "--registry",
                "http://127.0.0.1:${artifactoryConfiguration.port}/artifactory/api/npm/$repositoryKey")
    }

    fun resolveNugetArtifact(repositoryKey: String, externalId: ExternalId) {
        // Nuget is special and cannot find all the executables it needs when run through a java process.
        // The solution is to put the command in the Dockerfile and have them run during the build.
        val packagesJsonResource = this.javaClass.getResourceAsStream("/nuget/Dockerfile") ?: throw FileNotFoundException("Failed to find nuget Dockerfile file in resources.")
        val packageJsonText = packagesJsonResource.convertToString().replaceArtifactoryData(repositoryKey, externalId)

        val outputFile = File("/tmp/artifactory-automation/nuget/Dockerfile")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(packageJsonText)
        println("Dockerfile: ${outputFile.absolutePath}")

        dockerService.buildDockerfile(outputFile, outputFile.parentFile, PackageType.Defaults.NUGET.dockerImageTag!!, noCache = true)
    }

    fun resolvePyPiArtifact(repositoryKey: String, externalId: ExternalId) {
        dockerService.buildTestDockerfile(PackageType.Defaults.PYPI, noCache = false)
        dockerService.runDockerImage(
                PackageType.Defaults.PYPI.dockerImageTag!!,
                "pip3", "install", "${externalId.name}==${externalId.version}", "--index-url=http://127.0.0.1:${artifactoryConfiguration.port}/artifactory/api/pypi/$repositoryKey/simple"
        )
    }

    private fun String.replaceArtifactoryData(repositoryKey: String, externalId: ExternalId): String {
        return this
                .replace(usernameReplacement, artifactoryConfiguration.username)
                .replace(passwordReplacement, artifactoryConfiguration.password)
                .replace(serverReplacement, "127.0.0.1:${artifactoryConfiguration.port}")
                .replace(repoKeyReplacement, repositoryKey)
                .replace(dependencyNameReplacement, externalId.name)
                .replace(dependencyVersionReplacement, externalId.version)
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
                    TestablePackage("http-message-f6561bf28d520154e4b0ec72be95418abe6d9363.zip", externalIdFactory.createNameVersionExternalId(SupportedPackageType.COMPOSER.forge, "psr/http-message", "1.0.1"))
            )
    )

    val CONDA_RESOLVER = Resolver(
            ArtifactResolver::resolveCondaArtifact,
            listOf(
                    TestablePackage("numpy-1.13.1-py27_0.tar.bz2", externalIdFactory.createNameVersionExternalId(SupportedPackageType.CONDA.forge, "numpy", "numpy-1.13.1-py27_0-linux-64"))
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

    val GO_RESOLVER = Resolver(
            ArtifactResolver::resolveGoArtifact,
            listOf(
                    TestablePackage("v1.3.0.zip", externalIdFactory.createNameVersionExternalId(SupportedPackageType.GO.forge, "rsc.io/sampler", "v1.3.0")),
                    TestablePackage("v1.5.2.zip", externalIdFactory.createNameVersionExternalId(SupportedPackageType.GO.forge, "rsc.io/quote", "v1.5.2"))
            )
    )

    val GRADLE_RESOLVER = Resolver(
            ArtifactResolver::resolveMavenGradleArtifact,
            listOf(
                    TestablePackage("blackduck-common-43.0.0.jar", externalIdFactory.createMavenExternalId("com.blackducksoftware.integration", "blackduck-common", "43.0.0")),
                    TestablePackage("abbot-1.4.0.jar", externalIdFactory.createMavenExternalId("abbot", "abbot", "1.4.0"))
            )
    )

    val MAVEN_RESOLVER = Resolver(
            ArtifactResolver::resolveMavenGradleArtifact,
            listOf(
                    TestablePackage("blackduck-common-43.0.0.jar", externalIdFactory.createMavenExternalId("com.blackducksoftware.integration", "blackduck-common", "43.0.0"))
            )
    )

    val NPM_RESOLVER = Resolver(
            ArtifactResolver::resolveNpmArtifact,
            listOf(
                    TestablePackage("lodash-4.17.15.tgz", externalIdFactory.createNameVersionExternalId(SupportedPackageType.NPM.forge, "lodash", "4.17.15"))
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

data class Resolver(val resolverFunction: KFunction3<ArtifactResolver, String, ExternalId, Unit>, val testablePackages: List<TestablePackage>)
