import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty
import com.synopsys.integration.blackduck.artifactory.automation.Application
import com.synopsys.integration.blackduck.artifactory.automation.ApplicationConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.ComponentVerificationService
import com.synopsys.integration.blackduck.artifactory.automation.TestablePackage
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.RepositoryManager
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.artifacts.PropertiesApiService
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType
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

    protected fun cleanupBlackDuck(repositoryKey: String) {
        val projectService = blackDuckServicesFactory.createProjectService()
        val blackDuckService = blackDuckServicesFactory.createBlackDuckService()
        val projectView = projectService.getProjectByName(repositoryKey)
        blackDuckService.delete(projectView.get())
    }

    protected fun verifyNameVersionPackages(repositoryKey: String, testablePackages: List<TestablePackage>) {
        val itemProperties = propertiesApiService.getProperties(repositoryKey)
        Assertions.assertNotNull(itemProperties)
        println(itemProperties!!)

        val projectName = itemProperties.properties[BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME.propertyName]?.first()
        val projectVersionName = itemProperties.properties[BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME.propertyName]?.first()
        val projectService = blackDuckServicesFactory.createProjectService()

        Assertions.assertNotNull(projectName)
        Assertions.assertNotNull(projectVersionName)

        val projectVersionView = projectService.getProjectVersion(projectName, projectVersionName)
        Assertions.assertTrue(projectVersionView.isPresent)

        testablePackages.forEach { testablePackage ->
            componentVerificationService.waitForComponentInspection(repositoryKey, testablePackage)
            componentVerificationService.verifyComponentExistsInBOM(projectVersionView.get().projectVersionView, testablePackage)
        }
    }

    protected fun verifyTestSupport(packageType: PackageType) {
        val supported = SupportedPackageType.getAsSupportedPackageType(packageType.packageType).isPresent
        if (supported && packageType.dockerImageTag != null) {
            throw MissingSupportedPackageTypeException(packageType)
        } else if (supported && packageType.dockerImageTag == null) {
            println("Skipping $packageType because it cannot be automated.")
        } else {
            println("Skipping $packageType because it is not supported by the plugin.")
        }
    }
}

