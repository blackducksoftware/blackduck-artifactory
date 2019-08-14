package inspector

import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty
import com.synopsys.integration.blackduck.artifactory.automation.MissingPropertyException
import com.synopsys.integration.blackduck.artifactory.automation.NoPropertiesException
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.searches.ArtifactSearchesAPIService
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModule
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

class AfterCreateTest : InspectionTest() {
    @Autowired
    lateinit var artifactSearchesAPIService: ArtifactSearchesAPIService

    @ParameterizedTest
    @EnumSource(PackageType.Defaults::class)
    fun afterCreateTest(packageType: PackageType) {
        resolverRequiredTest(packageType) { resolver, repositoryKey, resolverRepositoryKey ->
            val inspectionModuleName = InspectionModule::class.simpleName!!
            blackDuckPluginApiService.setModuleState(inspectionModuleName, false)
            repositoryManager.addRepositoryToInspection(repositoryKey)
            blackDuckPluginApiService.setModuleState(inspectionModuleName, true)
            blackDuckPluginApiService.deleteInspectionProperties()
            blackDuckPluginApiService.blackDuckInitializeRepositories()

            val testablePackages = resolver.testablePackages
            testablePackages.forEach { testablePackage ->
                println("Testing package: ${testablePackage.externalId.createExternalId()}")

                resolver.resolverFunction(artifactResolver, resolverRepositoryKey, testablePackage.externalId)
                componentVerificationService.waitForComponentInspection(repositoryKey, testablePackage)

                val artifact = artifactSearchesAPIService.exactArtifactSearch(testablePackage.artifactoryFileName, repositoryKey)
                val repoPath = repositoryKey + artifact.path
                val itemProperties = propertiesApiService.getProperties(repoPath) ?: throw NoPropertiesException(repoPath)
                val inspectionStatusPropertyKey = BlackDuckArtifactoryProperty.INSPECTION_STATUS.getName()
                // If the inspection status property is missing, the after create event was not triggered.
                val inspectionStatus = itemProperties.properties[inspectionStatusPropertyKey]?.first() ?: throw MissingPropertyException(inspectionStatusPropertyKey, repoPath)

                if (inspectionStatus == InspectionStatus.SUCCESS.name) {
                    assertSuccess(itemProperties)
                } else {
                    assertFailure(itemProperties)
                    throw Exception("afterCreate failed and properly applied properties, but the inspection should not have failed.")
                }
            }

            return@resolverRequiredTest true
        }
    }
}