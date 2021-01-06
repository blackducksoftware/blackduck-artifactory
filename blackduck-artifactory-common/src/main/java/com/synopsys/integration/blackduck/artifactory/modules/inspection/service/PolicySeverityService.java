/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyStatusType;
import com.synopsys.integration.blackduck.api.generated.view.ComponentPolicyRulesView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionComponentView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.ComponentViewWrapper;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;
import com.synopsys.integration.blackduck.artifactory.modules.policy.PolicyModule;
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
                                                                         .put(BlackDuckArtifactoryProperty.POLICY_STATUS.getPropertyName(), PolicyStatusType.IN_VIOLATION.name())
                                                                         .build();
                SetMultimap<String, String> overriddenPropertyMap = new ImmutableSetMultimap.Builder<String, String>()
                                                                        .put(BlackDuckArtifactoryProperty.INSPECTION_STATUS.getPropertyName(), InspectionStatus.SUCCESS.name())
                                                                        .put(BlackDuckArtifactoryProperty.POLICY_STATUS.getPropertyName(), PolicyStatusType.IN_VIOLATION_OVERRIDDEN.name())
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
            logger.error(String.format("Failed to perform the policy severity upgrade for repo '%s'. The %s may not work as expected.", repoKey, PolicyModule.class.getSimpleName()), e);
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

            PolicyStatusType policyStatus = projectVersionComponentView.getPolicyStatus();
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
