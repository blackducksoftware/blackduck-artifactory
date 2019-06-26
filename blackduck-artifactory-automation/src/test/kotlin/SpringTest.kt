import com.synopsys.integration.blackduck.artifactory.automation.Application
import com.synopsys.integration.blackduck.artifactory.automation.ApplicationConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.Repository
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.RepositoryManager
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory
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

    protected fun cleanup(repository: Repository, blackDuckProjectCreated: Boolean) {
        repositoryManager.deleteRepository(repository)
        repositoryManager.removeRepositoryFromInspection(application.containerHash, repository)

        if (blackDuckProjectCreated) {
            val projectService = blackDuckServicesFactory.createProjectService()
            val blackDuckService = blackDuckServicesFactory.createBlackDuckService()
            val projectView = projectService.getProjectByName(repositoryManager.determineRepositoryKey(repository))
            blackDuckService.delete(projectView.get())
        }
    }
}