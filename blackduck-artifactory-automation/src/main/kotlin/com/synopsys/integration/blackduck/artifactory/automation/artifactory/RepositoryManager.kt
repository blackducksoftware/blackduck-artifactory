package com.synopsys.integration.blackduck.artifactory.automation.artifactory

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

    fun getRepository(packageType: PackageType, repositoryType: RepositoryType, repositoryKey: String = getRepositoryKey(packageType, repositoryType)): RepositoryConfiguration {
        return retrieveRepository(repositoryKey)
    }

    private fun retrieveRepository(repositoryKey: String): RepositoryConfiguration {
        return repositoriesApiService.getRepository(repositoryKey)
    }

    private fun getRepositoryKey(packageType: PackageType, repositoryType: RepositoryType): String {
        return "${packageType.packageType}-${repositoryType.name.toLowerCase()}"
    }

    private fun generateRepositoryKey(packageType: PackageType): String {
        return "${packageType.packageType}-${Random.nextInt(0, Int.MAX_VALUE)}"
    }

    fun addRepositoryToInspection(repositoryKey: String) {
        modifyList(repositoryKey, InspectionModuleProperty.REPOS, addToList = true)
    }

    fun addRepositoryToScanner(repositoryKey: String) {
        modifyList(repositoryKey, ScanModuleProperty.REPOS, addToList = true)
    }

    fun removeRepositoryFromInspection(repositoryKey: String) {
        modifyList(repositoryKey, InspectionModuleProperty.REPOS, addToList = false)
    }

    fun removeRepositoryFromScanner(repositoryKey: String) {
        modifyList(repositoryKey, ScanModuleProperty.REPOS, addToList = false)
    }

    private fun modifyList(repositoryKey: String, configurationProperty: ConfigurationProperty, addToList: Boolean = true) {
        val properties = blackDuckPluginManager.getProperties()
        val reposEntry: String? = properties.getProperty(configurationProperty.key)

        var repos = mutableSetOf<String>()
        if (reposEntry != null && reposEntry.isNotBlank()) {
            repos = reposEntry.split(",").map { it.trim() }.toMutableSet()
        }

        if (addToList) {
            repos.add(repositoryKey)
        } else {
            repos.remove(repositoryKey)
        }

        properties.setProperty(configurationProperty.key, repos.joinToString(separator = ","))
        blackDuckPluginManager.setProperties(properties)
    }
}
