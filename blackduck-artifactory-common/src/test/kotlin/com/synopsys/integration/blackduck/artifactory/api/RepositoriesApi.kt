package com.synopsys.integration.blackduck.artifactory.api

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.gson.responseObject
import org.artifactory.common.StatusHolder
import org.artifactory.fs.FileInfo
import org.artifactory.fs.FileLayoutInfo
import org.artifactory.fs.ItemInfo
import org.artifactory.fs.StatsInfo
import org.artifactory.md.Properties
import org.artifactory.repo.RepoPath
import org.artifactory.repo.Repositories
import org.artifactory.repo.RepositoryConfiguration
import org.artifactory.resource.ResourceStreamHandle
import java.io.InputStream

class RepositoriesApi(private val fuelManager: FuelManager) : Repositories {
    override fun getPropertyValues(repoPath: RepoPath?, propertyName: String?): MutableSet<String> {
        TODO("not implemented")
    }

    override fun isRepoPathAccepted(repoPath: RepoPath?): Boolean {
        TODO("not implemented")
    }

    override fun deleteAtomic(repoPath: RepoPath?): StatusHolder {
        TODO("not implemented")
    }

    override fun getRepositoryConfiguration(repoKey: String?): RepositoryConfiguration {
        return fuelManager.get("/api/repositories/$repoKey")
            .responseObject<RepositoryConfigurationImpl>()
            .third.get()
    }

    override fun copy(source: RepoPath?, target: RepoPath?): StatusHolder {
        TODO("not implemented")
    }

    override fun getLocalRepositories(): MutableList<String> {
        TODO("not implemented")
    }

    override fun getProperties(repoPath: RepoPath?): Properties {
        TODO("not implemented")
    }

    override fun getProperty(repoPath: RepoPath?, propertyName: String?): String {
        TODO("not implemented")
    }

    override fun isLocalRepoPathAccepted(repoPath: RepoPath?): Boolean {
        TODO("not implemented")
    }

    override fun getLayoutInfo(repoPath: RepoPath?): FileLayoutInfo {
        TODO("not implemented")
    }

    override fun getStats(repoPath: RepoPath?): StatsInfo? {
        TODO("not implemented")
    }

    override fun getVirtualRepositories(): MutableList<String> {
        TODO("not implemented")
    }

    override fun getContent(repoPath: RepoPath?): ResourceStreamHandle {
        TODO("not implemented")
    }

    override fun getArtifactsCount(repoPath: RepoPath): Long {
        if (repoPath.isRoot) {
            val repositorySummary = getStorageInfo().repositoriesSummaryList
                .first { it.repoKey == repoPath.repoKey }
            return repositorySummary.filesCount
        } else {
            TODO("not implemented")
        }
    }

    private fun getStorageInfo(): StorageInfoSummary {
        return fuelManager.get("/api/storageinfo")
            .responseObject<StorageInfoSummary>()
            .third.get()
    }

    override fun getItemInfo(repoPath: RepoPath?): ItemInfo {
        TODO("not implemented")
    }

    override fun deploy(repoPath: RepoPath?, inputStream: InputStream?): StatusHolder {
        TODO("not implemented")
    }

    override fun copyAtomic(source: RepoPath?, target: RepoPath?): StatusHolder {
        TODO("not implemented")
    }

    override fun delete(repoPath: RepoPath?): StatusHolder {
        TODO("not implemented")
    }

    override fun getDescriptorRepoPath(layoutInfo: FileLayoutInfo?, repoKey: String?): RepoPath {
        TODO("not implemented")
    }

    override fun hasProperty(repoPath: RepoPath?, propertyName: String?): Boolean {
        TODO("not implemented")
    }

    override fun isLcoalRepoPathHandled(repoPath: RepoPath?): Boolean {
        TODO("not implemented")
    }

    override fun getRemoteRepositories(): MutableList<String> {
        TODO("not implemented")
    }

    override fun getArtifactRepoPath(layoutInfo: FileLayoutInfo?, repoKey: String?): RepoPath {
        TODO("not implemented")
    }

    override fun getChildren(repoPath: RepoPath?): MutableList<ItemInfo> {
        TODO("not implemented")
    }

    override fun isRepoPathHandled(repoPath: RepoPath?): Boolean {
        TODO("not implemented")
    }

    override fun setProperty(repoPath: RepoPath?, propertyName: String?, vararg values: String?): Properties {
        TODO("not implemented")
    }

    override fun getArtifactsSize(repoPath: RepoPath?): Long {
        TODO("not implemented")
    }

    override fun getStringContent(fileInfo: FileInfo?): String {
        TODO("not implemented")
    }

    override fun getStringContent(repoPath: RepoPath?): String {
        TODO("not implemented")
    }

    override fun deleteProperty(repoPath: RepoPath?, propertyName: String?) {
        TODO("not implemented")
    }

    override fun undeploy(repoPath: RepoPath?): StatusHolder {
        TODO("not implemented")
    }

    override fun translateFilePath(source: RepoPath?, targetRepoKey: String?): String {
        TODO("not implemented")
    }

    override fun setPropertyRecursively(repoPath: RepoPath?, propertyName: String?, vararg values: String?): Properties {
        TODO("not implemented")
    }

    override fun moveAtomic(source: RepoPath?, target: RepoPath?): StatusHolder {
        TODO("not implemented")
    }

    override fun move(source: RepoPath?, target: RepoPath?): StatusHolder {
        TODO("not implemented")
    }

    override fun exists(repoPath: RepoPath?): Boolean {
        TODO("not implemented")
    }

    override fun getFileInfo(repoPath: RepoPath?): FileInfo {
        TODO("not implemented")
    }
}

data class RepositorySummary(val repoKey: String, val repoType: String, val foldersCount: Long, val filesCount: Long, val usedSpace: String, val itemsCount: Long, val packageType: String, val percentage: String)
data class StorageInfoSummary(val repositoriesSummaryList: List<RepositorySummary>)