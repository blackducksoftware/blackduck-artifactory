import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty
import com.synopsys.integration.blackduck.artifactory.automation.Application
import com.synopsys.integration.blackduck.artifactory.automation.ApplicationConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.ComponentVerificationService
import com.synopsys.integration.blackduck.artifactory.automation.TestablePackage
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.RepositoryManager
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.Repository
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.artifacts.PropertiesApiService
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ContextConfiguration(classes = [ApplicationConfiguration::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("automation")
abstract class SpringTest {
    @Autowired
    lateinit var application: Application

    @Autowired
    lateinit var repositoryManager: RepositoryManager

    @Autowired
    lateinit var blackDuckServicesFactory: BlackDuckServicesFactory

    @Autowired
    lateinit var propertiesApiService: PropertiesApiService

    @Autowired
    lateinit var componentVerificationService: ComponentVerificationService

    protected fun cleanup(repository: Repository, blackDuckProjectCreated: Boolean) {
        repositoryManager.deleteRepositoryFromArtifactory(repository)
        repositoryManager.removeRepositoryFromInspection(repository)

        if (blackDuckProjectCreated) {
            val projectService = blackDuckServicesFactory.createProjectService()
            val blackDuckService = blackDuckServicesFactory.createBlackDuckService()
            val projectView = projectService.getProjectByName(RepositoryManager.determineRepositoryKey(repository))
            blackDuckService.delete(projectView.get())
        }
    }

    protected fun verifyNameVersionPackages(repository: Repository, testablePackages: List<TestablePackage>) {
        val itemProperties = propertiesApiService.getProperties(repository)
        Assertions.assertNotNull(itemProperties)
        println(itemProperties!!)

        val projectName = itemProperties.properties[BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME.getName()]?.first()
        val projectVersionName = itemProperties.properties[BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME.getName()]?.first()
        val projectService = blackDuckServicesFactory.createProjectService()

        Assertions.assertNotNull(projectName)
        Assertions.assertNotNull(projectVersionName)

        val projectVersionView = projectService.getProjectVersion(projectName, projectVersionName)
        Assertions.assertTrue(projectVersionView.isPresent)

        testablePackages.forEach { testablePackage ->
            componentVerificationService.waitForComponentInspection(repository, testablePackage)
            componentVerificationService.verifyComponentExistsInBOM(projectVersionView.get().projectVersionView, testablePackage)
        }
    }
}

