package inspector

import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty
import com.synopsys.integration.blackduck.artifactory.automation.MissingPropertyException
import com.synopsys.integration.blackduck.artifactory.automation.NoPropertiesException
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.RepositoryManager
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.searches.ArtifactSearchesAPIService
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

class AfterCreateTest : InspectionTest() {
    @Autowired
    lateinit var artifactSearchesAPIService: ArtifactSearchesAPIService

    @ParameterizedTest
    @EnumSource(PackageType.Defaults::class)
    fun afterCreateTest(packageType: PackageType) {
        resolverRequiredTest(packageType) { repository, resolver ->
            repositoryManager.addRepositoryToInspection(repository)
            blackDuckPluginApiService.blackDuckInitializeRepositories()

            val testablePackages = resolver.testablePackages
            testablePackages.forEach { testablePackage ->
                resolver.resolverFunction(artifactResolver, repository, testablePackage.externalId)

                val repoKey = RepositoryManager.determineRepositoryKey(repository)
                val artifact = artifactSearchesAPIService.exactArtifactSearch(testablePackage.artifactoryFileName, repoKey)
                val repoPath = repository.key + artifact.path
                val properties = propertiesApiService.getProperties(repoPath) ?: throw NoPropertiesException(repoPath)

                val inspectionStatusPropertyKey = BlackDuckArtifactoryProperty.INSPECTION_STATUS.getName()

                // If the inspection status property is missing, the after create event was not triggered.
                properties.properties[inspectionStatusPropertyKey]?.first() ?: throw MissingPropertyException(inspectionStatusPropertyKey, repoPath)
            }

            return@resolverRequiredTest true
        }
    }
}