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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.enumeration.PolicySeverityType;
import com.synopsys.integration.blackduck.api.generated.discovery.ApiDiscovery;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ComponentView;
import com.synopsys.integration.blackduck.api.generated.view.OriginView;
import com.synopsys.integration.blackduck.api.generated.view.UserView;
import com.synopsys.integration.blackduck.api.manual.component.PolicyInfo;
import com.synopsys.integration.blackduck.api.manual.view.NotificationUserView;
import com.synopsys.integration.blackduck.api.manual.view.VulnerabilityNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.ArtifactSearchService;
import com.synopsys.integration.blackduck.artifactory.modules.UpdateStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.AffectedArtifact;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.BlackDuckNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.PolicyAffectedArtifact;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.PolicyNotifications;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.PolicyStatusNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerablityAffectedArtifact;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.PolicyOverrideProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.PolicyRuleClearedProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.PolicyViolationProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.ProcessedPolicyNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.ProcessedVulnerabilityNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.VulnerabilityProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.NotificationService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.NameVersion;

public class ArtifactNotificationService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final NotificationProcessingService notificationProcessingService;
    private final BlackDuckService blackDuckService;
    private final NotificationService notificationService;
    private final ArtifactSearchService artifactSearchService;
    private final InspectionPropertyService inspectionPropertyService;
    private final PolicyNotificationService policyNotificationService;
    private final VulnerabilityNotificationService vulnerabilityNotificationService;

    public ArtifactNotificationService(NotificationProcessingService notificationProcessingService, BlackDuckService blackDuckService, NotificationService notificationService, ArtifactSearchService artifactSearchService,
        InspectionPropertyService inspectionPropertyService, PolicyNotificationService policyNotificationService, VulnerabilityNotificationService vulnerabilityNotificationService) {
        this.notificationProcessingService = notificationProcessingService;
        this.blackDuckService = blackDuckService;
        this.notificationService = notificationService;
        this.artifactSearchService = artifactSearchService;
        this.inspectionPropertyService = inspectionPropertyService;
        this.policyNotificationService = policyNotificationService;
        this.vulnerabilityNotificationService = vulnerabilityNotificationService;
    }

    public void updateMetadataFromNotifications2(List<RepoPath> repoKeyPaths, Date startDate, Date endDate) throws IntegrationException {
        PolicyNotifications policyNotifications = policyNotificationService.fetchPolicyNotifications(startDate, endDate);
        NotificationRepositoryFilter notificationRepositoryFilter = NotificationRepositoryFilter.fromProperties(inspectionPropertyService, repoKeyPaths);

        List<PolicyAffectedArtifact> policyAffectedArtifacts = new ArrayList<>();

        PolicyOverrideProcessor policyOverrideProcessor = new PolicyOverrideProcessor(blackDuckService);
        List<ProcessedPolicyNotification> processedOverrideNotifications = policyOverrideProcessor.processPolicyOverrideNotifications(policyNotifications.getPolicyOverrideNotificationUserViews(), notificationRepositoryFilter);
        policyAffectedArtifacts.addAll(findPolicyAffectedArtifacts(processedOverrideNotifications));

        PolicyRuleClearedProcessor policyRuleClearedProcessor = new PolicyRuleClearedProcessor(blackDuckService);
        List<ProcessedPolicyNotification> processedRuleClearedNotifications = policyRuleClearedProcessor
                                                                                  .processPolicyRuleClearedNotifications(policyNotifications.getRuleViolationClearedNotificationUserViews(), notificationRepositoryFilter);
        policyAffectedArtifacts.addAll(findPolicyAffectedArtifacts(processedRuleClearedNotifications));

        PolicyViolationProcessor policyViolationProcessor = new PolicyViolationProcessor(blackDuckService);
        List<ProcessedPolicyNotification> processedViolationNotifications = policyViolationProcessor.processPolicyViolationNotifications(policyNotifications.getRuleViolationNotificationUserViews(), notificationRepositoryFilter);
        policyAffectedArtifacts.addAll(findPolicyAffectedArtifacts(processedViolationNotifications));

        for (PolicyAffectedArtifact affectedArtifact : policyAffectedArtifacts) {
            PolicyStatusReport policyStatusReport = affectedArtifact.getPolicyStatusReport();
            for (RepoPath repoPath : affectedArtifact.getAffectedArtifacts()) {
                inspectionPropertyService.setPolicyProperties(repoPath, policyStatusReport);
            }
        }

        List<VulnerabilityNotificationUserView> vulnerabilityNotificationUserViews = vulnerabilityNotificationService.fetchVulnerabilityNotifications(startDate, endDate);
        VulnerabilityProcessor vulnerabilityProcessor = new VulnerabilityProcessor(blackDuckService);
        List<ProcessedVulnerabilityNotification> processedVulnerabilityNotifications = vulnerabilityProcessor.processVulnerabilityNotifications(vulnerabilityNotificationUserViews, notificationRepositoryFilter);
        List<VulnerablityAffectedArtifact> vulnerabilityAffectedArtifacts = findVulnerabilityAffectedArtifacts(processedVulnerabilityNotifications);

        for (VulnerablityAffectedArtifact affectedArtifact : vulnerabilityAffectedArtifacts) {
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

    private List<VulnerablityAffectedArtifact> findVulnerabilityAffectedArtifacts(List<ProcessedVulnerabilityNotification> processedVulnerabilityNotifications) {
        List<VulnerablityAffectedArtifact> affectedArtifacts = new ArrayList<>();
        for (ProcessedVulnerabilityNotification processedVulnerabilityNotification : processedVulnerabilityNotifications) {
            String componentName = processedVulnerabilityNotification.getComponentName();
            String componentVersionName = processedVulnerabilityNotification.getComponentVersionName();
            List<RepoPath> affectedProjects = processedVulnerabilityNotification.getAffectedRepoKeyPaths();

            List<RepoPath> foundArtifacts = artifactSearchService.findArtifactsUsingComponentNameVersions(componentName, componentVersionName, affectedProjects);
            affectedArtifacts.add(new VulnerablityAffectedArtifact(foundArtifacts, processedVulnerabilityNotification.getVulnerabilityAggregate()));
        }

        return affectedArtifacts;
    }

    public void updateMetadataFromNotifications(List<RepoPath> repoKeyPaths, Date startDate, Date endDate) throws IntegrationException {
        UserView currentUser = blackDuckService.getResponse(ApiDiscovery.CURRENT_USER_LINK_RESPONSE);
        List<NotificationUserView> notificationUserViews = notificationService.getAllUserNotifications(currentUser, startDate, endDate);

        List<VulnerabilityNotification> vulnerabilityNotifications = notificationProcessingService.getVulnerabilityNotifications(notificationUserViews);
        List<PolicyStatusNotification> policyStatusNotifications = notificationProcessingService.getPolicyStatusNotifications(notificationUserViews);

        List<String> repoKeys = repoKeyPaths.stream().map(RepoPath::getRepoKey).collect(Collectors.toList());

        List<AffectedArtifact<VulnerabilityNotification>> vulnerabilityArtifacts = findVulnerabilityAffectedArtifacts(repoKeys, vulnerabilityNotifications);
        for (AffectedArtifact<VulnerabilityNotification> vulnerableArtifact : vulnerabilityArtifacts) {
            RepoPath repoPath = vulnerableArtifact.getRepoPath();
            VulnerabilityAggregate vulnerabilityAggregate = vulnerableArtifact.getBlackDuckNotification().getVulnerabilityAggregate();
            inspectionPropertyService.setVulnerabilityProperties(repoPath, vulnerabilityAggregate);
        }

        List<AffectedArtifact<PolicyStatusNotification>> policyArtifacts = findPolicyAffectedArtifacts(repoKeys, policyStatusNotifications);
        for (AffectedArtifact<PolicyStatusNotification> policyArtifact : policyArtifacts) {
            RepoPath repoPath = policyArtifact.getRepoPath();
            PolicyStatusNotification policyStatusNotification = policyArtifact.getBlackDuckNotification();

            String policyApprovalStatus = policyStatusNotification.getPolicyStatusView().getApprovalStatus().name();
            PolicySummaryStatusType policySummaryStatusType = PolicySummaryStatusType.valueOf(policyApprovalStatus);
            List<PolicySeverityType> policySeverityTypes = policyStatusNotification.getPolicyInfos().stream()
                                                               .map(PolicyInfo::getSeverity)
                                                               .map(severity -> {
                                                                   if (StringUtils.isBlank(severity)) {
                                                                       return PolicySeverityType.UNSPECIFIED;
                                                                   } else {
                                                                       return PolicySeverityType.valueOf(severity);
                                                                   }
                                                               })
                                                               .collect(Collectors.toList());
            PolicyStatusReport policyStatusReport = new PolicyStatusReport(policySummaryStatusType, policySeverityTypes);

            inspectionPropertyService.setPolicyProperties(repoPath, policyStatusReport);
        }

        Optional<Date> lastNotificationDate = getLatestNotificationCreatedAtDate(notificationUserViews);

        repoKeyPaths.forEach(repoKeyPath -> {
            inspectionPropertyService.setUpdateStatus(repoKeyPath, UpdateStatus.UP_TO_DATE);
            inspectionPropertyService.setInspectionStatus(repoKeyPath, InspectionStatus.SUCCESS);
            // We don't want to miss notifications, so if something goes wrong we will err on the side of caution.
            inspectionPropertyService.setLastUpdate(repoKeyPath, lastNotificationDate.orElse(startDate));
        });
    }

    private Optional<Date> getLatestNotificationCreatedAtDate(List<NotificationUserView> notificationUserViews) {
        return notificationUserViews.stream()
                   .max(Comparator.comparing(NotificationUserView::getCreatedAt))
                   .map(NotificationUserView::getCreatedAt);
    }

    private List<AffectedArtifact<PolicyStatusNotification>> findPolicyAffectedArtifacts(List<String> repoKeys, List<PolicyStatusNotification> policyStatusNotifications) {
        return policyStatusNotifications.stream()
                   .map(notification -> findPolicyAffectedArtifacts(repoKeys, notification))
                   .flatMap(List::stream)
                   .collect(Collectors.toList());
    }

    private List<AffectedArtifact<PolicyStatusNotification>> findPolicyAffectedArtifacts(List<String> repoKeys, PolicyStatusNotification notification) {
        List<AffectedArtifact<PolicyStatusNotification>> affectedArtifacts = new ArrayList<>();

        String componentName = notification.getComponentView().getName();
        String componentVersionName = notification.getComponentVersionView().getVersionName();

        List<NameVersion> affectedProjectVersions = notification.getAffectedProjectVersions();
        List<String> affectedRepoKeys = determineAffectedRepos(repoKeys, affectedProjectVersions);

        if (affectedRepoKeys.size() > 0) {
            List<RepoPath> artifactsWithComponentNameVersion = artifactSearchService.findArtifactsWithComponentNameVersion(componentName, componentVersionName, repoKeys);

            artifactsWithComponentNameVersion.stream()
                .map(repoPath -> new AffectedArtifact<>(repoPath, notification))
                .forEach(affectedArtifacts::add);

            if (artifactsWithComponentNameVersion.isEmpty()) {
                logger.warn(
                    String.format("Failed to find artifact that matches notification content (%s-%s) in the affected repositories: %s. Defaulting to legacy search.", componentName, componentVersionName, StringUtils.join(repoKeys, ',')));
                affectedArtifacts = legacySearchForAffectedArtifacts(repoKeys, notification);
            }
        }

        return affectedArtifacts;
    }

    private List<AffectedArtifact<VulnerabilityNotification>> findVulnerabilityAffectedArtifacts(List<String> repoKeys, List<VulnerabilityNotification> vulnerabilityNotifications) {
        return vulnerabilityNotifications.stream()
                   .map(notification -> findVulnerabilityAffectedArtifacts(repoKeys, notification))
                   .flatMap(List::stream)
                   .collect(Collectors.toList());
    }

    private List<AffectedArtifact<VulnerabilityNotification>> findVulnerabilityAffectedArtifacts(List<String> repoKeys, VulnerabilityNotification notification) {
        List<AffectedArtifact<VulnerabilityNotification>> affectedArtifacts = new ArrayList<>();

        List<NameVersion> affectedProjectVersions = notification.getAffectedProjectVersions();
        List<String> affectedRepoKeys = determineAffectedRepos(repoKeys, affectedProjectVersions);

        if (affectedRepoKeys.size() > 0) {
            List<RepoPath> artifactsWithOriginId = artifactSearchService.findArtifactsWithOriginId(notification.getComponentVersionOriginName(), notification.getComponentVersionOriginId(), repoKeys);

            artifactsWithOriginId.stream()
                .map(repoPath -> new AffectedArtifact<>(repoPath, notification))
                .forEach(affectedArtifacts::add);

            if (artifactsWithOriginId.isEmpty()) {
                logger.error(String.format(
                    "Failed to find artifact that matches notification content (%s:%s) in the affected repositories: %s. Defaulting to legacy search.",
                    notification.getComponentVersionOriginName(),
                    notification.getComponentVersionOriginId(),
                    StringUtils.join(affectedRepoKeys, ',')
                ));
                affectedArtifacts = legacySearchForAffectedArtifacts(repoKeys, notification);
            }
        }

        return affectedArtifacts;
    }

    private <T extends BlackDuckNotification> List<AffectedArtifact<T>> legacySearchForAffectedArtifacts(List<String> repoKeys, T notification) {
        List<AffectedArtifact<T>> affectedArtifacts = Collections.emptyList();
        try {
            List<NameVersion> affectedProjectVersions = notification.getAffectedProjectVersions();
            List<String> affectedRepoKeys = determineAffectedRepos(repoKeys, affectedProjectVersions);

            if (affectedRepoKeys.size() > 0) {
                ComponentVersionView componentVersionView = notification.getComponentVersionView();
                final int totalLimit = 550;
                List<OriginView> originViews = blackDuckService.getSomeResponses(componentVersionView, ComponentVersionView.ORIGINS_LINK_RESPONSE, totalLimit);

                // TODO: We should be attempting to get a match from artifactory while getting the responses.
                if (originViews.size() >= totalLimit) {
                    Optional<ComponentView> componentView = blackDuckService.getResponse(componentVersionView, ComponentVersionView.COMPONENT_LINK_RESPONSE);
                    String affectedRepos = String.join(",", affectedRepoKeys);
                    if (componentView.isPresent()) {
                        logger.error(String.format("Origin limit reached. Failed to update policy status for component '%s==%s' in one or more of the following repositories: %s."
                                                       + " This will require the blackduck properties to be deleted from that component manually wherever it appears to insure it is up to date.",
                            componentView.get().getName(), componentVersionView.getVersionName(), affectedRepos));
                    } else {
                        logger.error(String.format("Origin limit reached. Failed to update policy status for component '%s' one or more of the following repositories: %s.", componentVersionView.getHref(), affectedRepos)
                                         + " This will require the blackduck properties to be deleted from that component manually wherever it appears to insure it is up to date.");
                    }
                } else {
                    affectedArtifacts = originViews.stream()
                                            .filter(originView -> originView.getOriginId() != null)
                                            .filter(originView -> originView.getOriginName() != null)
                                            .map(originView -> artifactSearchService.findArtifactsWithOriginId(originView.getOriginName(), originView.getOriginId(), affectedRepoKeys))
                                            .flatMap(List::stream)
                                            .map(repoPath -> new AffectedArtifact<>(repoPath, notification))
                                            .collect(Collectors.toList());
                }
            }
        } catch (IntegrationException e) {
            logger.error(String.format("Failed to get origins for: %s", notification.getComponentVersionView().getHref().orElse("Unknown")), e);
        }

        logger.debug(String.format("Found %d artifacts affected by notification with legacy search.", affectedArtifacts.size()));

        return affectedArtifacts;
    }

    private List<String> determineAffectedRepos(List<String> repoKeys, List<NameVersion> affectedProjectVersions) {
        List<String> affectedRepos = new ArrayList<>();
        Map<String, String> nameVersionToRepoKeyMap = projectNameVersionToRepoKey(repoKeys);
        for (NameVersion nameVersion : affectedProjectVersions) {
            String projectName = nameVersion.getName();
            String projectVersionName = nameVersion.getVersion();
            String projectNameVersionKey = generateProjectNameKey(projectName, projectVersionName);
            String repoKey = nameVersionToRepoKeyMap.get(projectNameVersionKey);

            if (repoKey != null) {
                affectedRepos.add(repoKey);
            }
        }

        return affectedRepos;
    }

    private Map<String, String> projectNameVersionToRepoKey(List<String> repoKeys) {
        Map<String, String> nameVersionToRepoKeyMap = new HashMap<>();
        for (String repoKey : repoKeys) {
            String repoProjectName = inspectionPropertyService.getRepoProjectName(repoKey);
            String repoProjectVersionName = inspectionPropertyService.getRepoProjectVersionName(repoKey);
            String key = generateProjectNameKey(repoProjectName, repoProjectVersionName);
            nameVersionToRepoKeyMap.put(key, repoKey);
        }

        return nameVersionToRepoKeyMap;
    }

    private String generateProjectNameKey(String projectName, String projectVersionName) {
        return projectName + ":" + projectVersionName;
    }
}
