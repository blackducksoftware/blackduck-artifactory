/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.service;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.exception.FailedInspectionException;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.ExternalIdService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.Artifact;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.ComponentViewWrapper;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.deleteme.ComponentVersionIdUtil;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.util.ArtifactoryComponentService;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.blackduck.service.dataservice.ComponentService;
import com.synopsys.integration.blackduck.service.dataservice.ProjectService;
import com.synopsys.integration.blackduck.service.model.ComponentVersionVulnerabilities;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ArtifactInspectionService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final BlackDuckBOMService blackDuckBOMService;
    private final InspectionModuleConfig inspectionModuleConfig;
    private final InspectionPropertyService inspectionPropertyService;
    private final ProjectService projectService;
    private final ComponentService componentService;
    private final ExternalIdService externalIdService;
    private final BlackDuckApiClient blackDuckApiClient;
    private final ArtifactoryComponentService artifactoryComponentService; // TODO: Remove in favor of componentService once blackduck-common uses correct mime type.

    public ArtifactInspectionService(ArtifactoryPAPIService artifactoryPAPIService, BlackDuckBOMService blackDuckBOMService, InspectionModuleConfig inspectionModuleConfig,
        InspectionPropertyService inspectionPropertyService, ProjectService projectService, ComponentService componentService, ExternalIdService externalIdService,
        BlackDuckApiClient blackDuckApiClient, ArtifactoryComponentService artifactoryComponentService) {
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.blackDuckBOMService = blackDuckBOMService;
        this.inspectionModuleConfig = inspectionModuleConfig;
        this.inspectionPropertyService = inspectionPropertyService;
        this.projectService = projectService;
        this.componentService = componentService;
        this.externalIdService = externalIdService;
        this.blackDuckApiClient = blackDuckApiClient;
        this.artifactoryComponentService = artifactoryComponentService;
    }

    public boolean shouldInspectArtifact(RepoPath repoPath) {
        if (!inspectionModuleConfig.getRepos().contains(repoPath.getRepoKey())) {
            return false;
        }

        ItemInfo itemInfo = artifactoryPAPIService.getItemInfo(repoPath);
        Optional<List<String>> patterns = artifactoryPAPIService.getPackageType(repoPath.getRepoKey())
                                              .map(inspectionModuleConfig::getPatternsForPackageType);

        if (!patterns.isPresent() || patterns.get().isEmpty() || itemInfo.isFolder()) {
            return false;
        }

        File artifact = new File(itemInfo.getName());
        WildcardFileFilter wildcardFileFilter = new WildcardFileFilter(patterns.get());

        return wildcardFileFilter.accept(artifact);
    }

    private Artifact identifyArtifact(RepoPath repoPath) {
        ExternalId externalId = externalIdService.extractExternalId(repoPath).orElse(null);
        return new Artifact(repoPath, externalId);
    }

    public void inspectSingleArtifact(RepoPath repoPath) {
        try {
            String repoKey = repoPath.getRepoKey();
            ProjectVersionWrapper projectVersionWrapper = fetchProjectVersionWrapper(repoKey);

            inspectSingleArtifact(repoPath, projectVersionWrapper.getProjectVersionView());
        } catch (IntegrationException e) {
            inspectionPropertyService.failInspection(repoPath, e.getMessage());
        }
    }

    public void inspectSingleArtifact(RepoPath repoPath, ProjectVersionView projectVersionView) throws IntegrationException {
        Artifact artifact = identifyArtifact(repoPath);
        ComponentViewWrapper componentViewWrapper = blackDuckBOMService.addArtifactToProjectVersion(artifact, projectVersionView);
        populateBlackDuckMetadata(artifact.getRepoPath(), componentViewWrapper);
    }

    public ProjectVersionWrapper fetchProjectVersionWrapper(String repoKey) throws IntegrationException {
        String repoProjectName = inspectionPropertyService.getRepoProjectName(repoKey);
        String repoProjectVersionName = inspectionPropertyService.getRepoProjectVersionName(repoKey);
        return projectService.getProjectVersion(repoProjectName, repoProjectVersionName)
                   .orElseThrow(() -> new IntegrationException(String.format("Project '%s' and version '%s' could not be found in Black Duck.", repoProjectName, repoProjectVersionName)));
    }

    public void inspectAllUnknownArtifacts(String repoKey) {
        RepoPath repoKeyPath = RepoPathFactory.create(repoKey);
        try {
            inspectAllUnknownArtifacts(repoKeyPath);
        } catch (IntegrationException e) {
            logger.error(String.format("An error occurred when inspecting '%s'.", repoKey));
            inspectionPropertyService.setInspectionStatus(repoKeyPath, InspectionStatus.PENDING, e.getMessage());
        }
    }

    private void inspectAllUnknownArtifacts(RepoPath repoKeyPath) throws FailedInspectionException {
        String repoKey = repoKeyPath.getRepoKey();

        if (!inspectionPropertyService.hasInspectionStatus(repoKeyPath) || inspectionPropertyService.assertInspectionStatus(repoKeyPath, InspectionStatus.FAILURE)) {
            // Only inspect a delta if the repository has been successfully initialized.
            return;
        }

        Optional<String> packageType = artifactoryPAPIService.getPackageType(repoKey);
        if (!packageType.isPresent()) {
            String message = String.format("The repository '%s' has no package type. Inspection cannot be performed.", repoKey);
            logger.error(message);
            throw new FailedInspectionException(repoKeyPath, message);
        }

        List<String> patterns = inspectionModuleConfig.getPatternsForPackageType(packageType.get());
        if (patterns.isEmpty()) {
            // If we don't verify that patterns is not empty, artifactory will grab every artifact in the repo.
            logger.warn(String.format("The repository '%s' has a package type of '%s' which either isn't supported or has no patterns configured for it.", repoKey, packageType.get()));
            throw new FailedInspectionException(repoKeyPath, "Repo has an unsupported package type or not patterns configured for it.");
        }

        ProjectVersionView projectVersionView;
        try {
            projectVersionView = fetchProjectVersionWrapper(repoKey).getProjectVersionView();
            inspectionPropertyService.updateProjectUIUrl(repoKeyPath, projectVersionView);
        } catch (IntegrationException e) {
            logger.debug("An error occurred when attempting to get the project version from Black Duck.", e);
            throw new FailedInspectionException(repoKeyPath, String.format("Failed to get project version from Black Duck: %s", e.getMessage()));
        }

        List<Artifact> artifacts = artifactoryPAPIService.searchForArtifactsByPatterns(repoKey, patterns).stream()
                                       .filter(this::shouldPerformDeltaAnalysis)
                                       .map(this::identifyArtifact)
                                       .collect(Collectors.toList());

        for (Artifact artifact : artifacts) {
            try {
                ComponentViewWrapper componentViewWrapper = blackDuckBOMService.addArtifactToProjectVersion(artifact, projectVersionView);
                populateBlackDuckMetadata(artifact.getRepoPath(), componentViewWrapper);
            } catch (IntegrationException e) {
                inspectionPropertyService.failInspection(artifact.getRepoPath(), String.format("Failed to find component: %s", e.getMessage()));
            }
        }
    }

    private void populateBlackDuckMetadata(RepoPath repoPath, ComponentViewWrapper componentViewWrapper) throws IntegrationException {
        ComponentVersionView componentVersionView = componentViewWrapper.getComponentVersionView();
        ComponentVersionVulnerabilities componentVersionVulnerabilities = artifactoryComponentService.getComponentVersionVulnerabilities(componentVersionView);
        VulnerabilityAggregate vulnerabilityAggregate = VulnerabilityAggregate.fromVulnerabilityViews(componentVersionVulnerabilities.getVulnerabilities());
        PolicyStatusReport policyStatusReport = PolicyStatusReport.fromVersionBomComponentView(componentViewWrapper.getProjectVersionComponentView(), blackDuckApiClient);

        inspectionPropertyService.setPolicyProperties(repoPath, policyStatusReport);
        inspectionPropertyService.setVulnerabilityProperties(repoPath, vulnerabilityAggregate);
        inspectionPropertyService.setComponentVersionUrl(repoPath, componentViewWrapper.getProjectVersionComponentView().getComponentVersion());
        inspectionPropertyService.setInspectionStatus(repoPath, InspectionStatus.SUCCESS);
        inspectionPropertyService.deleteProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_FORGE, logger);
        inspectionPropertyService.deleteProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID, logger);
        inspectionPropertyService.deleteProperty(repoPath, BlackDuckArtifactoryProperty.COMPONENT_NAME_VERSION, logger);

        String componentVersionUrl = componentViewWrapper.getProjectVersionComponentView().getComponentVersion();
        String componentVersionId = ComponentVersionIdUtil.extractComponentVersionId(componentVersionUrl);
        inspectionPropertyService.setExternalIdProperties(repoPath, componentVersionId);
    }

    private boolean shouldPerformDeltaAnalysis(RepoPath repoPath) {
        return (!inspectionPropertyService.hasInspectionStatus(repoPath) && !inspectionPropertyService.hasExternalIdProperties(repoPath))
                   || inspectionPropertyService.shouldRetryInspection(repoPath)
                   || inspectionPropertyService.assertInspectionStatus(repoPath, InspectionStatus.PENDING);
    }
}
