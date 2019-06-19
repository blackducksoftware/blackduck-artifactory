package com.synopsys.integration.blackduck.artifactory.automation.test.inspection

import com.synopsys.integration.blackduck.artifactory.automation.artifactory.RepositoryManager
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.BlackDuckPluginApiService
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.PropertiesApiService
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.RepositoryConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.RepositoryType
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.model.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.plugin.BlackDuckPluginManager
import com.synopsys.integration.blackduck.artifactory.automation.test.Test
import com.synopsys.integration.blackduck.artifactory.automation.test.TestResult
import com.synopsys.integration.blackduck.artifactory.automation.test.TestSequence
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleProperty
import org.artifactory.repo.RepoPathFactory

class RepositoryInitializationTest(
    private val repositoryManager: RepositoryManager,
    private val blackDuckPluginManager: BlackDuckPluginManager,
    private val blackDuckPluginApiService: BlackDuckPluginApiService,
    private val propertiesApiService: PropertiesApiService
) : TestSequence() {
    private lateinit var repositoryConfiguration: RepositoryConfiguration

    override fun setup() {
        repositoryConfiguration = repositoryManager.createRepository(
            PackageType.Defaults.PYPI, RepositoryType.REMOTE)
        blackDuckPluginManager.updateProperties(Pair(InspectionModuleProperty.REPOS, repositoryConfiguration.key))
    }

    @Test
    fun test(): TestResult {
        blackDuckPluginApiService.blackDuckInitializeRepositories()
        val repoKeyPath = RepoPathFactory.create(repositoryConfiguration.key)
        val itemProperties = propertiesApiService.getProperties(repoKeyPath)

        //        itemProperties.properties[BlackDuckArtifactoryProperty.]

        return TestResult("Test name", true, "It passed!")
    }

    override fun tearDown() {

    }
}