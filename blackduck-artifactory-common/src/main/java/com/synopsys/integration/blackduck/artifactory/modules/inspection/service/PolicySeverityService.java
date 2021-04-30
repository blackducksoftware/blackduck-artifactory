/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionComponentPolicyStatusType;
import com.synopsys.integration.blackduck.api.generated.view.ComponentPolicyRulesView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionComponentView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.ComponentViewWrapper;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.blackduck.service.dataservice.ProjectService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.rest.HttpUrl;

public class PolicySeverityService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final InspectionPropertyService inspectionPropertyService;
    private final BlackDuckApiClient blackDuckApiClient;
    private final BlackDuckBOMService blackDuckBOMService;
    private final ProjectService projectService;

    public PolicySeverityService(ArtifactoryPropertyService artifactoryPropertyService, InspectionPropertyService inspectionPropertyService, BlackDuckApiClient blackDuckApiClient, BlackDuckBOMService blackDuckBOMService,
        ProjectService projectService) {
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.inspectionPropertyService = inspectionPropertyService;
        this.blackDuckApiClient = blackDuckApiClient;
        this.blackDuckBOMService = blackDuckBOMService;
        this.projectService = projectService;
    }

    public void performPolicySeverityUpgrade(String repoKey) {
        String projectName = inspectionPropertyService.getRepoProjectName(repoKey);
        String projectVersionName = inspectionPropertyService.getRepoProjectVersionName(repoKey);
        try {
            Optional<ProjectVersionWrapper> projectVersionWrapper = projectService.getProjectVersion(projectName, projectVersionName);
            if (projectVersionWrapper.isPresent()) {
                SetMultimap<String, String> inViolationPropertyMap = new ImmutableSetMultimap.Builder<String, String>()
                                                                         .put(BlackDuckArtifactoryProperty.INSPECTION_STATUS.getPropertyName(), InspectionStatus.SUCCESS.name())
                                                                         .put(BlackDuckArtifactoryProperty.POLICY_STATUS.getPropertyName(), ProjectVersionComponentPolicyStatusType.IN_VIOLATION.name())
                                                                         .build();
                SetMultimap<String, String> overriddenPropertyMap = new ImmutableSetMultimap.Builder<String, String>()
                                                                        .put(BlackDuckArtifactoryProperty.INSPECTION_STATUS.getPropertyName(), InspectionStatus.SUCCESS.name())
                                                                        .put(BlackDuckArtifactoryProperty.POLICY_STATUS.getPropertyName(), ProjectVersionComponentPolicyStatusType.IN_VIOLATION_OVERRIDDEN.name())
                                                                        .build();
                List<RepoPath> repoPathsFound = new ArrayList<>();
                repoPathsFound.addAll(artifactoryPropertyService.getItemsContainingPropertiesAndValues(inViolationPropertyMap, repoKey));
                repoPathsFound.addAll(artifactoryPropertyService.getItemsContainingPropertiesAndValues(overriddenPropertyMap, repoKey));

                repoPathsFound.forEach(repoPath -> upgradeSeverityForRepoPath(projectVersionWrapper.get().getProjectVersionView(), repoPath));
                logger.info(String.format("Attempted to update %d artifacts with outdated policy status.", repoPathsFound.size()));
            } else {
                logger.warn(String.format("Repo '%s' does not exist in Black Duck. Assuming initialization has not been run. Policy Severity Upgrade not applied.", repoKey));
            }
        } catch (IntegrationException e) {
            logger.error(String.format("Failed to perform the policy severity upgrade for repo '%s'. Blocking downloads by policy may not work as expected.", repoKey), e);
        }
    }

    private void upgradeSeverityForRepoPath(ProjectVersionView projectVersionView, RepoPath repoPath) {
        try {
            String componentVersionUrl = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.COMPONENT_VERSION_URL)
                                             .orElseThrow(() -> new IntegrationException("Missing component version url."));
            ComponentViewWrapper componentViewWrapper = blackDuckBOMService.getComponentViewWrapper(new HttpUrl(componentVersionUrl), projectVersionView);
            ProjectVersionComponentView projectVersionComponentView = componentViewWrapper.getProjectVersionComponentView();
            List<ComponentPolicyRulesView> versionBomPolicyRuleViews;
            versionBomPolicyRuleViews = blackDuckApiClient.getAllResponses(projectVersionComponentView, ProjectVersionComponentView.POLICY_RULES_LINK_RESPONSE);

            ProjectVersionComponentPolicyStatusType policyStatus = projectVersionComponentView.getPolicyStatus();
            List<PolicyRuleSeverityType> policySeverityTypes = versionBomPolicyRuleViews.stream()
                                                                   .map(ComponentPolicyRulesView::getSeverity)
                                                                   .collect(Collectors.toList());
            PolicyStatusReport policyStatusReport = new PolicyStatusReport(policyStatus, policySeverityTypes);
            inspectionPropertyService.setPolicyProperties(repoPath, policyStatusReport);
        } catch (IntegrationException e) {
            logger.error(String.format("Failed to perform policy upgrade on %s. %s", repoPath.toPath(), e.getMessage()));
        }
    }
}
