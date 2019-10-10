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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.synopsys.integration.blackduck.api.enumeration.PolicySeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomPolicyRuleView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.ComponentViewWrapper;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;
import com.synopsys.integration.blackduck.artifactory.modules.policy.PolicyModule;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class PolicySeverityService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final InspectionPropertyService inspectionPropertyService;
    private final BlackDuckService blackDuckService;
    private final BlackDuckBOMService blackDuckBOMService;
    private final ProjectService projectService;

    public PolicySeverityService(final ArtifactoryPAPIService artifactoryPAPIService, final ArtifactoryPropertyService artifactoryPropertyService, final InspectionPropertyService inspectionPropertyService,
        final BlackDuckService blackDuckService, final BlackDuckBOMService blackDuckBOMService, final ProjectService projectService) {
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.inspectionPropertyService = inspectionPropertyService;
        this.blackDuckService = blackDuckService;
        this.blackDuckBOMService = blackDuckBOMService;
        this.projectService = projectService;
    }

    public void performPolicySeverityUpgrade(final String repoKey) {
        final String projectName = inspectionPropertyService.getRepoProjectName(repoKey);
        final String projectVersionName = inspectionPropertyService.getRepoProjectVersionName(repoKey);
        try {
            final Optional<ProjectVersionWrapper> projectVersionWrapper = projectService.getProjectVersion(projectName, projectVersionName);
            if (projectVersionWrapper.isPresent()) {
                final SetMultimap<String, String> inViolationPropertyMap = new ImmutableSetMultimap.Builder<String, String>()
                                                                               .put(BlackDuckArtifactoryProperty.INSPECTION_STATUS.getName(), InspectionStatus.SUCCESS.name())
                                                                               .put(BlackDuckArtifactoryProperty.POLICY_STATUS.getName(), PolicySummaryStatusType.IN_VIOLATION.name())
                                                                               .build();
                final SetMultimap<String, String> overriddenPropertyMap = new ImmutableSetMultimap.Builder<String, String>()
                                                                              .put(BlackDuckArtifactoryProperty.INSPECTION_STATUS.getName(), InspectionStatus.SUCCESS.name())
                                                                              .put(BlackDuckArtifactoryProperty.POLICY_STATUS.getName(), PolicySummaryStatusType.IN_VIOLATION_OVERRIDDEN.name())
                                                                              .build();
                final List<RepoPath> repoPathsFound = new ArrayList<>();
                repoPathsFound.addAll(artifactoryPAPIService.itemsByProperties(inViolationPropertyMap, repoKey));
                repoPathsFound.addAll(artifactoryPAPIService.itemsByProperties(overriddenPropertyMap, repoKey));

                repoPathsFound.stream()
                    .filter(repoPath -> !artifactoryPropertyService.hasProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES))
                    .forEach(repoPath -> upgradeSeverityForRepoPath(projectVersionWrapper.get().getProjectVersionView(), repoPath));

            } else {
                logger.warn(String.format("Repo '%s' does not exist in Black Duck. Assuming initialization has not been run. Policy Severity Upgrade not applied.", repoKey));
            }
        } catch (final IntegrationException e) {
            logger.error(String.format("Failed to perform the policy severity upgrade for repo '%s'. The %s may not work as expected.", repoKey, PolicyModule.class.getSimpleName()), e);
        }
    }

    private void upgradeSeverityForRepoPath(final ProjectVersionView projectVersionView, final RepoPath repoPath) {
        try {
            final String componentVersionUrl = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.COMPONENT_VERSION_URL)
                                                   .orElseThrow(() -> new IntegrationException("Missing component version url."));
            final ComponentViewWrapper componentViewWrapper = blackDuckBOMService.getComponentViewWrapper(componentVersionUrl, projectVersionView);
            final VersionBomComponentView versionBomComponentView = componentViewWrapper.getVersionBomComponentView();
            final List<VersionBomPolicyRuleView> versionBomPolicyRuleViews;
            versionBomPolicyRuleViews = blackDuckService.getResponses(versionBomComponentView, VersionBomComponentView.POLICY_RULES_LINK_RESPONSE, true);

            final PolicySummaryStatusType policyStatus = versionBomComponentView.getPolicyStatus();
            final List<PolicySeverityType> policySeverityTypes = versionBomPolicyRuleViews.stream()
                                                                     .map(VersionBomPolicyRuleView::getSeverity)
                                                                     .map(PolicySeverityType::valueOf)
                                                                     .collect(Collectors.toList());
            final PolicyStatusReport policyStatusReport = new PolicyStatusReport(policyStatus, policySeverityTypes);
            inspectionPropertyService.setPolicyProperties(repoPath, policyStatusReport);
        } catch (final IntegrationException e) {
            logger.error(String.format("Failed to perform policy upgrade on %s. %s", repoPath.toPath(), e.getMessage()));
        }
    }
}
