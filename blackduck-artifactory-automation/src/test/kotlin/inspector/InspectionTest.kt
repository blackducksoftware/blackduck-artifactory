package inspector

import SpringTest
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.ArtifactResolver
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.Resolver
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.Repository
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.artifacts.ItemProperties
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.repositories.RepositoryType
import com.synopsys.integration.blackduck.artifactory.automation.plugin.BlackDuckPluginApiService
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired

abstract class InspectionTest : SpringTest() {
    @Autowired
    lateinit var blackDuckPluginApiService: BlackDuckPluginApiService

    @Autowired
    lateinit var artifactResolver: ArtifactResolver

    override fun cleanup(repository: Repository, blackDuckProjectCreated: Boolean) {
        super.cleanup(repository, blackDuckProjectCreated)
        repositoryManager.removeRepositoryFromInspection(repository)
    }

    /**
     * @param testFunction should return true if a project was created in Black Duck for cleanup.
     */
    fun resolverRequiredTest(packageType: PackageType, testFunction: (repository: Repository, resolver: Resolver) -> Boolean) {
        val resolver = packageType.resolver

        if (resolver != null) {
            val remoteRepository = repositoryManager.createRepositoryInArtifactory(packageType, RepositoryType.REMOTE)

            if (packageType.requiresVirtual) {
                val virtualRepository = repositoryManager.createRepositoryInArtifactory(packageType, RepositoryType.VIRTUAL, listOf(remoteRepository))
                val blackDuckProjectCreated = testFunction(virtualRepository, resolver)
                cleanup(virtualRepository, blackDuckProjectCreated)
                cleanup(remoteRepository, false)
            } else {
                val blackDuckProjectCreated = testFunction(remoteRepository, resolver)
                cleanup(remoteRepository, blackDuckProjectCreated)
            }
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
        assertNull(BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT, properties, itemProperties.uri, true)
        assertNull(BlackDuckArtifactoryProperty.INSPECTION_STATUS_MESSAGE, properties, itemProperties.uri, true)
        assertNotNull(BlackDuckArtifactoryProperty.LAST_INSPECTION, properties, itemProperties.uri, true)
        assertNotNull(BlackDuckArtifactoryProperty.HIGH_VULNERABILITIES, properties, itemProperties.uri, true)
        assertNotNull(BlackDuckArtifactoryProperty.MEDIUM_VULNERABILITIES, properties, itemProperties.uri, true)
        assertNotNull(BlackDuckArtifactoryProperty.LOW_VULNERABILITIES, properties, itemProperties.uri, true)
        assertNotNull(BlackDuckArtifactoryProperty.POLICY_STATUS, properties, itemProperties.uri, true)
        assertNotNull(BlackDuckArtifactoryProperty.COMPONENT_VERSION_URL, properties, itemProperties.uri, true)
        assertNotNull(BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID, properties, itemProperties.uri, true)
        assertNotNull(BlackDuckArtifactoryProperty.BLACKDUCK_FORGE, properties, itemProperties.uri, true)
    }

    private fun assertNull(property: BlackDuckArtifactoryProperty, properties: Map<String, String>, uri: String, success: Boolean = false) {
        Assertions.assertNull(properties[property.getName()], "An artifact marked as ${if (success) "success" else "failure"} should not have a ${property.getName()} property. $uri")
    }

    private fun assertNotNull(property: BlackDuckArtifactoryProperty, properties: Map<String, String>, uri: String, success: Boolean = false) {
        Assertions.assertNotNull(properties[property.getName()], "An artifact marked as ${if (success) "success" else "failure"} should have a ${property.getName()} property. $uri")
    }
}