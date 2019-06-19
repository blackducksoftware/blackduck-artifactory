package com.synopsys.integration.blackduck.artifactory.automation.artifactory

import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.RepositoriesApiService
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.RepositoryConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.RepositoryType
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.model.PackageType
import kotlin.random.Random

class RepositoryManager(private val repositoriesApiService: RepositoriesApiService) {
    fun createRepository(packageType: PackageType, repositoryType: RepositoryType): RepositoryConfiguration {
        val repositoryKey = "${packageType.packageType}-${Random.nextInt()}"
        repositoriesApiService.createRepository(repositoryKey, repositoryType, packageType)
        return retrieveRepository(repositoryKey)
    }

    fun retrieveRepository(repositoryKey: String): RepositoryConfiguration {
        return repositoriesApiService.getRepository(repositoryKey)
    }
}

