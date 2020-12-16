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
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.NotificationProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.ProcessedPolicyNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.ProcessedVulnerabilityNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.ArtifactInspectionService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;

public class ArtifactNotificationService {
    private final ArtifactSearchService artifactSearchService;
    private final InspectionPropertyService inspectionPropertyService;
    private final ArtifactInspectionService artifactInspectionService;
    private final PolicyNotificationService policyNotificationService;
    private final VulnerabilityNotificationService vulnerabilityNotificationService;
    private final NotificationProcessor notificationProcessor;

    public ArtifactNotificationService(
        ArtifactSearchService artifactSearchService,
        InspectionPropertyService inspectionPropertyService,
        ArtifactInspectionService artifactInspectionService,
        PolicyNotificationService policyNotificationService,
        VulnerabilityNotificationService vulnerabilityNotificationService,
        NotificationProcessor notificationProcessor
    ) {
        this.artifactSearchService = artifactSearchService;
        this.inspectionPropertyService = inspectionPropertyService;
        this.artifactInspectionService = artifactInspectionService;
        this.policyNotificationService = policyNotificationService;
        this.vulnerabilityNotificationService = vulnerabilityNotificationService;
        this.notificationProcessor = notificationProcessor;
    }

    public void updateMetadataFromNotifications(List<RepoPath> repoKeyPaths, Date startDate, Date endDate) throws IntegrationException {
        RepositoryProjectNameLookup repositoryProjectNameLookup = RepositoryProjectNameLookup.fromProperties(inspectionPropertyService, repoKeyPaths);

        // TODO: The code for processing policy and vulnerability notifications looks very similar. We should add another layer of abstractions to do this work.
        PolicyNotifications policyNotifications = policyNotificationService.fetchPolicyNotifications(startDate, endDate);
        List<ProcessedPolicyNotification> processedPolicyNotifications = notificationProcessor.processPolicyNotifications(policyNotifications, repositoryProjectNameLookup);
        List<PolicyAffectedArtifact> policyAffectedArtifacts = findPolicyAffectedArtifacts(processedPolicyNotifications);
        for (PolicyAffectedArtifact affectedArtifact : policyAffectedArtifacts) {
            PolicyStatusReport policyStatusReport = affectedArtifact.getPolicyStatusReport();
            for (RepoPath repoPath : affectedArtifact.getAffectedArtifacts()) {
                inspectionPropertyService.setPolicyProperties(repoPath, policyStatusReport);
            }
        }

        List<VulnerabilityNotificationUserView> vulnerabilityNotificationUserViews = vulnerabilityNotificationService.fetchVulnerabilityNotifications(startDate, endDate);
        List<ProcessedVulnerabilityNotification> processedVulnerabilityNotifications = notificationProcessor.processVulnerabilityNotifications(vulnerabilityNotificationUserViews, repositoryProjectNameLookup);
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
        for (RepoPath repoKeyPath : repoKeyPaths) {
            try {
                ProjectVersionWrapper projectVersionWrapper = artifactInspectionService.fetchProjectVersionWrapper(repoKeyPath.getRepoKey());
                inspectionPropertyService.updateProjectUIUrl(repoKeyPath, projectVersionWrapper.getProjectVersionView());
                inspectionPropertyService.setUpdateStatus(repoKeyPath, UpdateStatus.UP_TO_DATE);
                inspectionPropertyService.setInspectionStatus(repoKeyPath, InspectionStatus.SUCCESS);
                // We don't want to miss notifications, so if something goes wrong we will err on the side of caution.
                inspectionPropertyService.setLastUpdate(repoKeyPath, lastNotificationDate.orElse(startDate));
            } catch (IntegrationException exception) {
                inspectionPropertyService.setUpdateStatus(repoKeyPath, UpdateStatus.OUT_OF_DATE);
                inspectionPropertyService.setInspectionStatus(repoKeyPath, InspectionStatus.PENDING, exception.getMessage());
            }
        }
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
