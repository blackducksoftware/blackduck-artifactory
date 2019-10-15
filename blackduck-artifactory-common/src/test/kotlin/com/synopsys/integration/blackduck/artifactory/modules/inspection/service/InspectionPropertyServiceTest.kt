package com.synopsys.integration.blackduck.artifactory.modules.inspection.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.synopsys.integration.blackduck.api.enumeration.PolicySeverityType
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty
import com.synopsys.integration.blackduck.artifactory.DateTimeManager
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate
import com.synopsys.integration.blackduck.service.ProjectService
import org.artifactory.repo.RepoPath
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

typealias Properties = MutableMap<String, String>

class InspectionPropertyServiceTest {

    private fun createRepoPath(repoPath: String = "test"): RepoPath {
        return PluginRepoPathFactory(false).create(repoPath)
    }

    private fun createInspectionPropertyService(artifactoryPAPIService: ArtifactoryPAPIService, dateTimeManager: DateTimeManager = mock(), projectService: ProjectService = mock(), retryCount: Int = 5): InspectionPropertyService {
        return InspectionPropertyService(artifactoryPAPIService, dateTimeManager, projectService, retryCount)
    }

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

        // Has property
        whenever(artifactoryPAPIService.hasProperty(any(), any())).doAnswer {
            val repoPath: RepoPath = it.getArgument(0)
            val propertyKey: String = it.getArgument(1)
            return@doAnswer repoPathPropertyMap[repoPath]?.get(propertyKey) != null
        }

        // Delete property
        whenever(artifactoryPAPIService.deleteProperty(any(), any())).then {
            val repoPath: RepoPath = it.getArgument(0)
            val propertyKey: String = it.getArgument(1)
            repoPathPropertyMap[repoPath]?.remove(propertyKey)
        }

