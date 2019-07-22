package inspector

import SpringTest
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty
import com.synopsys.integration.blackduck.artifactory.automation.NoPropertiesException
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.ArtifactResolver
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.Repository
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.repositories.RepositoryType
import com.synopsys.integration.blackduck.artifactory.automation.plugin.BlackDuckPluginApiService
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

class RepositoryInitializationTest : SpringTest() {
    @Autowired
    lateinit var blackDuckPluginApiService: BlackDuckPluginApiService

    @Autowired
    lateinit var artifactResolver: ArtifactResolver

    @ParameterizedTest
    @EnumSource(PackageType.Defaults::class)
    fun emptyRepositoryInitialization(packageType: PackageType) {
        val repository = repositoryManager.createRepositoryInArtifactory(packageType, RepositoryType.REMOTE)
        val blackDuckProjectCreated = checkSuccessfulInitialization(repository, packageType)
        cleanup(repository, blackDuckProjectCreated)
    }

    @ParameterizedTest
    @EnumSource(PackageType.Defaults::class)
    fun populatedRepositoryInitialization(packageType: PackageType) {
        val resolver = packageType.resolver

        if (resolver != null) {
            val repository = repositoryManager.createRepositoryInArtifactory(packageType, RepositoryType.REMOTE)
            val testablePackages = resolver.testablePackages
            testablePackages.forEach { resolver.resolverFunction(artifactResolver, repository, it.externalId) }

            val blackDuckProjectCreated = checkSuccessfulInitialization(repository, packageType)
            cleanup(repository, blackDuckProjectCreated)
        } else {
            verifyTestSupport(packageType)
        }
    }

    /**
     * @return true if a project was created in Black Duck.
     */
    private fun checkSuccessfulInitialization(repository: Repository, packageType: PackageType): Boolean {
        val supported = SupportedPackageType.getAsSupportedPackageType(packageType.packageType).isPresent

        repositoryManager.addRepositoryToInspection(repository)
        blackDuckPluginApiService.blackDuckInitializeRepositories()
        val itemProperties = propertiesApiService.getProperties(repository.key) ?: throw NoPropertiesException(repository.key)

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