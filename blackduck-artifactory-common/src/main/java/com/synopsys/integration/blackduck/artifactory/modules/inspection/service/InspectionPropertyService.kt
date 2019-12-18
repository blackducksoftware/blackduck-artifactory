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

import com.google.common.collect.HashMultimap
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty
import com.synopsys.integration.blackduck.artifactory.DateTimeManager
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory
import com.synopsys.integration.blackduck.artifactory.modules.UpdateStatus
import com.synopsys.integration.blackduck.artifactory.modules.inspection.exception.FailedInspectionException
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate
import com.synopsys.integration.log.Slf4jIntLogger
import com.synopsys.integration.util.HostNameHelper
import org.apache.commons.lang3.StringUtils
import org.artifactory.repo.RepoPath
import org.slf4j.LoggerFactory
import java.util.*

class InspectionPropertyService(
        artifactoryPAPIService: ArtifactoryPAPIService,
        dateTimeManager: DateTimeManager,
        private val pluginRepoPathFactory: PluginRepoPathFactory,
        private val maxRetryCount: Int
) : ArtifactoryPropertyService(artifactoryPAPIService, dateTimeManager) {
    companion object {
        const val COMPONENT_NAME_VERSION_FORMAT: String = "%s-%s"
    }

    private val logger = Slf4jIntLogger(LoggerFactory.getLogger(this.javaClass))

    fun hasExternalIdProperties(repoPath: RepoPath): Boolean {
        return hasProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID)
                && hasProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_FORGE)
                && hasProperty(repoPath, BlackDuckArtifactoryProperty.COMPONENT_NAME_VERSION)
    }

    fun setExternalIdProperties(repoPath: RepoPath, forge: String, originId: String, componentName: String, componentVersionName: String) {
        setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID, originId, logger)
        setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_FORGE, forge, logger)
        setProperty(repoPath, BlackDuckArtifactoryProperty.COMPONENT_NAME_VERSION, COMPONENT_NAME_VERSION_FORMAT.format(componentName, componentVersionName), logger)
    }

    fun shouldRetryInspection(repoPath: RepoPath): Boolean {
        return !hasInspectionStatus(repoPath) || assertInspectionStatus(repoPath, InspectionStatus.FAILURE) && getFailedInspectionCount(repoPath) < maxRetryCount
    }

    fun setVulnerabilityProperties(repoPath: RepoPath, vulnerabilityAggregate: VulnerabilityAggregate) {
        setProperty(repoPath, BlackDuckArtifactoryProperty.HIGH_VULNERABILITIES, vulnerabilityAggregate.highSeverityCount.toString(), logger)
        setProperty(repoPath, BlackDuckArtifactoryProperty.MEDIUM_VULNERABILITIES, vulnerabilityAggregate.mediumSeverityCount.toString(), logger)
        setProperty(repoPath, BlackDuckArtifactoryProperty.LOW_VULNERABILITIES, vulnerabilityAggregate.lowSeverityCount.toString(), logger)
    }

    fun setPolicyProperties(repoPath: RepoPath, policyStatusReport: PolicyStatusReport) {
        if (policyStatusReport.policySeverityTypes.isEmpty()) {
            deleteProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES, logger)
        } else {
            val policySeverityTypes = StringUtils.join(policyStatusReport.policySeverityTypes, ",")
            setProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES, policySeverityTypes, logger)
        }
        setProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, policyStatusReport.policySummaryStatusType.name, logger)
    }

    fun setComponentVersionUrl(repoPath: RepoPath, componentVersionUrl: String) {
        setProperty(repoPath, BlackDuckArtifactoryProperty.COMPONENT_VERSION_URL, componentVersionUrl, logger)
    }

    fun failInspection(failedInspectionException: FailedInspectionException) {
        failInspection(failedInspectionException.repoPath, failedInspectionException.message)
    }

    fun failInspection(repoPath: RepoPath, inspectionStatusMessage: String?) {
        val retryCount = getFailedInspectionCount(repoPath) + 1
        logger.debug(String.format("Attempting to fail inspection for '%s' with message '%s'", repoPath.toPath(), inspectionStatusMessage))
        if (retryCount > maxRetryCount) {
            logger.debug(String.format("Attempting to fail inspection more than the number of maximum attempts: %s", repoPath.path))
        } else {
            setInspectionStatus(repoPath, InspectionStatus.FAILURE, inspectionStatusMessage, retryCount)
        }
    }

    private fun getFailedInspectionCount(repoPath: RepoPath): Int {
        return getPropertyAsInteger(repoPath, BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT) ?: 0
    }

    fun setInspectionStatus(repoPath: RepoPath, status: InspectionStatus, inspectionStatusMessage: String? = null, retryCount: Int? = null) {
        setPropertyFromDate(repoPath, BlackDuckArtifactoryProperty.LAST_INSPECTION, Date(), logger)
        setProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS, status.name, logger)

        if (!inspectionStatusMessage.isNullOrBlank()) {
            setProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS_MESSAGE, inspectionStatusMessage, logger)
        } else if (hasProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS_MESSAGE)) {
            deleteProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS_MESSAGE, logger)
        }

        if (retryCount != null) {
            setProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT, retryCount.toString(), logger)
        } else {
            deleteProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT, logger)
        }
    }

    fun getInspectionStatus(repoPath: RepoPath): InspectionStatus? {
        return getProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS)?.let { InspectionStatus.valueOf(it) }
    }

    fun hasInspectionStatus(repoPath: RepoPath): Boolean {
        return hasProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS)
    }

    fun getAllArtifactsInRepoWithInspectionStatus(repoKey: String, inspectionStatus: InspectionStatus): List<RepoPath> {
        val propertyMap = HashMultimap.create<String, String>()
        propertyMap.put(BlackDuckArtifactoryProperty.INSPECTION_STATUS.propertyName, inspectionStatus.name)
        return getItemsContainingPropertiesAndValues(propertyMap, repoKey)
    }

    fun assertInspectionStatus(repoPath: RepoPath, inspectionStatus: InspectionStatus): Boolean {
        return getInspectionStatus(repoPath)?.takeIf { inspectionStatus == it } != null
    }

    fun assertUpdateStatus(repoPath: RepoPath, updateStatus: UpdateStatus): Boolean {
        return getProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS)?.let { UpdateStatus.valueOf(it) }?.takeIf { updateStatus == it } != null
    }

    fun getRepoProjectName(repoKey: String): String {
        val repoPath = pluginRepoPathFactory.create(repoKey)
        val projectNameProperty = getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME)

        return projectNameProperty ?: repoKey
    }

    fun getRepoProjectVersionName(repoKey: String): String {
        val repoPath = pluginRepoPathFactory.create(repoKey)
        val projectVersionNameProperty = getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME)

        return projectVersionNameProperty ?: HostNameHelper.getMyHostName("UNKNOWN_HOST")
    }

    fun setRepoProjectNameProperties(repoKey: String, projectName: String, projectVersionName: String) {
        val repoPath = pluginRepoPathFactory.create(repoKey)
        setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME, projectName, logger)
        setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME, projectVersionName, logger)
    }

    fun updateProjectUIUrl(repoPath: RepoPath, projectVersionView: ProjectVersionView) {
        projectVersionView.href.ifPresent { uiUrl -> setProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL, uiUrl, logger) }
    }

    fun getLastUpdate(repoKeyPath: RepoPath): Date? {
        return getDateFromProperty(repoKeyPath, BlackDuckArtifactoryProperty.LAST_UPDATE)
    }

    fun getLastInspection(repoKeyPath: RepoPath): Date? {
        return getDateFromProperty(repoKeyPath, BlackDuckArtifactoryProperty.LAST_INSPECTION)
    }

    fun setUpdateStatus(repoKeyPath: RepoPath, updateStatus: UpdateStatus) {
        setProperty(repoKeyPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, updateStatus.toString(), logger)
    }

    fun setLastUpdate(repoKeyPath: RepoPath, lastNotificationDate: Date) {
        setPropertyFromDate(repoKeyPath, BlackDuckArtifactoryProperty.LAST_UPDATE, lastNotificationDate, logger)
    }
}