        return artifactoryPAPIService
    }

    @Test
    fun hasExternalIdProperties() {
        val repoPath = createRepoPath()
        val artifactoryPAPIService = mock<ArtifactoryPAPIService>()
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

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
        val repoPath = createRepoPath()

        val repoPathPropertyMap = mutableMapOf<RepoPath, Properties>()
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val forgeProperty = BlackDuckArtifactoryProperty.BLACKDUCK_FORGE.propertyName
        val originIdProperty = BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID.propertyName

        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)
        inspectionPropertyService.setExternalIdProperties(repoPath, "Forge", "OriginId")

        val propertyMap = repoPathPropertyMap[repoPath]!!
        Assertions.assertTrue(propertyMap.containsKey(forgeProperty), "The $forgeProperty is missing from the properties.")
        Assertions.assertTrue(propertyMap.containsKey(originIdProperty), "The $originIdProperty is missing from the properties.")
        Assertions.assertEquals("Forge", propertyMap[forgeProperty])
        Assertions.assertEquals("OriginId", propertyMap[originIdProperty])
    }

    @Test
    fun shouldRetryInspection_NoProperties() {
        val repoPath = createRepoPath()
        val repoPathPropertyMap = mutableMapOf<RepoPath, Properties>()
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        val shouldRetryInspection = inspectionPropertyService.shouldRetryInspection(repoPath)
        Assertions.assertTrue(shouldRetryInspection, "Should have tried inspection because the RepoPath was lacking properties.")
    }

    @Test
    fun shouldRetryInspection_InspectionSuccess() {
        val repoPathPropertyMap = mutableMapOf<RepoPath, Properties>()
        val repoPath = createRepoPath()
        repoPathPropertyMap[repoPath] = mutableMapOf(
                BlackDuckArtifactoryProperty.INSPECTION_STATUS.propertyName to InspectionStatus.SUCCESS.name
        )

        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        val shouldRetryInspection = inspectionPropertyService.shouldRetryInspection(repoPath)
        Assertions.assertFalse(shouldRetryInspection, "Should not have tried inspection because the RepoPath was marked as SUCCESS.")
    }

    @Test
    fun shouldRetryInspection_InspectionFailure() {
        val repoPathPropertyMap = mutableMapOf<RepoPath, Properties>()
        val repoPath = createRepoPath()
        repoPathPropertyMap[repoPath] = mutableMapOf(
                BlackDuckArtifactoryProperty.INSPECTION_STATUS.propertyName to InspectionStatus.FAILURE.name,
                BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT.propertyName to "3"
        )

        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService, retryCount = 5)

        inspectionPropertyService.shouldRetryInspection(repoPath)

        val shouldRetryInspection = inspectionPropertyService.shouldRetryInspection(repoPath)
        Assertions.assertTrue(shouldRetryInspection, "Should have tried inspection because the RepoPath was marked FAILURE and the retry count was below the maximum.")
    }

    @Test
    fun shouldRetryInspection_InspectionFailureRetryLimit() {
        val repoPathPropertyMap = mutableMapOf<RepoPath, Properties>()
        val repoPath = createRepoPath()
        repoPathPropertyMap[repoPath] = mutableMapOf(
                BlackDuckArtifactoryProperty.INSPECTION_STATUS.propertyName to InspectionStatus.FAILURE.name,
                BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT.propertyName to "5"
        )

        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService, retryCount = 5)

        inspectionPropertyService.shouldRetryInspection(repoPath)

        val shouldRetryInspection = inspectionPropertyService.shouldRetryInspection(repoPath)
        Assertions.assertFalse(shouldRetryInspection, "Should not have tried inspection because the RepoPath was marked FAILURE and the retry count was below the maximum.")
    }

    @Test
    fun setVulnerabilityProperties() {
        val repoPath = createRepoPath()
        val repoPathPropertyMap = mutableMapOf<RepoPath, Properties>()
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        val vulnerabilityAggregate = VulnerabilityAggregate(1, 2, 3)

        inspectionPropertyService.setVulnerabilityProperties(repoPath, vulnerabilityAggregate)

        val highVulnerabilitiesProperty = BlackDuckArtifactoryProperty.HIGH_VULNERABILITIES.propertyName
        Assertions.assertNotNull(repoPathPropertyMap[repoPath]!![highVulnerabilitiesProperty], "Vulnerability property $highVulnerabilitiesProperty is missing.")
        Assertions.assertEquals("1", repoPathPropertyMap[repoPath]!![highVulnerabilitiesProperty], "The $highVulnerabilitiesProperty property was set incorrectly.")

        val mediumVulnerabilitiesProperty = BlackDuckArtifactoryProperty.MEDIUM_VULNERABILITIES.propertyName
        Assertions.assertNotNull(repoPathPropertyMap[repoPath]!![mediumVulnerabilitiesProperty], "Vulnerability property $mediumVulnerabilitiesProperty is missing.")
        Assertions.assertEquals("2", repoPathPropertyMap[repoPath]!![mediumVulnerabilitiesProperty], "The $mediumVulnerabilitiesProperty property was set incorrectly.")

        val lowVulnerabilitiesProperty = BlackDuckArtifactoryProperty.LOW_VULNERABILITIES.propertyName
        Assertions.assertNotNull(repoPathPropertyMap[repoPath]!![lowVulnerabilitiesProperty], "Vulnerability property $lowVulnerabilitiesProperty is missing.")
        Assertions.assertEquals("3", repoPathPropertyMap[repoPath]!![lowVulnerabilitiesProperty], "The $lowVulnerabilitiesProperty property was set incorrectly.")
    }

    @Test
    fun setPolicyProperties_NewPolicyViolation() {
        val repoPath = createRepoPath()
        val repoPathPropertyMap = mutableMapOf<RepoPath, Properties>()
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        val policyStatusReport = PolicyStatusReport(PolicySummaryStatusType.IN_VIOLATION, listOf(PolicySeverityType.MAJOR, PolicySeverityType.TRIVIAL))

        inspectionPropertyService.setPolicyProperties(repoPath, policyStatusReport)

        val policyStatusProperty = BlackDuckArtifactoryProperty.POLICY_STATUS.propertyName
        Assertions.assertNotNull(repoPathPropertyMap[repoPath]!![policyStatusProperty], "The $policyStatusProperty property is missing.")
        Assertions.assertEquals(PolicySummaryStatusType.IN_VIOLATION.name, repoPathPropertyMap[repoPath]!![policyStatusProperty], "The $policyStatusProperty property was set incorrectly.")

        val severityTypesProperty = BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES.propertyName
        Assertions.assertNotNull(repoPathPropertyMap[repoPath]!![severityTypesProperty], "The $severityTypesProperty property is missing.")
        Assertions.assertEquals("MAJOR,TRIVIAL", repoPathPropertyMap[repoPath]!![severityTypesProperty], "The $severityTypesProperty property was set incorrectly. It is either missing data or in an unexpected format.")
    }

    @Test
    fun setPolicyProperties_ClearedPolicyViolation() {
        val repoPath = createRepoPath()
        val repoPathPropertyMap = mutableMapOf<RepoPath, Properties>()
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        val policyStatusReport = PolicyStatusReport(PolicySummaryStatusType.NOT_IN_VIOLATION, listOf())
        inspectionPropertyService.setPolicyProperties(repoPath, policyStatusReport)

        val policyStatusProperty = BlackDuckArtifactoryProperty.POLICY_STATUS.propertyName
        Assertions.assertNotNull(repoPathPropertyMap[repoPath]!![policyStatusProperty], "The $policyStatusProperty property is missing.")
        Assertions.assertEquals(PolicySummaryStatusType.NOT_IN_VIOLATION.name, repoPathPropertyMap[repoPath]!![policyStatusProperty], "The $policyStatusProperty property was set incorrectly.")

        val severityTypesProperty = BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES.propertyName
        Assertions.assertNull(repoPathPropertyMap[repoPath]!![severityTypesProperty], "The $severityTypesProperty exists even though there or no PolicySeverityTypes in the PolicyStatusReport.")
    }

    @Test
    fun setComponentVersionUrl() {
        val repoPath = createRepoPath()
        val repoPathPropertyMap = mutableMapOf<RepoPath, Properties>()
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        inspectionPropertyService.setComponentVersionUrl(repoPath, "componentVersionUrl")

        val componentVersionUrlProperty = BlackDuckArtifactoryProperty.COMPONENT_VERSION_URL.propertyName
        Assertions.assertNotNull(repoPathPropertyMap[repoPath]!![componentVersionUrlProperty], "The $componentVersionUrlProperty property is missing.")
        Assertions.assertEquals("componentVersionUrl", repoPathPropertyMap[repoPath]!![componentVersionUrlProperty], "The $componentVersionUrlProperty property was set incorrectly.")
    }

    @Test
    fun failInspection() {
        val repoPath = createRepoPath()
        val repoPathPropertyMap = mutableMapOf<RepoPath, Properties>()
        repoPathPropertyMap[repoPath] = mutableMapOf(
                BlackDuckArtifactoryProperty.INSPECTION_STATUS.propertyName to InspectionStatus.SUCCESS.name
        )

        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val dateTimeManager = DateTimeManager("yyyy-MM-dd'T'HH:mm:ss.SSS")
        val maxRetryCount = 5
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService, dateTimeManager = dateTimeManager, retryCount = maxRetryCount)

        val inspectionStatusProperty = BlackDuckArtifactoryProperty.INSPECTION_STATUS.propertyName
        val retryCountProperty = BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT.propertyName
        val lastInspectionProperty = BlackDuckArtifactoryProperty.LAST_INSPECTION.propertyName
        val inspectionStatusMessageProperty = BlackDuckArtifactoryProperty.INSPECTION_STATUS_MESSAGE.propertyName
        for (i in 1..maxRetryCount + 3) {
            inspectionPropertyService.failInspection(repoPath, "failed for test")

            Assertions.assertNotNull(repoPathPropertyMap[repoPath]!![lastInspectionProperty], "The $lastInspectionProperty property is missing on attempt $i.")
            Assertions.assertNotNull(repoPathPropertyMap[repoPath]!![inspectionStatusProperty], "The $inspectionStatusProperty property is missing on attempt $i.")
            Assertions.assertNotNull(repoPathPropertyMap[repoPath]!![inspectionStatusMessageProperty], "The $inspectionStatusMessageProperty property is missing on attempt $i.")
            Assertions.assertEquals("failed for test", repoPathPropertyMap[repoPath]!![inspectionStatusMessageProperty], "The $inspectionStatusMessageProperty property was set incorrectly on attempt $i.")
            Assertions.assertNotNull(repoPathPropertyMap[repoPath]!![retryCountProperty], "The $retryCountProperty property is missing on attempt $i.")
        }

        Assertions.assertEquals(maxRetryCount.toString(), repoPathPropertyMap[repoPath]!![retryCountProperty], "The $retryCountProperty property exceeded or was below the maximum retry count.")
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
        val repoPath = createRepoPath()
        val repoPathPropertyMap = mutableMapOf<RepoPath, Properties>()
        repoPathPropertyMap[repoPath] = mutableMapOf(BlackDuckArtifactoryProperty.INSPECTION_STATUS.propertyName to InspectionStatus.SUCCESS.name)
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        val hasInspectionStatus = inspectionPropertyService.hasInspectionStatus(repoPath)
        Assertions.assertTrue(hasInspectionStatus, "RepoPath '${repoPath.toPath()}' should have a ${BlackDuckArtifactoryProperty.INSPECTION_STATUS} property.")
    }

    @Test
    fun getAllArtifactsInRepoWithInspectionStatus() {
    }

    @Test
    fun assertInspectionStatus() {
        val repoPath = createRepoPath()
        val repoPathPropertyMap = mutableMapOf<RepoPath, Properties>()
        repoPathPropertyMap[repoPath] = mutableMapOf(
                BlackDuckArtifactoryProperty.INSPECTION_STATUS.propertyName to InspectionStatus.SUCCESS.name
        )

        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        val failureMessage = "${BlackDuckArtifactoryProperty.INSPECTION_STATUS.propertyName} should have been ${InspectionStatus.SUCCESS.name}."

        val successAssertion = inspectionPropertyService.assertInspectionStatus(repoPath, InspectionStatus.SUCCESS)
        Assertions.assertTrue(successAssertion, failureMessage)

        val pendingAssertion = inspectionPropertyService.assertInspectionStatus(repoPath, InspectionStatus.PENDING)
        Assertions.assertFalse(pendingAssertion, failureMessage)

        val failureAssertion = inspectionPropertyService.assertInspectionStatus(repoPath, InspectionStatus.FAILURE)
        Assertions.assertFalse(failureAssertion, failureMessage)
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