package com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.repositories

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.gson.jsonBody
import com.github.kittinunf.fuel.gson.responseObject
import com.google.gson.annotations.SerializedName
import com.synopsys.integration.blackduck.artifactory.automation.validate
import com.synopsys.integration.log.Slf4jIntLogger
import org.slf4j.LoggerFactory

class RepositoriesApiService(private val fuelManager: FuelManager) {
    private val logger = Slf4jIntLogger(LoggerFactory.getLogger(javaClass))

    fun createRepository(repositoryConfiguration: RepositoryConfiguration): Response {
        return fuelManager.put("/api/repositories/${repositoryConfiguration.key}")
            .jsonBody(repositoryConfiguration)
            .response()
            .second.validate()
    }

    fun deleteRepository(repositoryKey: String): Response {
        return fuelManager.delete("/api/repositories/$repositoryKey")
            .response()
            .second.validate()
    }

    fun getRepository(repositoryKey: String): RepositoryConfiguration {
        return fuelManager.get("/api/repositories/$repositoryKey")
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
    val key: String,
    @SerializedName("rclass")
    val repositoryType: RepositoryType,
    val packageType: String,
    @SerializedName("url")
    val remoteUrl: String?,
    val externalDependenciesEnabled: Boolean = false,
    @SerializedName("repoLayoutRef")
    val repositoryLayout: String = "simple-default",
    val feedContextPath: String = "api/v2",
    val downloadContextPath: String = "api/v2/package",
    val v3FeedUrl: String = "https://api.nuget.org/v3/index.json"
)
