package inspector

import SpringTest
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.ArtifactResolver
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.Resolver
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.Repository
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.repositories.RepositoryType
import com.synopsys.integration.blackduck.artifactory.automation.plugin.BlackDuckPluginApiService
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
            val repository = repositoryManager.createRepositoryInArtifactory(packageType, RepositoryType.REMOTE)
            val blackDuckProjectCreated = testFunction(repository, resolver)
            cleanup(repository, blackDuckProjectCreated)
        } else {
            verifyTestSupport(packageType)
        }
    }
}