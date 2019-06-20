import com.synopsys.integration.blackduck.artifactory.automation.Application
import com.synopsys.integration.blackduck.artifactory.automation.ApplicationConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ContextConfiguration(classes = [ApplicationConfiguration::class])
abstract class SpringTest {
    @Autowired
    lateinit var application: Application
}