package com.synopsys.integration.blackduck.artifactory.automation.artifactory

import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.RepositoriesApiService
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.RepositoryConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.RepositoryType
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.model.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.plugin.BlackDuckPluginManager
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleProperty
import kotlin.random.Random

class RepositoryManager(private val repositoriesApiService: RepositoriesApiService, private val blackDuckPluginManager: BlackDuckPluginManager) {
    fun createRepository(containerHash: String, packageType: PackageType, repositoryType: RepositoryType): RepositoryConfiguration {
        val repositoryKey = "${packageType.packageType}-${Random.nextInt()}"
        repositoriesApiService.createRepository(repositoryKey, repositoryType, packageType)

        val repositoryConfiguration = retrieveRepository(repositoryKey)
        blackDuckPluginManager.updateProperties(containerHash, Pair(InspectionModuleProperty.REPOS, repositoryConfiguration.key))

        return repositoryConfiguration
    }

    fun retrieveRepository(repositoryKey: String): RepositoryConfiguration {
        return repositoriesApiService.getRepository(repositoryKey)
    }
}

