package com.synopsys.integration.blackduck.artifactory.api.model

import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory
import com.synopsys.integration.blackduck.artifactory.api.ArtifactoryIntegrationTest.Companion.ARTIFACTORY_DATE_FORMAT
import org.artifactory.fs.ItemInfo
import org.artifactory.repo.RepoPath
import java.io.File

data class ItemInfoData(val uri: String?, val downloadUri: String?, val repo: String, val path: String, val remoteUrl: String?, val created: String, val createdBy: String, val lastModified: String, val modifiedBy: String,
    val lastUpdated: String, val size: String?)

class TestItemInfo(private val repoPathFactory: PluginRepoPathFactory, private val itemInfoData: ItemInfoData) : ItemInfo {
    override fun getModifiedBy(): String {
        return itemInfoData.modifiedBy
    }

    override fun getRepoPath(): RepoPath {
        return repoPathFactory.create(itemInfoData.repo, itemInfoData.path)
    }

    override fun getName(): String {
        return File(itemInfoData.path).name
    }

    override fun getId(): Long {
        TODO("not implemented")
    }

    override fun isIdentical(info: ItemInfo?): Boolean {
        TODO("not implemented")
    }

    override fun compareTo(other: ItemInfo?): Int {
        TODO("not implemented")
    }

    override fun getLastUpdated(): Long {
        return ARTIFACTORY_DATE_FORMAT.parse(itemInfoData.lastUpdated).time
    }

    override fun getRelPath(): String {
        TODO("not implemented")
    }

    override fun getCreatedBy(): String {
        return itemInfoData.createdBy
    }

    override fun getCreated(): Long {
        return ARTIFACTORY_DATE_FORMAT.parse(itemInfoData.created).time
    }

    override fun getLastModified(): Long {
        return ARTIFACTORY_DATE_FORMAT.parse(itemInfoData.lastModified).time
    }

    override fun getRepoKey(): String {
        return itemInfoData.repo
    }

    override fun isFolder(): Boolean {
        return itemInfoData.downloadUri.isNullOrBlank()
    }

}