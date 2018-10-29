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
import com.synopsys.integration.blackduck.artifactory.BlackDuckConnectionService;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.UpdateStatus;
import com.synopsys.integration.blackduck.service.HubService;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.PolicyStatusDescription;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.NameVersion;

public class ScanPolicyService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ProjectService projectService;
    private final HubService hubService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final DateTimeManager dateTimeManager;

    public ScanPolicyService(final ProjectService projectService, final HubService hubService, final ArtifactoryPropertyService artifactoryPropertyService, final DateTimeManager dateTimeManager) {
        this.projectService = projectService;
        this.hubService = hubService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.dateTimeManager = dateTimeManager;
    }

    public static ScanPolicyService createDefault(final BlackDuckConnectionService blackDuckConnectionService, final ArtifactoryPropertyService artifactoryPropertyService, final DateTimeManager dateTimeManager) {
        final HubServicesFactory hubServicesFactory = blackDuckConnectionService.getHubServicesFactory();
        final ProjectService projectService = hubServicesFactory.createProjectService();
        final HubService hubService = hubServicesFactory.createHubService();

        return new ScanPolicyService(projectService, hubService, artifactoryPropertyService, dateTimeManager);
    }

    public void populatePolicyStatuses(final Set<RepoPath> repoPaths) {
        boolean problemRetrievingPolicyStatus = false;

        logger.info(String.format("Attempting to update policy status of %d repoPaths", repoPaths.size()));
        for (final RepoPath repoPath : repoPaths) {
            final Optional<NameVersion> nameVersion = resolveProjectNameVersion(hubService, repoPath);

            if (nameVersion.isPresent()) {
                updateProjectUIUrl(nameVersion.get().getName(), nameVersion.get().getVersion(), projectService, repoPath);
                problemRetrievingPolicyStatus = !setPolicyStatusProperties(repoPath, nameVersion.get().getName(), nameVersion.get().getVersion());
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

    private boolean setPolicyStatusProperties(final RepoPath repoPath, final String projectName, final String projectVersionName) {
        boolean success = false;

        try {
            final VersionBomPolicyStatusView versionBomPolicyStatusView = getVersionBomPolicyStatus(projectName, projectVersionName);
            if (versionBomPolicyStatusView == null) {
                throw new IntegrationException("BlackDuck failed to return a policy status");
            }

            logger.debug(String.format("Policy status json for %s is: %s", repoPath.toPath(), versionBomPolicyStatusView.json));
            final PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(versionBomPolicyStatusView);
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, policyStatusDescription.getPolicyStatusMessage());
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS, versionBomPolicyStatusView.overallStatus.toString());
            logger.info(String.format("Updated policy status of %s: %s", repoPath.getName(), repoPath.toPath()));
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, UpdateStatus.UP_TO_DATE.toString());
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.LAST_UPDATE, dateTimeManager.getStringFromDate(new Date()));
            success = true;
        } catch (final IntegrationException e) {
            logger.debug(String.format("An error occurred while attempting to update policy status on %s", repoPath.getPath()), e);
            final Optional<String> policyStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS);
            final Optional<String> overallPolicyStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS);
            if (policyStatus.isPresent() || overallPolicyStatus.isPresent()) {
                artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, UpdateStatus.OUT_OF_DATE.toString());
            }
        }

        return success;
    }

    private void updateProjectUIUrl(final String projectName, final String projectVersionName, final ProjectService projectService, final RepoPath repoPath) {
        try {
            final ProjectVersionWrapper projectVersionWrapper = projectService.getProjectVersion(projectName, projectVersionName);
            final String projectVersionUIUrl = getProjectVersionUIUrlFromView(projectVersionWrapper.getProjectVersionView());

            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL, projectVersionUIUrl);
        } catch (final IntegrationException e) {
            logger.debug(String.format("Failed to update property %s on %s", BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL.getName(), repoPath.toPath()), e);
        }
    }

    // TODO: Check if project exists before attempting to get the policy
    private VersionBomPolicyStatusView getVersionBomPolicyStatus(final String projectName, final String projectVersion) throws IntegrationException {
        final ProjectVersionWrapper projectVersionWrapper = projectService.getProjectVersion(projectName, projectVersion);

        return projectService.getPolicyStatusForVersion(projectVersionWrapper.getProjectVersionView());
    }

    private String getProjectVersionUIUrlFromView(final ProjectVersionView projectVersionView) {
        return hubService.getFirstLinkSafely(projectVersionView, "components");
    }

    // TODO: Replace instances of this with ArtifactoryPropertyService::resolveProjectNameVersion once BlackDuckArtifactoryProperty.PROJECT_VERSION_URL has been removed
    private Optional<NameVersion> resolveProjectNameVersion(final HubService hubService, final RepoPath repoPath) {
        final Optional<String> apiUrl = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_URL);
        Optional<NameVersion> nameVersion = artifactoryPropertyService.getProjectNameVersion(repoPath);

        if (!nameVersion.isPresent() && apiUrl.isPresent()) {
            try {
                final ProjectVersionView projectVersionView = hubService.getResponse(apiUrl.get(), ProjectVersionView.class);
                final ProjectView projectView = hubService.getResponse(projectVersionView, ProjectVersionView.PROJECT_LINK_RESPONSE);
                final NameVersion projectNameVersion = new NameVersion(projectView.name, projectVersionView.versionName);

                // TODO: Move to a DeprecationService class
                artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME, projectNameVersion.getName());
                artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME, projectNameVersion.getVersion());

                nameVersion = Optional.of(projectNameVersion);
            } catch (final IntegrationException e) {
                logger.error(String.format("Failed to get project name and version from url: %s", apiUrl.get()));
                logger.debug(e.getMessage(), e);
            }
        }

        return nameVersion;
    }
}
