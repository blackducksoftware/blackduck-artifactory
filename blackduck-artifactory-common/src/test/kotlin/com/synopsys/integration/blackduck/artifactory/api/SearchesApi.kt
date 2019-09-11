package com.synopsys.integration.blackduck.artifactory.api

import com.github.kittinunf.fuel.core.FuelManager
import com.google.common.collect.SetMultimap
import org.artifactory.build.BuildRun
import org.artifactory.repo.RepoPath
import org.artifactory.search.Searches
import org.artifactory.search.aql.AqlResultHandler
import java.util.*

class SearchesApi(fuelManager: FuelManager) : Searches {
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

    override fun artifactsByName(query: String?, vararg repositories: String?): MutableList<RepoPath> {
        TODO("not implemented")
    }
}