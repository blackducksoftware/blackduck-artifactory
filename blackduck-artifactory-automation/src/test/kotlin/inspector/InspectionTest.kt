package inspector

import SpringTest
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.ArtifactResolver
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.ArtifactoryConfigurationService
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.Resolver
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.artifacts.ItemProperties
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.repositories.RepositoryType
import com.synopsys.integration.blackduck.artifactory.automation.plugin.BlackDuckPluginApiService
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired

abstract class InspectionTest : SpringTest() {
    @Autowired
    lateinit var blackDuckPluginApiService: BlackDuckPluginApiService

    @Autowired
    lateinit var artifactResolver: ArtifactResolver

    @Autowired
    lateinit var artifactoryConfigurationService: ArtifactoryConfigurationService

    fun cleanup(repositoryKey: String, blackDuckProjectCreated: Boolean) {
        if (blackDuckProjectCreated) {
            super.cleanupBlackDuck(repositoryKey)
        }
        blackDuckPluginApiService.deleteInspectionProperties()
        repositoryManager.removeRepositoryFromInspection(repositoryKey)
        blackDuckPluginApiService.reloadPlugin()
    }

    @BeforeAll
    internal fun setUp() {
        artifactoryConfigurationService.importSettings()
    }

    /**
     * @param testFunction should return true if a project was created in Black Duck for cleanup.
     */
    fun resolverRequiredTest(packageType: PackageType, testFunction: (resolver: Resolver, repositoryKey: String, resolverRepositoryKey: String) -> Boolean) {
        val resolver = packageType.resolver

        if (resolver != null) {
            val remoteRepository = repositoryManager.getRepository(packageType, RepositoryType.REMOTE)
            var resolverRepository = remoteRepository
            if (packageType.requiresVirtual) {
                resolverRepository = repositoryManager.getRepository(packageType, RepositoryType.VIRTUAL)
            }

            val testRepoKey = "${remoteRepository.key}-cache"
            val blackDuckProjectCreated = testFunction(resolver, testRepoKey, resolverRepository.key)
            cleanup(testRepoKey, blackDuckProjectCreated)
        } else {
            verifyTestSupport(packageType)
        }
    }

    fun assertFailure(itemProperties: ItemProperties) {
        val properties: Map<String, String> = itemProperties.properties.mapValues { it.value.first() }

        Assertions.assertEquals(properties[BlackDuckArtifactoryProperty.INSPECTION_STATUS.getName()], InspectionStatus.FAILURE.name, "Inspection status should be ${InspectionStatus.FAILURE.name}. ${itemProperties.uri}")
        assertNotNull(BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT, properties, itemProperties.uri, false)
        assertNotNull(BlackDuckArtifactoryProperty.INSPECTION_STATUS_MESSAGE, properties, itemProperties.uri, false)
        assertNotNull(BlackDuckArtifactoryProperty.LAST_INSPECTION, properties, itemProperties.uri, false)
    }

    fun assertSuccess(itemProperties: ItemProperties) {
        val properties: Map<String, String> = itemProperties.properties.mapValues { it.value.first() }

        Assertions.assertEquals(properties[BlackDuckArtifactoryProperty.INSPECTION_STATUS.getName()], InspectionStatus.SUCCESS.name, "Inspection status should be ${InspectionStatus.FAILURE.name}. ${itemProperties.uri}")
        assertNull(BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT, properties, itemProperties.uri)
        assertNull(BlackDuckArtifactoryProperty.INSPECTION_STATUS_MESSAGE, properties, itemProperties.uri)
        assertNotNull(BlackDuckArtifactoryProperty.LAST_INSPECTION, properties, itemProperties.uri)
        assertNotNull(BlackDuckArtifactoryProperty.HIGH_VULNERABILITIES, properties, itemProperties.uri)
        assertNotNull(BlackDuckArtifactoryProperty.MEDIUM_VULNERABILITIES, properties, itemProperties.uri)
        assertNotNull(BlackDuckArtifactoryProperty.LOW_VULNERABILITIES, properties, itemProperties.uri)
        assertNotNull(BlackDuckArtifactoryProperty.POLICY_STATUS, properties, itemProperties.uri)
        assertNotNull(BlackDuckArtifactoryProperty.COMPONENT_VERSION_URL, properties, itemProperties.uri)
        assertNotNull(BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID, properties, itemProperties.uri)
        assertNotNull(BlackDuckArtifactoryProperty.BLACKDUCK_FORGE, properties, itemProperties.uri)
        assertNotNull(BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES, properties, itemProperties.uri);
    }

    private fun assertNull(property: BlackDuckArtifactoryProperty, properties: Map<String, String>, uri: String, success: Boolean = true) {
        Assertions.assertNull(properties[property.getName()], "An artifact marked as ${if (success) "success" else "failure"} should not have a ${property.getName()} property. $uri")
    }

    private fun assertNotNull(property: BlackDuckArtifactoryProperty, properties: Map<String, String>, uri: String, success: Boolean = true) {
        Assertions.assertNotNull(properties[property.getName()], "An artifact marked as ${if (success) "success" else "failure"} should have a ${property.getName()} property. $uri")
    }
}