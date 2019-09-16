package com.synopsys.integration.blackduck.artifactory

import com.synopsys.integration.blackduck.artifactory.api.ArtifactoryIntegrationTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf

@EnabledIf(ArtifactoryIntegrationTest.ARTIFACTORY_ENVIRONMENT_PRESENT)
internal class ArtifactoryPAPIServiceTest : ArtifactoryIntegrationTest() {
    private var artifactoryPAPIService: ArtifactoryPAPIService = ArtifactoryPAPIService(PluginRepoPathFactory(false), repositories, searches)

    @Test
    fun getPackageType() {
        val packageType = artifactoryPAPIService.getPackageType("composer-remote")
        Assertions.assertEquals("composer", packageType.get())
    }

    @Test
    fun getArtifactCount() {
        val artifactCount = artifactoryPAPIService.getArtifactCount(listOf("cocoapods-remote-cache"))
        Assertions.assertEquals(0, artifactCount)
    }

    @Test
    fun getItemInfo() {
        val repoPath = PluginRepoPathFactory(false).create("bower-remote-cache/angular-ui/angular-ui-router-bower/tags/1.0.22/angular-ui-router-bower-1.0.22.tar.gz")
        val itemInfo = artifactoryPAPIService.getItemInfo(repoPath)
        Assertions.assertEquals("angular-ui-router-bower-1.0.22.tar.gz", itemInfo.name)
        Assertions.assertTrue(0 < itemInfo.lastModified)
    }

    @Test
    fun isValidRepository() {
        Assertions.assertTrue(artifactoryPAPIService.isValidRepository("cran-remote"))
        Assertions.assertFalse(artifactoryPAPIService.isValidRepository("some invalid repo key"))
    }

    @Test
    fun searchForArtifactsByPatterns() {
        val repoPaths = artifactoryPAPIService.searchForArtifactsByPatterns(listOf("cran-remote-cache"), listOf("*.tar.gz"))
        Assertions.assertEquals(1, repoPaths.size)
        Assertions.assertEquals("cran-remote-cache/src/contrib/fortunes_1.5-4.tar.gz", repoPaths[0].toPath())
    }

    @Test
    fun getLayoutInfo() {
    }

    @Test
    fun getContent() {
    }

    @Test
    fun getProperties() {
    }

    @Test
    fun hasProperty() {
    }

    @Test
    fun getProperty() {
    }

    @Test
    fun setProperty() {
    }

    @Test
    fun deleteProperty() {
    }

    @Test
    fun itemsByProperties() {
    }

    @Test
    fun itemsByName() {
    }

    @Test
    fun getArtifactContent() {
    }
}