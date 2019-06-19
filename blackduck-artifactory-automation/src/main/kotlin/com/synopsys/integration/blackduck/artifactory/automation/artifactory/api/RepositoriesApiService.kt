package com.synopsys.integration.blackduck.artifactory.automation.artifactory.api

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.gson.jsonBody
import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.success
import com.google.gson.annotations.SerializedName
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.model.PackageType
import com.synopsys.integration.log.Slf4jIntLogger
import org.slf4j.LoggerFactory

class RepositoriesApiService(fuelManager: FuelManager) : ArtifactoryApiService(fuelManager) {
    private val logger = Slf4jIntLogger(LoggerFactory.getLogger(javaClass))

    fun createRepository(key: String, repositoryType: RepositoryType, packageType: PackageType, remoteUrl: String = packageType.remoteUrl, externalDependenciesEnabled: Boolean = false): Response {
        val repositoryConfiguration = RepositoryConfiguration(key, repositoryType, packageType = packageType.packageType, remoteUrl = remoteUrl,
            externalDependenciesEnabled = externalDependenciesEnabled)
        return createRepository(repositoryConfiguration)
    }

    fun createRepository(repositoryConfiguration: RepositoryConfiguration): Response {
        return "/api/repositories/${repositoryConfiguration.key}".httpPut()
            .jsonBody(repositoryConfiguration)
            .response { response ->
                response.failure { logger.warn(it.exception.message) }
                response.success { logger.info("Successfully created repository '${repositoryConfiguration.key}") }
            }
            .join()
            .validate()
    }

    fun getRepository(repositoryKey: String): RepositoryConfiguration {
        return "/api/repositories/$repositoryKey".httpGet()
            .responseObject<RepositoryConfiguration>()
            .third.get()
    }
}

enum class RepositoryType {
    @SerializedName("local")
    LOCAL,
    @SerializedName("remote")
    REMOTE,
    @SerializedName("virtual")
    VIRTUAL,
}

data class RepositoryConfiguration(
    @SerializedName("key")
    val key: String,
    @SerializedName("rclass")
    val repositoryType: RepositoryType,
    @SerializedName("packageType")
    val packageType: String,
    @SerializedName("url")
    val remoteUrl: String?,
    @SerializedName("externalDependenciesEnabled")
    val externalDependenciesEnabled: Boolean = false
)
