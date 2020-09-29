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
package com.synopsys.integration.blackduck.artifactory.modules.scan.service;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.view.VersionBomPolicyStatusView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.UpdateStatus;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectBomService;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.PolicyStatusDescription;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.rest.exception.IntegrationRestException;
import com.synopsys.integration.util.NameVersion;

public class ScanPolicyService {
    private static final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(ScanPolicyService.class));

    private final ProjectBomService projectBomService;
    private final ProjectService projectService;
    private final ArtifactoryPropertyService artifactoryPropertyService;

    public ScanPolicyService(ProjectBomService projectBomService, ProjectService projectService, ArtifactoryPropertyService artifactoryPropertyService) {
        this.projectBomService = projectBomService;
        this.projectService = projectService;
        this.artifactoryPropertyService = artifactoryPropertyService;
    }

    public static ScanPolicyService createDefault(BlackDuckServerConfig blackDuckServerConfig, ArtifactoryPropertyService artifactoryPropertyService) {
        BlackDuckServicesFactory blackDuckServicesFactory = blackDuckServerConfig.createBlackDuckServicesFactory(logger);
        ProjectBomService projectBomService = blackDuckServicesFactory.createProjectBomService();
        ProjectService projectService = blackDuckServicesFactory.createProjectService();

        return new ScanPolicyService(projectBomService, projectService, artifactoryPropertyService);
    }

    public void populatePolicyStatuses(Set<RepoPath> repoPaths) {
        boolean problemRetrievingPolicyStatus = false;

        logger.info(String.format("Attempting to update policy status of %d repoPaths", repoPaths.size()));
        for (RepoPath repoPath : repoPaths) {
            try {
                ProjectVersionWrapper projectVersionWrapper = resolveProjectVersionWrapper(repoPath);
                Optional<String> projectVersionUIUrl = projectVersionWrapper.getProjectVersionView().getHref();
                projectVersionUIUrl.ifPresent(uiUrl -> artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL, uiUrl, logger));
                problemRetrievingPolicyStatus = !setPolicyStatusProperties(repoPath, projectVersionWrapper);
            } catch (IntegrationException e) {
                Exception exception = new IntegrationException(String.format("Failed to get project version for artifact. Scan may not be finished. Cannot update policy: %s", repoPath.toPath()));
                failPolicyStatusUpdate(repoPath, exception);
                problemRetrievingPolicyStatus = true;
            }
        }

        if (problemRetrievingPolicyStatus) {
            logger.warn("There was a problem retrieving policy status for one or more artifacts. This is expected if you do not have policy management.");
        }
    }

    private boolean setPolicyStatusProperties(RepoPath repoPath, ProjectVersionWrapper projectVersionWrapper) {
        String projectName = projectVersionWrapper.getProjectView().getName();
        String projectVersionName = projectVersionWrapper.getProjectVersionView().getVersionName();
        boolean success = false;

        try {
            Optional<VersionBomPolicyStatusView> versionBomPolicyStatusViewOptional = projectBomService.getPolicyStatusForVersion(projectVersionWrapper.getProjectVersionView());
            if (!versionBomPolicyStatusViewOptional.isPresent()) {
                throw new IntegrationException(String.format("BlackDuck failed to return a policy status. Project '%s' with version '%s' may not exist in BlackDuck", projectName, projectVersionName));
            }

            VersionBomPolicyStatusView versionBomPolicyStatusView = versionBomPolicyStatusViewOptional.get();
            logger.debug(String.format("Policy status json for %s is: %s", repoPath.toPath(), versionBomPolicyStatusView.getJson()));
            PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(versionBomPolicyStatusView);
            String patchedPolicyStatusMessage = policyStatusDescription.getPolicyStatusMessage();
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, patchedPolicyStatusMessage, logger);
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS, versionBomPolicyStatusView.getOverallStatus().toString(), logger);
            logger.info(String.format("Updated policy status of %s: %s", repoPath.getName(), repoPath.toPath()));
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, UpdateStatus.UP_TO_DATE.toString(), logger);
            artifactoryPropertyService.setPropertyFromDate(repoPath, BlackDuckArtifactoryProperty.LAST_UPDATE, new Date(), logger);
            success = true;
        } catch (IntegrationRestException e) {
            logger.warn(String.format("Update policy status failed with status code (%d)", e.getHttpStatusCode()));
            failPolicyStatusUpdate(repoPath, e);
        } catch (IntegrationException e) {
            logger.warn(String.format("Update policy status failed. Project '%s' with version '%s' may not exist in BlackDuck", projectName, projectVersionName));
            failPolicyStatusUpdate(repoPath, e);
        }

        return success;
    }

    private void failPolicyStatusUpdate(RepoPath repoPath, Exception e) {
        logger.debug(String.format("An error occurred while attempting to update policy status on %s", repoPath.getPath()), e);
        Optional<String> policyStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS);
        Optional<String> overallPolicyStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS);
        if (policyStatus.isPresent() || overallPolicyStatus.isPresent()) {
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, UpdateStatus.OUT_OF_DATE.toString(), logger);
        }
    }

    private ProjectVersionWrapper resolveProjectVersionWrapper(RepoPath repoPath) throws IntegrationException {
        Optional<NameVersion> nameVersion = artifactoryPropertyService.getProjectNameVersion(repoPath);

        if (nameVersion.isPresent()) {
            String projectName = nameVersion.get().getName();
            String projectVersionName = nameVersion.get().getVersion();
            return projectService.getProjectVersion(projectName, projectVersionName)
                       .orElseThrow(() -> new IntegrationException(String.format("Failed to find Black Duck project version with name '%s' and version '%s'.", projectName, projectVersionName)));
        } else {
            throw new IntegrationException("Failed to extract project name and project version from properties.");
        }
    }
}
