/**
 * blackduck-artifactory-common
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.synopsys.integration.blackduck.artifactory.modules.scan;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomPolicyStatusView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.UpdateStatus;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
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

    private final ProjectService projectService;
    private final BlackDuckService blackDuckService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final DateTimeManager dateTimeManager;

    public ScanPolicyService(final ProjectService projectService, final BlackDuckService blackDuckService, final ArtifactoryPropertyService artifactoryPropertyService, final DateTimeManager dateTimeManager) {
        this.projectService = projectService;
        this.blackDuckService = blackDuckService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.dateTimeManager = dateTimeManager;
    }

    public static ScanPolicyService createDefault(final BlackDuckServerConfig blackDuckServerConfig, final ArtifactoryPropertyService artifactoryPropertyService, final DateTimeManager dateTimeManager) {
        final BlackDuckServicesFactory blackDuckServicesFactory = blackDuckServerConfig.createBlackDuckServicesFactory(logger);
        final ProjectService projectService = blackDuckServicesFactory.createProjectService();
        final BlackDuckService blackDuckService = blackDuckServicesFactory.createBlackDuckService();

        return new ScanPolicyService(projectService, blackDuckService, artifactoryPropertyService, dateTimeManager);
    }

    public void populatePolicyStatuses(final Set<RepoPath> repoPaths) {
        boolean problemRetrievingPolicyStatus = false;

        logger.info(String.format("Attempting to update policy status of %d repoPaths", repoPaths.size()));
        for (final RepoPath repoPath : repoPaths) {
            final Optional<ProjectVersionWrapper> projectVersionWrapperOptional = resolveProjectVersionWrapper(blackDuckService, repoPath);

            if (projectVersionWrapperOptional.isPresent()) {
                final ProjectVersionWrapper projectVersionWrapper = projectVersionWrapperOptional.get();
                final Optional<String> projectVersionUIUrl = projectVersionWrapper.getProjectVersionView().getHref();

                projectVersionUIUrl.ifPresent(uiUrl -> artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL, uiUrl, logger));
                problemRetrievingPolicyStatus = !setPolicyStatusProperties(repoPath, projectVersionWrapper);
            } else {
                logger.debug(
                    String.format("Properties %s and %s were not found on %s. Cannot update policy",
                        BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME.getName(),
                        BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME.getName(),
                        repoPath.getPath()
                    )
                );
            }
        }

        if (problemRetrievingPolicyStatus) {
            logger.warn("There was a problem retrieving policy status for one or more repos. This is expected if you do not have policy management.");
        }
    }

    private boolean setPolicyStatusProperties(final RepoPath repoPath, final ProjectVersionWrapper projectVersionWrapper) {
        final String projectName = projectVersionWrapper.getProjectView().getName();
        final String projectVersionName = projectVersionWrapper.getProjectVersionView().getVersionName();
        boolean success = false;

        try {
            final Optional<VersionBomPolicyStatusView> versionBomPolicyStatusViewOptional = projectService.getPolicyStatusForVersion(projectVersionWrapper.getProjectVersionView());
            if (!versionBomPolicyStatusViewOptional.isPresent()) {
                throw new IntegrationException(String.format("BlackDuck failed to return a policy status. Project '%s' with version '%s' may not exist in BlackDuck", projectName, projectVersionName));
            }

            final VersionBomPolicyStatusView versionBomPolicyStatusView = versionBomPolicyStatusViewOptional.get();
            logger.debug(String.format("Policy status json for %s is: %s", repoPath.toPath(), versionBomPolicyStatusView.getJson()));
            final PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(versionBomPolicyStatusView);
            final String patchedPolicyStatusMessage = policyStatusDescription.getPolicyStatusMessage();
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, patchedPolicyStatusMessage, logger);
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS, versionBomPolicyStatusView.getOverallStatus().toString(), logger);
            logger.info(String.format("Updated policy status of %s: %s", repoPath.getName(), repoPath.toPath()));
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, UpdateStatus.UP_TO_DATE.toString(), logger);
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.LAST_UPDATE, dateTimeManager.getStringFromDate(new Date()), logger);
            success = true;
        } catch (final IntegrationRestException e) {
            logger.warn(String.format("Update policy status failed with status code (%d)", e.getHttpStatusCode()));
            failPolicyStatusUpdate(repoPath, e);
        } catch (final IntegrationException e) {
            logger.warn(String.format("Update policy status failed. Project '%s' with version '%s' may not exist in BlackDuck", projectName, projectVersionName));
            failPolicyStatusUpdate(repoPath, e);
        }

        return success;
    }

    private void failPolicyStatusUpdate(final RepoPath repoPath, final Exception e) {
        logger.debug(String.format("An error occurred while attempting to update policy status on %s", repoPath.getPath()), e);
        final Optional<String> policyStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, logger);
        final Optional<String> overallPolicyStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS, logger);
        if (policyStatus.isPresent() || overallPolicyStatus.isPresent()) {
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, UpdateStatus.OUT_OF_DATE.toString(), logger);
        }
    }

    // TODO: Replace instances of this with ArtifactoryPropertyService::resolveProjectVersionWrapper once BlackDuckArtifactoryProperty.PROJECT_VERSION_URL has been removed
    private Optional<ProjectVersionWrapper> resolveProjectVersionWrapper(final BlackDuckService hubService, final RepoPath repoPath) {
        final Optional<String> apiUrl = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_URL, logger);
        final Optional<NameVersion> nameVersion = artifactoryPropertyService.getProjectNameVersion(repoPath, logger);

        Optional<ProjectVersionWrapper> projectVersionViewWrapper = Optional.empty();
        if (nameVersion.isPresent()) {
            final String projectName = nameVersion.get().getName();
            final String projectVersionName = nameVersion.get().getVersion();
            try {
                projectVersionViewWrapper = projectService.getProjectVersion(projectName, projectVersionName);
            } catch (final IntegrationException e) {
                logger.error(String.format("Failed to find Black Duck project version with name '%s' and version '%s'", projectName, projectVersionName));
                logger.debug(e.getMessage(), e);
            }
        } else if (apiUrl.isPresent()) {
            try {
                final ProjectVersionView projectVersionView = hubService.getResponse(apiUrl.get(), ProjectVersionView.class);
                final Optional<ProjectView> projectViewOptional = hubService.getResponse(projectVersionView, ProjectVersionView.PROJECT_LINK_RESPONSE);

                if (projectViewOptional.isPresent()) {
                    artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME, projectViewOptional.get().getName(), logger);
                    artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME, projectVersionView.getVersionName(), logger);

                    projectVersionViewWrapper = Optional.of(new ProjectVersionWrapper(projectViewOptional.get(), projectVersionView));
                } else {
                    throw new IntegrationException("Failed to get ProjectView from ProjectVersionView");
                }
            } catch (final IntegrationException e) {
                logger.error(String.format("Failed to find Black Duck project version from url: %s", apiUrl.get()));
                logger.debug(e.getMessage(), e);
            }
        }

        return projectVersionViewWrapper;
    }
}
