package com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.searches

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.gson.responseObject
import com.synopsys.integration.exception.IntegrationException

class ArtifactSearchesAPIService(private val artifactoryFuelManager: FuelManager) {
    fun artifactQuickSearch(name: String, vararg repoKey: String): QuickSearchResult? {
        val parameters = listOf(
                Pair("name", name),
                Pair("repos", repoKey.joinToString(","))
        )

        return artifactoryFuelManager.get("/api/search/artifact", parameters)
                .responseObject<QuickSearchResult>()
                .third.component1()
    }

    fun exactArtifactSearch(name: String, repoKey: String): Artifact {
        return artifactSearch(name, repoKey).first()
    }

    fun artifactSearch(name: String, vararg repoKeys: String): List<Artifact> {
        val quickSearchResult = artifactQuickSearch(name, *repoKeys) ?: throw FailedSearchException(name, *repoKeys)
        return quickSearchResult.results.map { followArtifactUri(it.uri) }
    }

    private fun followArtifactUri(uri: String): Artifact {
        return artifactoryFuelManager.get(uri)
                .responseObject<Artifact>()
                .third.component1()!!
    }
}

class FailedSearchException(name: String, vararg repoKeys: String, override val message: String = "Failed to search for artifact '$name' in any of the following repositories: ${repoKeys.joinToString()}") : IntegrationException()

data class Artifact(
        val repo: String,
        val path: String,
        val created: String,
        val createdBy: String,
        val lastModified: String,
        val modifiedBy: String,
        val lastUpdated: String,
        val downloadUri: String,
        val remoteUrl: String,
        val mimeType: String,
        val size: Int,
        val uri: String
)

data class QuickSearchResult(val results: List<QuickSearchURIResult>)
data class QuickSearchURIResult(val uri: String)