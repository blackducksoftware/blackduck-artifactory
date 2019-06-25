package com.synopsys.integration.blackduck.artifactory.automation.artifactory

import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.RepositoriesApiService
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.RepositoryConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.RepositoryType
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.model.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.plugin.BlackDuckPluginManager
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleProperty
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModuleProperty
import kotlin.random.Random

class RepositoryManager(private val repositoriesApiService: RepositoriesApiService, private val blackDuckPluginManager: BlackDuckPluginManager) {
    fun createRepository(packageType: PackageType, repositoryType: RepositoryType): Repository {
        val repositoryKey = generateRepostioryKey(packageType)
        repositoriesApiService.createRepository(repositoryKey, repositoryType, packageType)
        val repositoryConfiguration = retrieveRepository(repositoryKey)
        return Repository(repositoryKey, repositoryConfiguration, repositoryType)
    }

    fun addRepositoryToInspection(containerHash: String, repository: Repository) {
        val key: String = when (repository.type) {
            RepositoryType.REMOTE -> "${repository.key}-cache"
            else -> repository.key
        }
        blackDuckPluginManager.updateProperties(containerHash, Pair(InspectionModuleProperty.REPOS, key))
    }

    fun addRepositoryToScanner(containerHash: String, repository: Repository) {
        blackDuckPluginManager.updateProperties(containerHash, Pair(ScanModuleProperty.REPOS, repository.key))
    }

    private fun retrieveRepository(repositoryKey: String): RepositoryConfiguration {
        return repositoriesApiService.getRepository(repositoryKey)
    }

    private fun generateRepostioryKey(packageType: PackageType): String {
        return "${packageType.packageType}-${Random.nextInt(0, Int.MAX_VALUE)}"
    }
}

data class Repository(
    val key: String,
    val configuration: RepositoryConfiguration,
    val type: RepositoryType
)
