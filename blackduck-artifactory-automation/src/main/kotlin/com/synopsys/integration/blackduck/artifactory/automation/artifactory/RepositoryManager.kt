package com.synopsys.integration.blackduck.artifactory.automation.artifactory

import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.Repository
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.repositories.RepositoriesApiService
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.repositories.RepositoryConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.repositories.RepositoryType
import com.synopsys.integration.blackduck.artifactory.automation.plugin.BlackDuckPluginManager
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleProperty
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModuleProperty
import com.synopsys.integration.log.Slf4jIntLogger
import org.slf4j.LoggerFactory
import kotlin.random.Random

class RepositoryManager(private val repositoriesApiService: RepositoriesApiService, private val blackDuckPluginManager: BlackDuckPluginManager) {
    private val logger = Slf4jIntLogger(LoggerFactory.getLogger(this::class.java))

    fun createRepositoryInArtifactory(packageType: PackageType, repositoryType: RepositoryType): Repository {
        val repositoryKey = generateRepositoryKey(packageType)
        logger.info("Creating repository '$repositoryKey'")
        repositoriesApiService.createRepository(repositoryKey, repositoryType, packageType)
        val repositoryConfiguration = retrieveRepository(repositoryKey)
        return Repository(repositoryKey, repositoryConfiguration, repositoryType)
    }

    fun deleteRepositoryFromArtifactory(repository: Repository?) {
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
        modifyList(containerHash, repository, InspectionModuleProperty.REPOS, addToList = true)
    }

    fun addRepositoryToScanner(containerHash: String, repository: Repository) {
        modifyList(containerHash, repository, ScanModuleProperty.REPOS, addToList = true)
    }

    fun removeRepositoryFromInspection(containerHash: String, repository: Repository) {
        modifyList(containerHash, repository, InspectionModuleProperty.REPOS, addToList = false)
    }

    fun removeRepositoryFromScanner(containerHash: String, repository: Repository) {
        modifyList(containerHash, repository, ScanModuleProperty.REPOS, addToList = false)
    }

    private fun modifyList(containerHash: String, repository: Repository, configurationProperty: ConfigurationProperty, addToList: Boolean = true) {
        val properties = blackDuckPluginManager.getProperties(containerHash)
        val reposEntry: String? = properties.getProperty(configurationProperty.key)
        val key = determineRepositoryKey(repository)

        var repos = mutableSetOf<String>()
        if (reposEntry != null && reposEntry.isNotBlank()) {
            repos = reposEntry.split(",").map { it.trim() }.toMutableSet()
        }

        if (addToList) {
            repos.add(key)
        } else {
            repos.remove(key)
        }

        properties.setProperty(configurationProperty.key, repos.joinToString(separator = ","))
        blackDuckPluginManager.setProperties(containerHash, properties)
    }

    fun determineRepositoryKey(repository: Repository): String {
        return when (repository.type) {
            RepositoryType.REMOTE -> "${repository.key}-cache"
            else -> repository.key
        }
    }
}
