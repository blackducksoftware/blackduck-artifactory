package com.synopsys.integration.blackduck.artifactory.api

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.gson.responseObject
import com.google.common.collect.SetMultimap
import com.synopsys.integration.blackduck.artifactory.PluginRepoPath
import com.synopsys.integration.blackduck.artifactory.api.model.ItemInfoData
import org.artifactory.build.BuildRun
import org.artifactory.repo.RepoPath
import org.artifactory.search.Searches
import org.artifactory.search.aql.AqlResultHandler
import java.util.*

class SearchesApi(private val fuelManager: FuelManager) : Searches {
    override fun buildsByArtifactSha1(sha1: String?): MutableSet<BuildRun> {
        TODO("not implemented")
    }

    override fun artifactsByGavc(groupId: String?, artifactId: String?, version: String?, classifier: String?, vararg repositories: String?): MutableList<RepoPath> {
        TODO("not implemented")
    }

    override fun itemsByProperties(properties: SetMultimap<String, String>?, vararg repositories: String?): MutableList<RepoPath> {
        TODO("not implemented")
    }

    override fun aql(aqlQuery: String?, handler: AqlResultHandler?) {
        TODO("not implemented")
    }

    override fun artifactsCreatedOrModifiedInRange(from: Calendar?, to: Calendar?, vararg repositories: String?): MutableList<RepoPath> {
        TODO("not implemented")
    }

    override fun buildsByDependencySha1(sha1: String?): MutableSet<BuildRun> {
        TODO("not implemented")
    }

    override fun artifactsBySha1(sha1: String?, vararg repositories: String?): MutableSet<RepoPath> {
        TODO("not implemented")
    }

    override fun artifactsNotDownloadedSince(since: Calendar?, createdBefore: Calendar?, vararg repositories: String?): MutableList<RepoPath> {
        TODO("not implemented")
    }

    override fun archiveEntriesByName(query: String?, vararg repositories: String?): MutableList<RepoPath> {
        TODO("not implemented")
    }

    override fun artifactsBySha256(sha256: String?, vararg repositories: String?): MutableSet<RepoPath> {
        TODO("not implemented")
    }

    override fun artifactsByName(query: String, vararg repositories: String?): MutableList<RepoPath> {
        val repositoryQuery = if (repositories.isNotEmpty()) "&repos=${repositories.joinToString(",")}" else ""
        val searchResults = fuelManager.get("/api/search/artifact?name=$query$repositoryQuery")
            .responseObject<SearchResult>()
            .third.get()

        val basePath = fuelManager.basePath
        fuelManager.basePath = null

        val repoPaths = searchResults.results
            .map { it.uri }
            .map { fuelManager.get(it).responseObject<ItemInfoData>().third.get() }
            .map { PluginRepoPath(it.repo, it.path) }

        fuelManager.basePath = basePath
        return repoPaths.toMutableList()
    }
}

data class UriResult(val uri: String)
data class SearchResult(val results: List<UriResult>)