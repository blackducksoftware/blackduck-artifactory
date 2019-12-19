/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2020 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.service

import PropertiesMap
import TestUtil.createMockArtifactoryPAPIService
import TestUtil.createRepoPath
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.synopsys.integration.blackduck.api.enumeration.PolicySeverityType
import com.synopsys.integration.blackduck.api.generated.component.ResourceMetadata
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty
import com.synopsys.integration.blackduck.artifactory.DateTimeManager
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory
import com.synopsys.integration.blackduck.artifactory.modules.UpdateStatus
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate
import com.synopsys.integration.util.HostNameHelper
import org.artifactory.repo.RepoPath
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*


class InspectionPropertyServiceTest {
    private fun createInspectionPropertyService(
            artifactoryPAPIService: ArtifactoryPAPIService,
            dateTimeManager: DateTimeManager = mock(),
            pluginRepoPathFactory: PluginRepoPathFactory = PluginRepoPathFactory(false),
            retryCount: Int = 5
    ): InspectionPropertyService {
        return InspectionPropertyService(artifactoryPAPIService, dateTimeManager, pluginRepoPathFactory, retryCount)
    }

    @Test
    fun hasExternalIdProperties() {
        val repoPath = createRepoPath()
        val artifactoryPAPIService = mock<ArtifactoryPAPIService>()
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        fun setReturns(hasOriginId: Boolean, hasForge: Boolean, hasComponentNameVersion: Boolean) {
            whenever(artifactoryPAPIService.hasProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_FORGE.propertyName)).thenReturn(hasForge)
            whenever(artifactoryPAPIService.hasProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID.propertyName)).thenReturn(hasOriginId)
            whenever(artifactoryPAPIService.hasProperty(repoPath, BlackDuckArtifactoryProperty.COMPONENT_NAME_VERSION.propertyName)).thenReturn(hasComponentNameVersion)
        }

        val possibleValues = listOf(true, false)

        for (hasOriginId in possibleValues) {
            for (hasForge in possibleValues) {
                for (hasComponentNameVersion in possibleValues) {
                    setReturns(hasOriginId, hasForge, hasComponentNameVersion)
                    val shouldBeTrue = hasOriginId && hasForge && hasComponentNameVersion
                    Assertions.assertEquals(shouldBeTrue, inspectionPropertyService.hasExternalIdProperties(repoPath))
                }
            }
        }
    }

    @Test
    fun setExternalIdProperties() {
        val repoPath = createRepoPath()

        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val forgeProperty = BlackDuckArtifactoryProperty.BLACKDUCK_FORGE.propertyName
        val originIdProperty = BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID.propertyName
        val componentNameProperty = BlackDuckArtifactoryProperty.COMPONENT_NAME_VERSION.propertyName

        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)
        inspectionPropertyService.setExternalIdProperties(repoPath, "Forge", "OriginId", "ComponentName", "ComponentVersionName")

        val propertyMap = repoPathPropertyMap[repoPath]!!

        Assertions.assertTrue(propertyMap.containsKey(forgeProperty), "The $forgeProperty is missing from the properties.")
        Assertions.assertTrue(propertyMap.containsKey(originIdProperty), "The $originIdProperty is missing from the properties.")
        Assertions.assertTrue(propertyMap.containsKey(componentNameProperty), "The $componentNameProperty is missing from the properties.")

        Assertions.assertEquals("Forge", propertyMap[forgeProperty])
        Assertions.assertEquals("OriginId", propertyMap[originIdProperty])
        Assertions.assertEquals(InspectionPropertyService.COMPONENT_NAME_VERSION_FORMAT.format("ComponentName", "ComponentVersionName"), propertyMap[componentNameProperty])
    }

    @Test
    fun shouldRetryInspection_NoProperties() {
        val repoPath = createRepoPath()
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        val shouldRetryInspection = inspectionPropertyService.shouldRetryInspection(repoPath)
        Assertions.assertTrue(shouldRetryInspection, "Should have tried inspection because the RepoPath was lacking properties.")
    }

    @Test
    fun shouldRetryInspection_InspectionSuccess() {
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
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
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
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
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
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
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
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
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
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
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
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
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
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
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
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
    fun setInspectionStatus() {
        val repoPath = createRepoPath()
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val dateTimeManager = DateTimeManager("yyyy-MM-dd'T'HH:mm:ss.SSS")
        val maxRetryCount = 5
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService, dateTimeManager = dateTimeManager, retryCount = maxRetryCount)

        val inspectionStatusProperty = BlackDuckArtifactoryProperty.INSPECTION_STATUS.propertyName
        val retryCountProperty = BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT.propertyName
        val lastInspectionProperty = BlackDuckArtifactoryProperty.LAST_INSPECTION.propertyName
        val inspectionStatusMessageProperty = BlackDuckArtifactoryProperty.INSPECTION_STATUS_MESSAGE.propertyName

        inspectionPropertyService.setInspectionStatus(repoPath, InspectionStatus.FAILURE)
        Assertions.assertEquals(InspectionStatus.FAILURE.name, repoPathPropertyMap[repoPath]!![inspectionStatusProperty])
        Assertions.assertTrue(repoPathPropertyMap[repoPath]!!.containsKey(lastInspectionProperty), "The $lastInspectionProperty property is missing.")
        Assertions.assertEquals(2, repoPathPropertyMap[repoPath]!!.size)

        inspectionPropertyService.setInspectionStatus(repoPath, InspectionStatus.FAILURE, "test-message")
        Assertions.assertEquals(InspectionStatus.FAILURE.name, repoPathPropertyMap[repoPath]!![inspectionStatusProperty])
        Assertions.assertEquals("test-message", repoPathPropertyMap[repoPath]!![inspectionStatusMessageProperty])
        Assertions.assertTrue(repoPathPropertyMap[repoPath]!!.containsKey(lastInspectionProperty), "The $lastInspectionProperty property is missing.")
        Assertions.assertEquals(3, repoPathPropertyMap[repoPath]!!.size)

        inspectionPropertyService.setInspectionStatus(repoPath, InspectionStatus.FAILURE, "test-message", 5)
        Assertions.assertEquals(InspectionStatus.FAILURE.name, repoPathPropertyMap[repoPath]!![inspectionStatusProperty])
        Assertions.assertEquals("test-message", repoPathPropertyMap[repoPath]!![inspectionStatusMessageProperty])
        Assertions.assertEquals("5", repoPathPropertyMap[repoPath]!![retryCountProperty])
        Assertions.assertTrue(repoPathPropertyMap[repoPath]!!.containsKey(lastInspectionProperty), "The $lastInspectionProperty property is missing.")
        Assertions.assertEquals(4, repoPathPropertyMap[repoPath]!!.size)

        inspectionPropertyService.setInspectionStatus(repoPath, InspectionStatus.SUCCESS)
        Assertions.assertEquals(InspectionStatus.SUCCESS.name, repoPathPropertyMap[repoPath]!![inspectionStatusProperty])
        Assertions.assertTrue(repoPathPropertyMap[repoPath]!!.containsKey(lastInspectionProperty), "The $lastInspectionProperty property is missing.")
        Assertions.assertEquals(2, repoPathPropertyMap[repoPath]!!.size)
    }

    @Test
    fun getInspectionStatus() {
        val repoPath = createRepoPath()
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
        repoPathPropertyMap[repoPath] = mutableMapOf(
                BlackDuckArtifactoryProperty.INSPECTION_STATUS.propertyName to InspectionStatus.SUCCESS.name
        )
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        val inspectionStatus = inspectionPropertyService.getInspectionStatus(repoPath)
        Assertions.assertEquals(InspectionStatus.SUCCESS, inspectionStatus, "RepoPath '${repoPath.toPath()}' should have a ${BlackDuckArtifactoryProperty.INSPECTION_STATUS} property.")
    }

    @Test
    fun hasInspectionStatus() {
        val repoPath = createRepoPath()
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
        repoPathPropertyMap[repoPath] = mutableMapOf(
                BlackDuckArtifactoryProperty.INSPECTION_STATUS.propertyName to InspectionStatus.SUCCESS.name
        )
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        val hasInspectionStatus = inspectionPropertyService.hasInspectionStatus(repoPath)
        Assertions.assertTrue(hasInspectionStatus, "RepoPath '${repoPath.toPath()}' should have a ${BlackDuckArtifactoryProperty.INSPECTION_STATUS} property.")
    }

    @Test
    fun getAllArtifactsInRepoWithInspectionStatus() {
        val repoPathSuccess = createRepoPath("test/valid")
        val repoPathPending = createRepoPath("test/pending")
        val repoPathFailure = createRepoPath("test/failure")
        val repoPathNoProperties = createRepoPath("test/no-properties")
        val inspectionStatusProperty = BlackDuckArtifactoryProperty.INSPECTION_STATUS.propertyName
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>(
                repoPathSuccess to mutableMapOf(
                        inspectionStatusProperty to InspectionStatus.SUCCESS.name
                ),
                repoPathPending to mutableMapOf(
                        inspectionStatusProperty to InspectionStatus.PENDING.name
                ),
                repoPathFailure to mutableMapOf(
                        inspectionStatusProperty to InspectionStatus.FAILURE.name
                ),
                repoPathNoProperties to mutableMapOf()
        )
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        InspectionStatus.values().forEach { inspectionStatus ->
            val foundArtifacts = inspectionPropertyService.getAllArtifactsInRepoWithInspectionStatus("test", InspectionStatus.SUCCESS)
            foundArtifacts.forEach { repoPath ->
                Assertions.assertEquals(inspectionStatus.name, repoPathPropertyMap[repoPath]!![inspectionStatusProperty], "The $inspectionStatusProperty should be set to ${inspectionStatus.name} on ${repoPath.toPath()}")
            }
        }
    }

    @Test
    fun assertInspectionStatus() {
        val repoPath = createRepoPath()
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
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
        val repoPathWithNoName = createRepoPath("test-repo-1")
        val repoPathWithName = createRepoPath("test-repo-2")
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
        repoPathPropertyMap[repoPathWithNoName] = mutableMapOf()
        repoPathPropertyMap[repoPathWithName] = mutableMapOf(
                BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME.propertyName to "project-name"
        )

        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        val projectNameProperty = inspectionPropertyService.getRepoProjectName(repoPathWithName.repoKey)
        Assertions.assertEquals("project-name", projectNameProperty, "The ${BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME.propertyName} property should be used as the source of the project name.")

        val projectNameNoProperty = inspectionPropertyService.getRepoProjectName(repoPathWithNoName.repoKey)
        Assertions.assertEquals("test-repo-1", projectNameNoProperty, "The repo key should be used as the source of the project name.")
    }

    @Test
    fun getRepoProjectVersionName() {
        val repoPathWithNoVersion = createRepoPath("test-repo-1")
        val repoPathWithVersion = createRepoPath("test-repo-2")
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
        repoPathPropertyMap[repoPathWithNoVersion] = mutableMapOf()
        repoPathPropertyMap[repoPathWithVersion] = mutableMapOf(
                BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME.propertyName to "project-version-name"
        )
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        val projectVersionNameProperty = inspectionPropertyService.getRepoProjectVersionName(repoPathWithVersion.repoKey)
        Assertions.assertEquals("project-version-name", projectVersionNameProperty, "The ${BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME.propertyName} property should be used as the source of the project version name.")

        val projectVersionNoProperty = inspectionPropertyService.getRepoProjectVersionName(repoPathWithNoVersion.repoKey)
        Assertions.assertEquals(HostNameHelper.getMyHostName("UNKNOWN_HOST"), projectVersionNoProperty, "The hostname should be used as the source of the project version name.")
    }

    @Test
    fun setRepoProjectNameProperties() {
        val repoPath = createRepoPath("test-repo-1")
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
        repoPathPropertyMap[repoPath] = mutableMapOf()
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        inspectionPropertyService.setRepoProjectNameProperties(repoPath.repoKey, "project-name", "project-version-name")

        assertPropertyWasSet(repoPath, repoPathPropertyMap, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME, "project-name")
        assertPropertyWasSet(repoPath, repoPathPropertyMap, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME, "project-version-name")
    }

    @Test
    fun updateProjectUIUrl() {
        val repoPath = createRepoPath("test-repo-1")
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
        repoPathPropertyMap[repoPath] = mutableMapOf()
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        val projectVersionView = ProjectVersionView()
        val resourceMetadata = ResourceMetadata()
        resourceMetadata.href = "https://synopsys.com"
        projectVersionView.meta = resourceMetadata
        inspectionPropertyService.updateProjectUIUrl(repoPath, projectVersionView)

        assertPropertyWasSet(repoPath, repoPathPropertyMap, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL, "https://synopsys.com")
    }

    @Test
    fun getLastUpdate() {
        val repoPath = createRepoPath("test-repo-1")
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
        repoPathPropertyMap[repoPath] = mutableMapOf(
                BlackDuckArtifactoryProperty.LAST_UPDATE.propertyName to "last-update"
        )
        val dateTimeManager = mock<DateTimeManager>()
        val expectedDate = Date()
        whenever(dateTimeManager.getDateFromString("last-update")).doReturn(expectedDate)

        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService, dateTimeManager = dateTimeManager)

        val lastUpdate: Date? = inspectionPropertyService.getLastUpdate(repoPath)
        Assertions.assertNotNull(lastUpdate, "The date retrieved should not be null.")
        Assertions.assertEquals(expectedDate, lastUpdate, "The last update retrieved differs. This may just be a mocking issue.")
    }

    @Test
    fun getLastInspection() {
        val repoPath = createRepoPath("test-repo-1")
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
        repoPathPropertyMap[repoPath] = mutableMapOf(
                BlackDuckArtifactoryProperty.LAST_INSPECTION.propertyName to "last-update"
        )
        val dateTimeManager = mock<DateTimeManager>()
        val expectedDate = Date()
        whenever(dateTimeManager.getDateFromString("last-update")).doReturn(expectedDate)
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService, dateTimeManager = dateTimeManager)

        val lastInspection = inspectionPropertyService.getLastInspection(repoPath)
        Assertions.assertNotNull(lastInspection, "The date retrieved should not be null.")
        Assertions.assertEquals(expectedDate, lastInspection, "The last inspection retrieved differs. This may just be a mocking issue.")
    }

    @Test
    fun setUpdateStatus() {
        val repoPath = createRepoPath("test-repo-1")
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
        repoPathPropertyMap[repoPath] = mutableMapOf()
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService)

        UpdateStatus.values().forEach { updateStatus ->
            inspectionPropertyService.setUpdateStatus(repoPath, updateStatus)

            assertPropertyWasSet(repoPath, repoPathPropertyMap, BlackDuckArtifactoryProperty.UPDATE_STATUS, updateStatus.name)
        }
    }

    @Test
    fun setLastUpdate() {
        val repoPath = createRepoPath("test-repo-1")
        val repoPathPropertyMap = mutableMapOf<RepoPath, PropertiesMap>()
        repoPathPropertyMap[repoPath] = mutableMapOf()

        val dateTimeManager = mock<DateTimeManager>()
        whenever(dateTimeManager.getStringFromDate(any())).doReturn("the-date")
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val inspectionPropertyService = createInspectionPropertyService(artifactoryPAPIService, dateTimeManager = dateTimeManager)

        inspectionPropertyService.setLastUpdate(repoPath, Date())

        assertPropertyWasSet(repoPath, repoPathPropertyMap, BlackDuckArtifactoryProperty.LAST_UPDATE, "the-date")
    }

    private fun assertPropertyWasSet(repoPath: RepoPath, repoPathPropertyMap: MutableMap<RepoPath, PropertiesMap>, property: BlackDuckArtifactoryProperty, expectedPropertyValue: String) {
        val propertyName = property.propertyName
        val propertyValue = repoPathPropertyMap[repoPath]!![propertyName]
        Assertions.assertNotNull(propertyValue, "The $propertyName should exist on the artifact.")
        Assertions.assertEquals(expectedPropertyValue, propertyValue, "The $propertyName property should be set to '$expectedPropertyValue'.")
    }
}