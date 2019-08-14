package inspector

import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty
import com.synopsys.integration.blackduck.artifactory.automation.NoPropertiesException
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.repositories.RepositoryType
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModule
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class RepositoryInitializationTest : InspectionTest() {
    @ParameterizedTest
    @EnumSource(PackageType.Defaults::class)
    fun emptyRepositoryInitialization(packageType: PackageType) {
        val repository = repositoryManager.getRepository(packageType, repositoryType = RepositoryType.REMOTE)
        val repositoryKey = "${repository.key}-cache"
        val blackDuckProjectCreated = testRepository(repositoryKey, packageType)
        cleanup(repositoryKey, blackDuckProjectCreated)
    }

    @ParameterizedTest
    @EnumSource(PackageType.Defaults::class)
    fun populatedRepositoryInitialization(packageType: PackageType) {
        resolverRequiredTest(packageType) { resolver, repositoryKey, resolverRepositoryKey ->
            val testablePackages = resolver.testablePackages
            testablePackages.forEach { resolver.resolverFunction(artifactResolver, resolverRepositoryKey, it.externalId) }
            return@resolverRequiredTest testRepository(repositoryKey, packageType)
        }
    }

    /**
     * @return true if a project was created in Black Duck.
     */
    private fun testRepository(repositoryKey: String, packageType: PackageType): Boolean {
        val supported = SupportedPackageType.getAsSupportedPackageType(packageType.packageType).isPresent
        val inspectionModuleName = InspectionModule::class.simpleName!!

        blackDuckPluginApiService.setModuleState(inspectionModuleName, false)
        repositoryManager.addRepositoryToInspection(repositoryKey)
        blackDuckPluginApiService.setModuleState(inspectionModuleName, true)
        blackDuckPluginApiService.deleteInspectionProperties()
        blackDuckPluginApiService.blackDuckInitializeRepositories()

        val itemProperties = propertiesApiService.getProperties(repositoryKey) ?: throw NoPropertiesException(repositoryKey)

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