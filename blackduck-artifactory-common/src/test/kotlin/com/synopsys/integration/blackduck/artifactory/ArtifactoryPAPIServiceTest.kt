package com.synopsys.integration.blackduck.artifactory

import com.synopsys.integration.blackduck.artifactory.api.ArtifactoryIntegrationTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf

internal class ArtifactoryPAPIServiceTest : ArtifactoryIntegrationTest() {
    private var artifactoryPAPIService: ArtifactoryPAPIService = ArtifactoryPAPIService(PluginRepoPathFactory(false), repositories, searches)

    @Test
    @EnabledIf(ARTIFACTORY_ENVIRONMENT_PRESENT)
    fun getPackageType() {
        val packageType = artifactoryPAPIService.getPackageType("composer-remote")
        Assertions.assertEquals("composer", packageType.get())
    }

    @Test
    @EnabledIf(ARTIFACTORY_ENVIRONMENT_PRESENT)
    fun getArtifactCount() {
        val artifactCount = artifactoryPAPIService.getArtifactCount(listOf("cocoapods-remote-cache"))
        Assertions.assertEquals(0, artifactCount)
    }

    @Test
    @EnabledIf(ARTIFACTORY_ENVIRONMENT_PRESENT)
    fun getItemInfo() {
    }

    @Test
    @EnabledIf(ARTIFACTORY_ENVIRONMENT_PRESENT)
    fun isValidRepository() {
    }

    @Test
    @EnabledIf(ARTIFACTORY_ENVIRONMENT_PRESENT)
    fun searchForArtifactsByPatterns() {
    }

    @Test
    @EnabledIf(ARTIFACTORY_ENVIRONMENT_PRESENT)
    fun getLayoutInfo() {
    }

    @Test
    @EnabledIf(ARTIFACTORY_ENVIRONMENT_PRESENT)
    fun getContent() {
    }

    @Test
    @EnabledIf(ARTIFACTORY_ENVIRONMENT_PRESENT)
    fun getProperties() {
    }

    @Test
    @EnabledIf(ARTIFACTORY_ENVIRONMENT_PRESENT)
    fun hasProperty() {
    }

    @Test
    @EnabledIf(ARTIFACTORY_ENVIRONMENT_PRESENT)
    fun getProperty() {
    }

    @Test
    @EnabledIf(ARTIFACTORY_ENVIRONMENT_PRESENT)
    fun setProperty() {
    }

    @Test
    @EnabledIf(ARTIFACTORY_ENVIRONMENT_PRESENT)
    fun deleteProperty() {
    }

    @Test
    @EnabledIf(ARTIFACTORY_ENVIRONMENT_PRESENT)
    fun itemsByProperties() {
    }

    @Test
    @EnabledIf(ARTIFACTORY_ENVIRONMENT_PRESENT)
    fun itemsByName() {
    }

    @Test
    @EnabledIf(ARTIFACTORY_ENVIRONMENT_PRESENT)
    fun getArtifactContent() {
    }
}