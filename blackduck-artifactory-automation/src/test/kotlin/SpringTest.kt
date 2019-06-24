import com.synopsys.integration.blackduck.artifactory.automation.ApplicationConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ContextConfiguration(classes = [ApplicationConfiguration::class])
abstract class SpringTest {

}