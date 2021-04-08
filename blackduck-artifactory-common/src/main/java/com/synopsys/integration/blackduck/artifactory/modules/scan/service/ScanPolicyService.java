/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scan.service;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionPolicyStatusView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.UpdateStatus;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.dataservice.ProjectBomService;
import com.synopsys.integration.blackduck.service.dataservice.ProjectService;
import com.synopsys.integration.blackduck.service.model.PolicyStatusDescription;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.rest.HttpUrl;
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
                Optional<HttpUrl> componentsLink = projectVersionWrapper.getProjectVersionView().getFirstLinkSafely(ProjectVersionView.COMPONENTS_LINK);
                componentsLink.ifPresent(uiUrl -> artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL, uiUrl.string(), logger));
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
            Optional<ProjectVersionPolicyStatusView> projectVersionPolicyStatusViewOptional = projectBomService.getPolicyStatusForVersion(projectVersionWrapper.getProjectVersionView());
            if (!projectVersionPolicyStatusViewOptional.isPresent()) {
                throw new IntegrationException(String.format("BlackDuck failed to return a policy status. Project '%s' with version '%s' may not exist in BlackDuck", projectName, projectVersionName));
            }

            ProjectVersionPolicyStatusView projectVersionPolicyStatusView = projectVersionPolicyStatusViewOptional.get();
            logger.debug(String.format("Policy status json for %s is: %s", repoPath.toPath(), projectVersionPolicyStatusView.getJson()));
            PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(projectVersionPolicyStatusView);
            String patchedPolicyStatusMessage = policyStatusDescription.getPolicyStatusMessage();
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, patchedPolicyStatusMessage, logger);
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS, projectVersionPolicyStatusView.getOverallStatus().toString(), logger);
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
