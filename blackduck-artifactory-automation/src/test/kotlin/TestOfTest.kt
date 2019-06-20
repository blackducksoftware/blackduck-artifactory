import com.synopsys.integration.blackduck.artifactory.automation.Application
import com.synopsys.integration.blackduck.artifactory.automation.ApplicationConfiguration
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.context.support.DirtiesContextTestExecutionListener

@RunWith(SpringRunner::class)
@SpringBootTest
@ContextConfiguration(classes = [ApplicationConfiguration::class])
@TestExecutionListeners(listeners = [DependencyInjectionTestExecutionListener::class, DirtiesContextTestExecutionListener::class])
class TestOfTest {
    @Autowired
    lateinit var application: Application

    @Test
    fun test() {
        println(application.containerHash)
    }
}