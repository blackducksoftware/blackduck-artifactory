package com.synopsys.integration.blackduck.artifactory.automation.artifactory

import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.RepositoriesApiService
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.RepositoryConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.RepositoryType
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.model.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.plugin.BlackDuckPluginManager
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleProperty
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModuleProperty
import com.synopsys.integration.log.Slf4jIntLogger
import org.slf4j.LoggerFactory
import kotlin.random.Random

class RepositoryManager(private val repositoriesApiService: RepositoriesApiService, private val blackDuckPluginManager: BlackDuckPluginManager) {
    val logger = Slf4jIntLogger(LoggerFactory.getLogger(this::class.java))

    fun createRepository(packageType: PackageType, repositoryType: RepositoryType): Repository {
        val repositoryKey = generateRepositoryKey(packageType)
        logger.info("Creating repository '$repositoryKey'")
        repositoriesApiService.createRepository(repositoryKey, repositoryType, packageType)
        val repositoryConfiguration = retrieveRepository(repositoryKey)
        return Repository(repositoryKey, repositoryConfiguration, repositoryType)
    }

    fun deleteRepository(repository: Repository?) {
        if (repository != null) {
            val keyToDelete = repository.key
            logger.info("Deleting repository '$keyToDelete'")
            repositoriesApiService.deleteRepository(keyToDelete)
        }
    }

    private fun retrieveRepository(repositoryKey: String): RepositoryConfiguration {
        return repositoriesApiService.getRepository(repositoryKey)
    }

    private fun generateRepositoryKey(packageType: PackageType): String {
        return "${packageType.packageType}-${Random.nextInt(0, Int.MAX_VALUE)}"
    }

    fun addRepositoryToInspection(containerHash: String, repository: Repository) {
        val key = determineRepositoryKey(repository)
        blackDuckPluginManager.updateProperties(containerHash, Pair(InspectionModuleProperty.REPOS, key))
    }

    fun addRepositoryToScanner(containerHash: String, repository: Repository) {
        blackDuckPluginManager.updateProperties(containerHash, Pair(ScanModuleProperty.REPOS, repository.key))
    }

    fun removeRepositoryFromInspection(containerHash: String, repository: Repository) {
        removeFromList(containerHash, repository, InspectionModuleProperty.REPOS)
    }

    fun removeRepositoryFromScanner(containerHash: String, repository: Repository) {
        removeFromList(containerHash, repository, ScanModuleProperty.REPOS)
    }

    private fun removeFromList(containerHash: String, repository: Repository, configurationProperty: ConfigurationProperty) {
        val key = determineRepositoryKey(repository)
        val properties = blackDuckPluginManager.getProperties(containerHash)
        val reposEntry: String? = properties.getProperty(configurationProperty.key)

        if (reposEntry != null) {
            val repos = reposEntry.split(",").map { it.trim() }.toMutableSet()
            repos.remove(key)
        } else {
            logger.error("Attempted to remove repository from plugin configuration properties, but the configuration property ${configurationProperty.key} was missing.")
        }
    }

    fun determineRepositoryKey(repository: Repository): String {
        return when (repository.type) {
            RepositoryType.REMOTE -> "${repository.key}-cache"
            else -> repository.key
        }
    }
}

data class Repository(
    val key: String,
    val configuration: RepositoryConfiguration,
    val type: RepositoryType
)
