import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty
import com.synopsys.integration.blackduck.artifactory.automation.Application
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.RepositoryManager
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.PropertiesApiService
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.RepositoryConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.RepositoryType
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.model.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.plugin.BlackDuckPluginApiService
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Tag("automation")
class RepositoryInitializationTest : SpringTest() {
    @Autowired
    lateinit var application: Application

    @Autowired
    lateinit var repositoryManager: RepositoryManager

    @Autowired
    lateinit var blackDuckPluginApiService: BlackDuckPluginApiService

    @Autowired
    lateinit var propertiesApiService: PropertiesApiService

    lateinit var repositoryConfiguration: RepositoryConfiguration

    @BeforeEach
    fun setup() {
        repositoryConfiguration = repositoryManager.createRepository(application.containerHash, PackageType.Defaults.PYPI, RepositoryType.REMOTE)
    }

    @Test
    fun test() {
        blackDuckPluginApiService.blackDuckInitializeRepositories()
        val itemProperties = propertiesApiService.getProperties(repositoryConfiguration.key)

        val propertyKey = BlackDuckArtifactoryProperty.INSPECTION_STATUS.getName()
        val inspectionStatus = itemProperties.properties[propertyKey]
        Assertions.assertEquals(1, inspectionStatus?.size)
        Assertions.assertEquals(InspectionStatus.SUCCESS.name, inspectionStatus!![0])
    }
}