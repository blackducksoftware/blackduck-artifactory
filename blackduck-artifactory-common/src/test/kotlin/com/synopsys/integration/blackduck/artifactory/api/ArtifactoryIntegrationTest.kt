package com.synopsys.integration.blackduck.artifactory.api

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.extensions.authentication
import com.synopsys.integration.log.IntLogger
import com.synopsys.integration.log.Slf4jIntLogger
import org.artifactory.repo.Repositories
import org.artifactory.search.Searches
import org.junit.jupiter.api.Tag

@Tag("integration")
open class ArtifactoryIntegrationTest {

    private val logger: IntLogger = Slf4jIntLogger(org.slf4j.LoggerFactory.getLogger(this.javaClass))

    val searches: Searches
    val repositories: Repositories

    init {
        val fuelManager = FuelManager()
        fuelManager.basePath = System.getenv("AUTOMATION_ARTIFACTORY_BASE_URL")
        fuelManager.addRequestInterceptor {
            {
                logger.info("Making ${it.method} request to ${it.url}")
                it.authentication().basic(System.getenv("AUTOMATION_ARTIFACTORY_USERNAME"), System.getenv("AUTOMATION_ARTIFACTORY_PASSWORD"))
            }
        }

        searches = SearchesApi(fuelManager)
        repositories = RepositoriesApi(fuelManager)
    }

    companion object {
        const val ARTIFACTORY_ENVIRONMENT_PRESENT: String = "systemEnvironment.get('AUTOMATION_ARTIFACTORY_BASE_URL') != null" +
            " && systemEnvironment.get('AUTOMATION_ARTIFACTORY_USERNAME') != null" +
            " && systemEnvironment.get('AUTOMATION_ARTIFACTORY_PASSWORD') != null"
    }
}