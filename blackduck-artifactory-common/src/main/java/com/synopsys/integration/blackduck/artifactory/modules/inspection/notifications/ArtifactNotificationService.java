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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.api.manual.view.NotificationUserView;
import com.synopsys.integration.blackduck.api.manual.view.VulnerabilityNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.ArtifactSearchService;
import com.synopsys.integration.blackduck.artifactory.modules.UpdateStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.PolicyAffectedArtifact;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.PolicyNotifications;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAffectedArtifact;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.PolicyOverrideProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.PolicyRuleClearedProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.PolicyViolationProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.ProcessedPolicyNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.ProcessedVulnerabilityNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.VulnerabilityProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.exception.IntegrationException;

public class ArtifactNotificationService {
    private final ArtifactSearchService artifactSearchService;
    private final InspectionPropertyService inspectionPropertyService;
    private final PolicyNotificationService policyNotificationService;
    private final VulnerabilityNotificationService vulnerabilityNotificationService;
    private final PolicyOverrideProcessor policyOverrideProcessor;
    private final PolicyRuleClearedProcessor policyRuleClearedProcessor;
    private final PolicyViolationProcessor policyViolationProcessor;
    private final VulnerabilityProcessor vulnerabilityProcessor;

    public ArtifactNotificationService(ArtifactSearchService artifactSearchService, InspectionPropertyService inspectionPropertyService, PolicyNotificationService policyNotificationService,
        VulnerabilityNotificationService vulnerabilityNotificationService, PolicyOverrideProcessor policyOverrideProcessor, PolicyRuleClearedProcessor policyRuleClearedProcessor, PolicyViolationProcessor policyViolationProcessor,
        VulnerabilityProcessor vulnerabilityProcessor) {
        this.artifactSearchService = artifactSearchService;
        this.inspectionPropertyService = inspectionPropertyService;
        this.policyNotificationService = policyNotificationService;
        this.vulnerabilityNotificationService = vulnerabilityNotificationService;
        this.policyOverrideProcessor = policyOverrideProcessor;
        this.policyRuleClearedProcessor = policyRuleClearedProcessor;
        this.policyViolationProcessor = policyViolationProcessor;
        this.vulnerabilityProcessor = vulnerabilityProcessor;
    }

    public void updateMetadataFromNotifications(List<RepoPath> repoKeyPaths, Date startDate, Date endDate) throws IntegrationException {
        PolicyNotifications policyNotifications = policyNotificationService.fetchPolicyNotifications(startDate, endDate);
        RepositoryProjectNameLookup repositoryProjectNameLookup = RepositoryProjectNameLookup.fromProperties(inspectionPropertyService, repoKeyPaths);

        List<PolicyAffectedArtifact> policyAffectedArtifacts = new ArrayList<>();

        List<ProcessedPolicyNotification> processedOverrideNotifications = policyOverrideProcessor.processPolicyOverrideNotifications(policyNotifications.getPolicyOverrideNotificationUserViews(), repositoryProjectNameLookup);
        policyAffectedArtifacts.addAll(findPolicyAffectedArtifacts(processedOverrideNotifications));

        List<ProcessedPolicyNotification> processedRuleClearedNotifications = policyRuleClearedProcessor.processPolicyRuleClearedNotifications(policyNotifications.getRuleViolationClearedNotificationUserViews(), repositoryProjectNameLookup);
        policyAffectedArtifacts.addAll(findPolicyAffectedArtifacts(processedRuleClearedNotifications));

        List<ProcessedPolicyNotification> processedViolationNotifications = policyViolationProcessor.processPolicyViolationNotifications(policyNotifications.getRuleViolationNotificationUserViews(), repositoryProjectNameLookup);
        policyAffectedArtifacts.addAll(findPolicyAffectedArtifacts(processedViolationNotifications));

        for (PolicyAffectedArtifact affectedArtifact : policyAffectedArtifacts) {
            PolicyStatusReport policyStatusReport = affectedArtifact.getPolicyStatusReport();
            for (RepoPath repoPath : affectedArtifact.getAffectedArtifacts()) {
                inspectionPropertyService.setPolicyProperties(repoPath, policyStatusReport);
            }
        }

        List<VulnerabilityNotificationUserView> vulnerabilityNotificationUserViews = vulnerabilityNotificationService.fetchVulnerabilityNotifications(startDate, endDate);
        List<ProcessedVulnerabilityNotification> processedVulnerabilityNotifications = vulnerabilityProcessor.processVulnerabilityNotifications(vulnerabilityNotificationUserViews, repositoryProjectNameLookup);
        List<VulnerabilityAffectedArtifact> vulnerabilityAffectedArtifacts = findVulnerabilityAffectedArtifacts(processedVulnerabilityNotifications);

        for (VulnerabilityAffectedArtifact affectedArtifact : vulnerabilityAffectedArtifacts) {
            VulnerabilityAggregate vulnerabilityAggregate = affectedArtifact.getVulnerabilityAggregate();
            for (RepoPath repoPath : affectedArtifact.getAffectedArtifacts()) {
                inspectionPropertyService.setVulnerabilityProperties(repoPath, vulnerabilityAggregate);
            }
        }

        List<NotificationUserView> allNotifications = new ArrayList<>();
        allNotifications.addAll(policyNotifications.getAllNotificationUserViews());
        allNotifications.addAll(vulnerabilityNotificationUserViews);
        Optional<Date> lastNotificationDate = getLatestNotificationCreatedAtDate(allNotifications);
        repoKeyPaths.forEach(repoKeyPath -> {
            inspectionPropertyService.setUpdateStatus(repoKeyPath, UpdateStatus.UP_TO_DATE);
            inspectionPropertyService.setInspectionStatus(repoKeyPath, InspectionStatus.SUCCESS);
            // We don't want to miss notifications, so if something goes wrong we will err on the side of caution.
            inspectionPropertyService.setLastUpdate(repoKeyPath, lastNotificationDate.orElse(startDate));
        });
    }

