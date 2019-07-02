package inspector

import ArtifactsToTest
import SpringTest
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty
import com.synopsys.integration.blackduck.artifactory.automation.BlackDuckVerificationService
import com.synopsys.integration.blackduck.artifactory.automation.TestablePackage
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.ArtifactResolver
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.Repository
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.artifacts.PropertiesApiService
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.repositories.RepositoryType
import com.synopsys.integration.blackduck.artifactory.automation.plugin.BlackDuckPluginApiService
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

class RepositoryInitializationTest : SpringTest() {
    @Autowired
    lateinit var blackDuckPluginApiService: BlackDuckPluginApiService

    @Autowired
    lateinit var propertiesApiService: PropertiesApiService

    @Autowired
    lateinit var artifactResolver: ArtifactResolver

    @Autowired
    lateinit var blackDuckVerificationService: BlackDuckVerificationService

    // @ParameterizedTest
    @EnumSource(PackageType.Defaults::class)
    fun emptyRepositoryInitialization(packageType: PackageType) {
        val repository = repositoryManager.createRepositoryInArtifactory(packageType, RepositoryType.REMOTE)
        val blackDuckProjectCreated = testRepository(repository, packageType)
        cleanup(repository, blackDuckProjectCreated)
    }

    @Test
    fun populatedRepositoryInitialization() {
        val packageType = PackageType.Defaults.PYPI
        val repository = repositoryManager.createRepositoryInArtifactory(packageType, RepositoryType.REMOTE)

        when (packageType) {
            PackageType.Defaults.BOWER -> TODO()
            PackageType.Defaults.CHEF -> TODO()
            PackageType.Defaults.COCOAPODS -> TODO()
            PackageType.Defaults.COMPOSER -> TODO()
            PackageType.Defaults.CONAN -> TODO()
            PackageType.Defaults.CONDA -> TODO()
            PackageType.Defaults.CRAN -> TODO()
            PackageType.Defaults.DEBIAN -> TODO()
            PackageType.Defaults.GEMS -> TODO()
            PackageType.Defaults.GO -> TODO()
            PackageType.Defaults.GRADLE -> TODO()
            PackageType.Defaults.HELM -> TODO()
            PackageType.Defaults.IVY -> TODO()
            PackageType.Defaults.MAVEN -> TODO()
            PackageType.Defaults.NPM -> TODO()
            PackageType.Defaults.NUGET -> TODO()
            PackageType.Defaults.PUPPET -> TODO()
            PackageType.Defaults.PYPI -> ArtifactsToTest.PYPI_PACKAGES.forEach { artifactResolver.resolvePyPiArtifact(repository, it.externalId.name, it.externalId.version) }
            PackageType.Defaults.RPM -> TODO()
            PackageType.Defaults.SBT -> TODO()
            PackageType.Defaults.VCS -> TODO()
        }

        val blackDuckProjectCreated = testRepository(repository, packageType)

        verifyNameVersionPackages(repository, ArtifactsToTest.PYPI_PACKAGES)

        cleanup(repository, blackDuckProjectCreated)
    }

    private fun verifyNameVersionPackages(repository: Repository, testablePackages: List<TestablePackage>) {
        val itemProperties = propertiesApiService.getProperties(repository)
        val projectName = itemProperties.properties[BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME.getName()]?.first()
        val projectVersionName = itemProperties.properties[BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME.getName()]?.first()
        val projectService = blackDuckServicesFactory.createProjectService()

        Assertions.assertNotNull(projectName)
        Assertions.assertNotNull(projectVersionName)

        val projectVersionView = projectService.getProjectVersion(projectName, projectVersionName)
        Assertions.assertTrue(projectVersionView.isPresent)

        blackDuckVerificationService.verifyComponentsExists(projectVersionView.get().projectVersionView, testablePackages)
    }

    /**
     * @return true if a project was created in Black Duck.
     */
    private fun testRepository(repository: Repository, packageType: PackageType): Boolean {
        val supported = SupportedPackageType.getAsSupportedPackageType(packageType.packageType).isPresent

        repositoryManager.addRepositoryToInspection(application.containerHash, repository)

        blackDuckPluginApiService.blackDuckInitializeRepositories()
        val itemProperties = propertiesApiService.getProperties(repository.key)

        val propertyKey = BlackDuckArtifactoryProperty.INSPECTION_STATUS.getName()
        val inspectionStatuses = itemProperties.properties[propertyKey]
        Assertions.assertEquals(1, inspectionStatuses?.size)

        val inspectionStatus = inspectionStatuses!![0]
        if (supported) {
            Assertions.assertEquals(InspectionStatus.SUCCESS.name, inspectionStatus)
        } else {
            Assertions.assertEquals(InspectionStatus.FAILURE.name, inspectionStatus)
        }

        return supported
    }
}