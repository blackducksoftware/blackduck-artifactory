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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.modules.UpdateStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.exception.FailedInspectionException;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.HostNameHelper;

public class InspectionPropertyService extends ArtifactoryPropertyService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));
    public static final String COMPONENT_NAME_VERSION_FORMAT = "%s-%s";

    private final PluginRepoPathFactory pluginRepoPathFactory;
    private final int maxRetryCount;

    public InspectionPropertyService(ArtifactoryPAPIService artifactoryPAPIService, DateTimeManager dateTimeManager, PluginRepoPathFactory pluginRepoPathFactory, int maxRetryCount) {
        super(artifactoryPAPIService, dateTimeManager);
        this.pluginRepoPathFactory = pluginRepoPathFactory;
        this.maxRetryCount = maxRetryCount;
    }

    public boolean hasExternalIdProperties(RepoPath repoPath) {
        return hasProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID)
                   && hasProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_FORGE)
                   && hasProperty(repoPath, BlackDuckArtifactoryProperty.COMPONENT_NAME_VERSION);
    }

    public void setExternalIdProperties(RepoPath repoPath, String forge, String originId, String componentName, String componentVersionName) {
        setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID, originId, logger);
        setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_FORGE, forge, logger);
        setProperty(repoPath, BlackDuckArtifactoryProperty.COMPONENT_NAME_VERSION, String.format(COMPONENT_NAME_VERSION_FORMAT, componentName, componentVersionName), logger);
    }

    public boolean shouldRetryInspection(RepoPath repoPath) {
        return !hasInspectionStatus(repoPath) || assertInspectionStatus(repoPath, InspectionStatus.FAILURE) && getFailedInspectionCount(repoPath) < maxRetryCount;
    }

    public void setVulnerabilityProperties(RepoPath repoPath, VulnerabilityAggregate vulnerabilityAggregate) {
        setProperty(repoPath, BlackDuckArtifactoryProperty.CRITICAL_VULNERABILITIES, String.valueOf(vulnerabilityAggregate.getCriticalSeverityCount()), logger);
        setProperty(repoPath, BlackDuckArtifactoryProperty.HIGH_VULNERABILITIES, String.valueOf(vulnerabilityAggregate.getHighSeverityCount()), logger);
        setProperty(repoPath, BlackDuckArtifactoryProperty.MEDIUM_VULNERABILITIES, String.valueOf(vulnerabilityAggregate.getMediumSeverityCount()), logger);
        setProperty(repoPath, BlackDuckArtifactoryProperty.LOW_VULNERABILITIES, String.valueOf(vulnerabilityAggregate.getLowSeverityCount()), logger);
    }

    public void setPolicyProperties(RepoPath repoPath, PolicyStatusReport policyStatusReport) {
        if (policyStatusReport.getPolicySeverityTypes().isEmpty()) {
            deleteProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES, logger);
        } else {
            String policySeverityTypes = StringUtils.join(policyStatusReport.getPolicySeverityTypes(), ",");
            setProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES, policySeverityTypes, logger);
        }
        setProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, policyStatusReport.getPolicySummaryStatusType().name(), logger);
    }

    public void setComponentVersionUrl(RepoPath repoPath, String componentVersionUrl) {
        setProperty(repoPath, BlackDuckArtifactoryProperty.COMPONENT_VERSION_URL, componentVersionUrl, logger);
    }

    public void failInspection(FailedInspectionException failedInspectionException) {
        failInspection(failedInspectionException.getRepoPath(), failedInspectionException.getMessage());
    }

    public void failInspection(RepoPath repoPath, @Nullable String inspectionStatusMessage) {
        int retryCount = getFailedInspectionCount(repoPath) + 1;
        logger.debug(String.format("Attempting to fail inspection for '%s' with message '%s'", repoPath.toPath(), inspectionStatusMessage));
        if (retryCount > maxRetryCount) {
            logger.debug(String.format("Attempting to fail inspection more than the number of maximum attempts: %s", repoPath.getPath()));
        } else {
            setInspectionStatus(repoPath, InspectionStatus.FAILURE, inspectionStatusMessage, retryCount);
        }
    }

    private int getFailedInspectionCount(RepoPath repoPath) {
        return getPropertyAsInteger(repoPath, BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT).orElse(0);
    }

    public void setInspectionStatus(RepoPath repoPath, InspectionStatus status) {
        setInspectionStatus(repoPath, status, null);
    }

    public void setInspectionStatus(RepoPath repoPath, InspectionStatus status, @Nullable String inspectionStatusMessage) {
        setInspectionStatus(repoPath, status, inspectionStatusMessage, null);
    }

    public void setInspectionStatus(RepoPath repoPath, InspectionStatus status, @Nullable String inspectionStatusMessage, @Nullable Integer retryCount) {
        setPropertyFromDate(repoPath, BlackDuckArtifactoryProperty.LAST_INSPECTION, new Date(), logger);
        setProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS, status.name(), logger);

        if (StringUtils.isNotBlank(inspectionStatusMessage)) {
            setProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS_MESSAGE, inspectionStatusMessage, logger);
        } else if (hasProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS_MESSAGE)) {
            deleteProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS_MESSAGE, logger);
        }

        if (retryCount != null) {
            setProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT, retryCount.toString(), logger);
        } else {
            deleteProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT, logger);
        }
    }

    public Optional<InspectionStatus> getInspectionStatus(RepoPath repoPath) {
        return getProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS)
                   .map(InspectionStatus::valueOf);
    }

    public boolean hasInspectionStatus(RepoPath repoPath) {
        return hasProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS);
    }

    public List<RepoPath> getAllArtifactsInRepoWithInspectionStatus(String repoKey, InspectionStatus inspectionStatus) {
        SetMultimap<String, String> propertyMap = HashMultimap.create();
        propertyMap.put(BlackDuckArtifactoryProperty.INSPECTION_STATUS.getPropertyName(), inspectionStatus.name());
        return getItemsContainingPropertiesAndValues(propertyMap, repoKey);
    }

    public boolean assertInspectionStatus(RepoPath repoPath, InspectionStatus inspectionStatus) {
        return getInspectionStatus(repoPath)
                   .filter(inspectionStatus::equals)
                   .isPresent();
    }

    public boolean assertUpdateStatus(RepoPath repoPath, UpdateStatus updateStatus) {
        return getProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS)
                   .map(UpdateStatus::valueOf)
                   .filter(updateStatus::equals)
                   .isPresent();
    }

    public String getRepoProjectName(String repoKey) {
        RepoPath repoPath = pluginRepoPathFactory.create(repoKey);
        Optional<String> projectNameProperty = getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME);

        return projectNameProperty.orElse(repoKey);
    }

    public String getRepoProjectVersionName(String repoKey) {
        RepoPath repoPath = pluginRepoPathFactory.create(repoKey);
        Optional<String> projectVersionNameProperty = getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME);

        return projectVersionNameProperty.orElse(HostNameHelper.getMyHostName("UNKNOWN_HOST"));
    }

    public void setRepoProjectNameProperties(String repoKey, String projectName, String projectVersionName) {
        RepoPath repoPath = pluginRepoPathFactory.create(repoKey);
        setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME, projectName, logger);
        setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME, projectVersionName, logger);
    }

    public void updateProjectUIUrl(RepoPath repoPath, ProjectVersionView projectVersionView) {
        Optional<String> componentsLink = projectVersionView.getFirstLink(ProjectVersionView.COMPONENTS_LINK);
        componentsLink.ifPresent(uiUrl -> setProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL, uiUrl, logger));
    }

    public Optional<Date> getLastUpdate(RepoPath repoKeyPath) {
        return getDateFromProperty(repoKeyPath, BlackDuckArtifactoryProperty.LAST_UPDATE);
    }

    public Optional<Date> getLastInspection(RepoPath repoKeyPath) {
        return getDateFromProperty(repoKeyPath, BlackDuckArtifactoryProperty.LAST_INSPECTION);
    }

    public void setUpdateStatus(RepoPath repoKeyPath, UpdateStatus updateStatus) {
        setProperty(repoKeyPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, updateStatus.toString(), logger);
    }

    public void setLastUpdate(RepoPath repoKeyPath, Date lastNotificationDate) {
        setPropertyFromDate(repoKeyPath, BlackDuckArtifactoryProperty.LAST_UPDATE, lastNotificationDate, logger);
    }
}