    private List<PolicyAffectedArtifact> findPolicyAffectedArtifacts(List<ProcessedPolicyNotification> processedPolicyNotifications) {
        List<PolicyAffectedArtifact> affectedArtifacts = new ArrayList<>();
        for (ProcessedPolicyNotification processedPolicyNotification : processedPolicyNotifications) {
            String componentName = processedPolicyNotification.getComponentName();
            String componentVersionName = processedPolicyNotification.getComponentVersionName();
            PolicyStatusReport policyStatusReport = processedPolicyNotification.getPolicyStatusReport();
            List<RepoPath> affectedProjects = processedPolicyNotification.getAffectedRepoKeyPaths();

            List<RepoPath> foundArtifacts = artifactSearchService.findArtifactsUsingComponentNameVersions(componentName, componentVersionName, affectedProjects);
            affectedArtifacts.add(new PolicyAffectedArtifact(foundArtifacts, policyStatusReport));
        }

        return affectedArtifacts;
    }

    private List<VulnerabilityAffectedArtifact> findVulnerabilityAffectedArtifacts(List<ProcessedVulnerabilityNotification> processedVulnerabilityNotifications) {
        List<VulnerabilityAffectedArtifact> affectedArtifacts = new ArrayList<>();
        for (ProcessedVulnerabilityNotification processedVulnerabilityNotification : processedVulnerabilityNotifications) {
            String componentName = processedVulnerabilityNotification.getComponentName();
            String componentVersionName = processedVulnerabilityNotification.getComponentVersionName();
            List<RepoPath> affectedProjects = processedVulnerabilityNotification.getAffectedRepoKeyPaths();

            List<RepoPath> foundArtifacts = artifactSearchService.findArtifactsUsingComponentNameVersions(componentName, componentVersionName, affectedProjects);
            affectedArtifacts.add(new VulnerabilityAffectedArtifact(foundArtifacts, processedVulnerabilityNotification.getVulnerabilityAggregate()));
        }

        return affectedArtifacts;
    }

    private Optional<Date> getLatestNotificationCreatedAtDate(List<NotificationUserView> notificationUserViews) {
        return notificationUserViews.stream()
                   .max(Comparator.comparing(NotificationUserView::getCreatedAt))
                   .map(NotificationUserView::getCreatedAt);
    }

}
