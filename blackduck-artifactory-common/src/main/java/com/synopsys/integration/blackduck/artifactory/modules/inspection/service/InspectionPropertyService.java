/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.UpdateStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.exception.FailedInspectionException;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.PolicyVulnerabilityAggregate;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.HostNameHelper;

public class InspectionPropertyService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final ProjectService projectService;
    private final InspectionModuleConfig inspectionModuleConfig;

    public InspectionPropertyService(final ArtifactoryPropertyService artifactoryPropertyService, final ProjectService projectService,
        final InspectionModuleConfig inspectionModuleConfig) {
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.projectService = projectService;
        this.inspectionModuleConfig = inspectionModuleConfig;
    }

    public boolean hasExternalIdProperties(final RepoPath repoPath) {
        return artifactoryPropertyService.hasProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID) && artifactoryPropertyService.hasProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_FORGE);
    }

    public void setExternalIdProperties(final RepoPath repoPath, final String forge, final String originId) {
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID, originId, logger);
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_FORGE, forge, logger);
    }

    public boolean shouldRetryInspection(final RepoPath repoPath) {
        return assertInspectionStatus(repoPath, InspectionStatus.FAILURE) && getFailedInspectionCount(repoPath) < inspectionModuleConfig.getRetryCount();
    }

    public void setPolicyAndVulnerabilityProperties(final RepoPath repoPath, final PolicyVulnerabilityAggregate policyVulnerabilityAggregate) {
        this.populatePolicyAndVulnerabilityProperties(repoPath, policyVulnerabilityAggregate, true);
    }

    public void updatePolicyAndVulnerabilityProperties(final RepoPath repoPath, final PolicyVulnerabilityAggregate policyVulnerabilityAggregate) {
        this.populatePolicyAndVulnerabilityProperties(repoPath, policyVulnerabilityAggregate, false);
    }

    private void populatePolicyAndVulnerabilityProperties(final RepoPath repoPath, final PolicyVulnerabilityAggregate policyVulnerabilityAggregate, final boolean overrideValues) {
        if (overrideValues || policyVulnerabilityAggregate.hasVulnerabilityData()) {
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.HIGH_VULNERABILITIES, policyVulnerabilityAggregate.getHighVulnerabilities(), logger);
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.MEDIUM_VULNERABILITIES, policyVulnerabilityAggregate.getMediumVulnerabilities(), logger);
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.LOW_VULNERABILITIES, policyVulnerabilityAggregate.getLowVulnerabilities(), logger);
        }
        if (overrideValues || policyVulnerabilityAggregate.hasPolicyData()) {
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, policyVulnerabilityAggregate.getPolicySummaryStatusType(), logger);
        }
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.COMPONENT_VERSION_URL, policyVulnerabilityAggregate.getComponentVersionUrl(), logger);
        setInspectionStatus(repoPath, InspectionStatus.SUCCESS);
    }

    public void failInspection(final FailedInspectionException failedInspectionException) {
        failInspection(failedInspectionException.getRepoPath(), failedInspectionException.getMessage());
    }

    public void failInspection(final RepoPath repoPath, final String inspectionStatusMessage) {
        final int retryCount = getFailedInspectionCount(repoPath) + 1;
        if (retryCount > inspectionModuleConfig.getRetryCount()) {
            logger.debug(String.format("Attempting to fail inspection more than the number of maximum attempts: %s", repoPath.getPath()));
        } else {
            setInspectionStatus(repoPath, InspectionStatus.FAILURE, inspectionStatusMessage, retryCount);
        }
    }

    public Integer getFailedInspectionCount(final RepoPath repoPath) {
        final Optional<Integer> retryCount = artifactoryPropertyService.getPropertyAsInteger(repoPath, BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT);
        return retryCount.orElse(0);
    }

    public void setInspectionStatus(final RepoPath repoPath, final InspectionStatus status) {
        setInspectionStatus(repoPath, status, null);
    }

    public void setInspectionStatus(final RepoPath repoPath, final InspectionStatus status, final String inspectionStatusMessage) {
        setInspectionStatus(repoPath, status, inspectionStatusMessage, null);
    }

    private void setInspectionStatus(final RepoPath repoPath, final InspectionStatus status, final String inspectionStatusMessage, final Integer retryCount) {
        artifactoryPropertyService.setPropertyFromDate(repoPath, BlackDuckArtifactoryProperty.LAST_INSPECTION, new Date(), logger);
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS, status.name(), logger);

        if (StringUtils.isNotBlank(inspectionStatusMessage)) {
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS_MESSAGE, inspectionStatusMessage, logger);
        } else if (artifactoryPropertyService.hasProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS_MESSAGE)) {
            artifactoryPropertyService.deleteProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS_MESSAGE, logger);
        }

        if (retryCount != null) {
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT, retryCount.toString(), logger);
        } else {
            artifactoryPropertyService.deleteProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT, logger);
        }
    }

    public Optional<InspectionStatus> getInspectionStatus(final RepoPath repoPath) {
        final Optional<String> inspectionStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS);
        return inspectionStatus.map(InspectionStatus::valueOf);
    }

    public boolean hasInspectionStatus(final RepoPath repoPath) {
        return artifactoryPropertyService.hasProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS);
    }

    public List<RepoPath> getAllArtifactsInRepoWithInspectionStatus(final String repoKey, final InspectionStatus inspectionStatus) {
        final SetMultimap<String, String> propertyMap = HashMultimap.create();
        propertyMap.put(BlackDuckArtifactoryProperty.INSPECTION_STATUS.getName(), inspectionStatus.name());
        return artifactoryPropertyService.getAllItemsInRepoWithPropertiesAndValues(propertyMap, repoKey);
    }

    public boolean assertInspectionStatus(final RepoPath repoPath, final InspectionStatus inspectionStatus) {
        return getInspectionStatus(repoPath)
                   .filter(inspectionStatus::equals)
                   .isPresent();
    }

    public String getRepoProjectName(final String repoKey) {
        final RepoPath repoPath = RepoPathFactory.create(repoKey);
        final Optional<String> projectNameProperty = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME);

        return projectNameProperty.orElse(repoKey);
    }

    public String getRepoProjectVersionName(final String repoKey) {
        final RepoPath repoPath = RepoPathFactory.create(repoKey);
        final Optional<String> projectVersionNameProperty = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME);

        return projectVersionNameProperty.orElse(HostNameHelper.getMyHostName("UNKNOWN_HOST"));
    }

    public void setRepoProjectNameProperties(final String repoKey, final String projectName, final String projectVersionName) {
        final RepoPath repoPath = RepoPathFactory.create(repoKey);
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME, projectName, logger);
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME, projectVersionName, logger);
    }

    public void updateUIUrl(final RepoPath repoPath, final String projectName, final String projectVersion) throws IntegrationException {
        final Optional<ProjectVersionWrapper> projectVersionWrapper = projectService.getProjectVersion(projectName, projectVersion);

        if (projectVersionWrapper.isPresent()) {
            final ProjectVersionView projectVersionView = projectVersionWrapper.get().getProjectVersionView();
            final Optional<String> projectVersionUIUrl = projectVersionView.getHref();

            projectVersionUIUrl.ifPresent(uiUrl -> artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL, uiUrl, logger));
        }
    }

    public Optional<Date> getLastUpdate(final RepoPath repoKeyPath) {
        return artifactoryPropertyService.getDateFromProperty(repoKeyPath, BlackDuckArtifactoryProperty.LAST_UPDATE);
    }

    public Optional<Date> getLastInspection(final RepoPath repoKeyPath) {
        return artifactoryPropertyService.getDateFromProperty(repoKeyPath, BlackDuckArtifactoryProperty.LAST_INSPECTION);
    }

    public void setUpdateStatus(final RepoPath repoKeyPath, final UpdateStatus updateStatus) {
        artifactoryPropertyService.setProperty(repoKeyPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, updateStatus.toString(), logger);
    }

    public void setLastUpdate(final RepoPath repoKeyPath, final Date lastNotificationDate) {
        artifactoryPropertyService.setPropertyFromDate(repoKeyPath, BlackDuckArtifactoryProperty.LAST_UPDATE, lastNotificationDate, logger);
    }
}
