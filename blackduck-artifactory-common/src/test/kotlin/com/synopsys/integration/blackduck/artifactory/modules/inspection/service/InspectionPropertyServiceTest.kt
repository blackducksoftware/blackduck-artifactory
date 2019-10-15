package com.synopsys.integration.blackduck.artifactory.modules.inspection.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty
import com.synopsys.integration.blackduck.artifactory.DateTimeManager
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory
import com.synopsys.integration.blackduck.service.ProjectService
import org.artifactory.repo.RepoPath
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

typealias Properties = MutableMap<String, String>

class InspectionPropertyServiceTest {

    private fun createMockArtifactoryPAPIService(repoPathPropertyMap: MutableMap<RepoPath, Properties>): ArtifactoryPAPIService {
        val artifactoryPAPIService = mock<ArtifactoryPAPIService>()

        // Set property
        whenever(artifactoryPAPIService.setProperty(any(), any(), any())).then {
            val repoPath: RepoPath = it.getArgument(0)
            val propertyKey: String = it.getArgument(1)
            val propertyValue: String = it.getArgument(2)
            val properties = repoPathPropertyMap.getOrPut(repoPath, defaultValue = { mutableMapOf() })
            properties[propertyKey] = propertyValue
            repoPathPropertyMap.put(repoPath, properties)
        }

        // Get property
        whenever(artifactoryPAPIService.getProperty(any(), any())).doAnswer {
            val repoPath: RepoPath = it.getArgument(0)
            val propertyKey: String = it.getArgument(1)
            return@doAnswer repoPathPropertyMap[repoPath]?.get(propertyKey)
        }
        return artifactoryPAPIService
    }

    @Test
    fun hasExternalIdProperties() {
        val repoPath = PluginRepoPathFactory(false).create("test")
        val artifactoryPAPIService = mock<ArtifactoryPAPIService>()
        val dateTimeManager = mock<DateTimeManager>()
        val projectService = mock<ProjectService>()
        val inspectionPropertyService = InspectionPropertyService(artifactoryPAPIService, dateTimeManager, projectService, 5)

        fun setReturns(hasOriginId: Boolean, hasForge: Boolean) {
            whenever(artifactoryPAPIService.hasProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_FORGE.propertyName)).thenReturn(hasForge)
            whenever(artifactoryPAPIService.hasProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID.propertyName)).thenReturn(hasOriginId)
        }

        setReturns(true, true)
        Assertions.assertTrue(inspectionPropertyService.hasExternalIdProperties(repoPath))

        setReturns(true, false)
        Assertions.assertFalse(inspectionPropertyService.hasExternalIdProperties(repoPath))

        setReturns(false, true)
        Assertions.assertFalse(inspectionPropertyService.hasExternalIdProperties(repoPath))

        setReturns(false, false)
        Assertions.assertFalse(inspectionPropertyService.hasExternalIdProperties(repoPath))
    }

    @Test
    fun setExternalIdProperties() {
        val repoPath = PluginRepoPathFactory(false).create("test")
        val dateTimeManager = mock<DateTimeManager>()
        val projectService = mock<ProjectService>()

        val repoPathPropertyMap = mutableMapOf<RepoPath, Properties>()
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val forgeProperty = BlackDuckArtifactoryProperty.BLACKDUCK_FORGE.propertyName
        val originIdProperty = BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID.propertyName

        val inspectionPropertyService = InspectionPropertyService(artifactoryPAPIService, dateTimeManager, projectService, 5)
        inspectionPropertyService.setExternalIdProperties(repoPath, "Forge", "OriginId")

        val propertyMap = repoPathPropertyMap[repoPath]!!
        Assertions.assertTrue(propertyMap.containsKey(forgeProperty), "The $forgeProperty is missing from the properties.")
        Assertions.assertTrue(propertyMap.containsKey(originIdProperty), "The $originIdProperty is missing from the properties.")
        Assertions.assertEquals("Forge", propertyMap[forgeProperty])
        Assertions.assertEquals("OriginId", propertyMap[originIdProperty])
    }

    @Test
    fun shouldRetryInspection() {
    }

    @Test
    fun setVulnerabilityProperties() {
    }

    @Test
    fun setPolicyProperties() {
    }

    @Test
    fun setComponentVersionUrl() {
    }

    @Test
    fun failInspection() {
    }

    @Test
    fun testFailInspection() {
    }

    @Test
    fun setInspectionStatus() {
    }

    @Test
    fun getInspectionStatus() {
    }

    @Test
    fun hasInspectionStatus() {
    }

    @Test
    fun getAllArtifactsInRepoWithInspectionStatus() {
    }

    @Test
    fun assertInspectionStatus() {

    }

    @Test
    fun getRepoProjectName() {
    }

    @Test
    fun getRepoProjectVersionName() {
    }

    @Test
    fun setRepoProjectNameProperties() {
    }

    @Test
    fun updateProjectUIUrl() {
    }

    @Test
    fun testUpdateProjectUIUrl() {
    }

    @Test
    fun getLastUpdate() {
    }

    @Test
    fun getLastInspection() {
    }

    @Test
    fun setUpdateStatus() {
    }

    @Test
    fun setLastUpdate() {
    }
}