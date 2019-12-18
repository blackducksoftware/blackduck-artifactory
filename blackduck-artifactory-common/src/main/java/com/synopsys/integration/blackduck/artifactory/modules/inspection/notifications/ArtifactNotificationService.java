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
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.UserView;
import com.synopsys.integration.blackduck.api.manual.component.PolicyInfo;
import com.synopsys.integration.blackduck.api.manual.view.NotificationUserView;
import com.synopsys.integration.blackduck.artifactory.ArtifactSearchService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.UpdateStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.AffectedArtifact;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.BlackDuckNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.PolicyStatusNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.NotificationService;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.NameVersion;

public class ArtifactNotificationService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final NotificationProcessingService notificationProcessingService;
    private final BlackDuckService blackDuckService;
    private final ProjectService projectService;
    private final NotificationService notificationService;
    private final ArtifactSearchService artifactSearchService;
    private final InspectionPropertyService inspectionPropertyService;

    public ArtifactNotificationService(final NotificationProcessingService notificationProcessingService, final BlackDuckService blackDuckService, final ProjectService projectService,
        final NotificationService notificationService,
        final ArtifactSearchService artifactSearchService, final InspectionPropertyService inspectionPropertyService) {
        this.notificationProcessingService = notificationProcessingService;
        this.blackDuckService = blackDuckService;
        this.projectService = projectService;
        this.notificationService = notificationService;
        this.artifactSearchService = artifactSearchService;
        this.inspectionPropertyService = inspectionPropertyService;
    }

    public void updateMetadataFromNotifications(final List<RepoPath> repoKeyPaths, final Date startDate, final Date endDate) throws IntegrationException {
        final UserView currentUser = blackDuckService.getResponse(ApiDiscovery.CURRENT_USER_LINK_RESPONSE);
        final List<NotificationUserView> notificationUserViews = notificationService.getAllUserNotifications(currentUser, startDate, endDate);
        final List<VulnerabilityNotification> vulnerabilityNotifications = notificationProcessingService.getVulnerabilityNotifications(notificationUserViews);
        final List<PolicyStatusNotification> policyStatusNotifications = notificationProcessingService.getPolicyStatusNotifications(notificationUserViews);

        final List<String> repoKeys = repoKeyPaths.stream().map(RepoPath::getRepoKey).collect(Collectors.toList());

        final List<AffectedArtifact<VulnerabilityNotification>> vulnerabilityArtifacts = findVulnerabilityAffectedArtifacts(repoKeys, vulnerabilityNotifications);
        for (final AffectedArtifact<VulnerabilityNotification> vulnerableArtifact : vulnerabilityArtifacts) {
            final RepoPath repoPath = vulnerableArtifact.getRepoPath();
            final VulnerabilityAggregate vulnerabilityAggregate = vulnerableArtifact.getBlackDuckNotification().getVulnerabilityAggregate();
            inspectionPropertyService.setVulnerabilityProperties(repoPath, vulnerabilityAggregate);
        }

        final List<AffectedArtifact<PolicyStatusNotification>> policyArtifacts = findPolicyAffectedArtifacts(repoKeys, policyStatusNotifications);
        for (final AffectedArtifact<PolicyStatusNotification> policyArtifact : policyArtifacts) {
            final RepoPath repoPath = policyArtifact.getRepoPath();
            final PolicyStatusNotification policyStatusNotification = policyArtifact.getBlackDuckNotification();

            final String policyApprovalStatus = policyStatusNotification.getPolicyStatusView().getApprovalStatus().name();
            final PolicySummaryStatusType policySummaryStatusType = PolicySummaryStatusType.valueOf(policyApprovalStatus);
            final List<PolicySeverityType> policySeverityTypes = policyStatusNotification.getPolicyInfos().stream()
                                                                     .map(PolicyInfo::getSeverity)
                                                                     .map(severity -> {
                                                                         if (StringUtils.isBlank(severity)) {
                                                                             return PolicySeverityType.UNSPECIFIED;
                                                                         } else {
                                                                             return PolicySeverityType.valueOf(severity);
                                                                         }
                                                                     })
                                                                     .collect(Collectors.toList());
            final PolicyStatusReport policyStatusReport = new PolicyStatusReport(policySummaryStatusType, policySeverityTypes);

            inspectionPropertyService.setPolicyProperties(repoPath, policyStatusReport);
        }

        final Optional<Date> lastNotificationDate = getLatestNotificationCreatedAtDate(notificationUserViews);

        repoKeyPaths.forEach(repoKeyPath -> {
            if (inspectionPropertyService.assertInspectionStatus(repoKeyPath, InspectionStatus.SUCCESS)) {
                inspectionPropertyService.setUpdateStatus(repoKeyPath, UpdateStatus.UP_TO_DATE);
                inspectionPropertyService.setInspectionStatus(repoKeyPath, InspectionStatus.SUCCESS, null, null);
                // We don't want to miss notifications, so if something goes wrong we will err on the side of caution.
                inspectionPropertyService.setLastUpdate(repoKeyPath, lastNotificationDate.orElse(startDate));
                final String repoKey = repoKeyPath.getRepoKey();
                final String repoProjectName = inspectionPropertyService.getRepoProjectName(repoKey);
                final String repoProjectVersionName = inspectionPropertyService.getRepoProjectVersionName(repoKey);
                try {
                    final Optional<ProjectVersionWrapper> projectVersionWrapper = projectService.getProjectVersion(repoProjectName, repoProjectVersionName);
                    if (projectVersionWrapper.isPresent()) {
                        final ProjectVersionView projectVersionView = projectVersionWrapper.get().getProjectVersionView();
                        inspectionPropertyService.updateProjectUIUrl(repoKeyPath, projectVersionView);
                    }
                } catch (final IntegrationException e) {
                    logger.debug(String.format("Failed to update %s on repo '%s'", BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL.getPropertyName(), repoKey));
                }
            } else {
                logger.debug(String.format("Not updating from notifications for repo '%s' because the repository has not been initialized.", repoKeyPath.getRepoKey()));
            }
        });
    }

    private Optional<Date> getLatestNotificationCreatedAtDate(final List<NotificationUserView> notificationUserViews) {
        return notificationUserViews.stream()
                   .max(Comparator.comparing(NotificationUserView::getCreatedAt))
                   .map(NotificationUserView::getCreatedAt);
    }

    private List<AffectedArtifact<PolicyStatusNotification>> findPolicyAffectedArtifacts(final List<String> repoKeys, final List<PolicyStatusNotification> policyStatusNotifications) {
        return policyStatusNotifications.stream()
                   .map(notification -> findPolicyAffectedArtifacts(repoKeys, notification))
                   .flatMap(List::stream)
                   .collect(Collectors.toList());
    }

    private List<AffectedArtifact<PolicyStatusNotification>> findPolicyAffectedArtifacts(final List<String> repoKeys, final PolicyStatusNotification notification) {
        List<AffectedArtifact<PolicyStatusNotification>> affectedArtifacts = new ArrayList<>();

        final String componentName = notification.getComponentView().getName();
        final String componentVersionName = notification.getComponentVersionView().getVersionName();

        final List<NameVersion> affectedProjectVersions = notification.getAffectedProjectVersions();
        final List<String> affectedRepoKeys = determineAffectedRepos(repoKeys, affectedProjectVersions);

        if (affectedRepoKeys.size() > 0) {
            final List<RepoPath> artifactsWithComponentNameVersion = artifactSearchService.findArtifactsWithComponentNameVersion(componentName, componentVersionName, repoKeys);

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

    private List<AffectedArtifact<VulnerabilityNotification>> findVulnerabilityAffectedArtifacts(final List<String> repoKeys, final List<VulnerabilityNotification> vulnerabilityNotifications) {
        return vulnerabilityNotifications.stream()
                   .map(notification -> findVulnerabilityAffectedArtifacts(repoKeys, notification))
                   .flatMap(List::stream)
                   .collect(Collectors.toList());
    }

    private List<AffectedArtifact<VulnerabilityNotification>> findVulnerabilityAffectedArtifacts(final List<String> repoKeys, final VulnerabilityNotification notification) {
        List<AffectedArtifact<VulnerabilityNotification>> affectedArtifacts = new ArrayList<>();

        final List<NameVersion> affectedProjectVersions = notification.getAffectedProjectVersions();
        final List<String> affectedRepoKeys = determineAffectedRepos(repoKeys, affectedProjectVersions);

        if (affectedRepoKeys.size() > 0) {
            final List<RepoPath> artifactsWithOriginId = artifactSearchService.findArtifactsWithOriginId(notification.getComponentVersionOriginName(), notification.getComponentVersionOriginId(), repoKeys);

            artifactsWithOriginId.stream()
                .map(repoPath -> new AffectedArtifact<>(repoPath, notification))
                .forEach(affectedArtifacts::add);

            if (artifactsWithOriginId.isEmpty()) {
                logger.error(String.format(
                    "Failed to find artifact that matches notification content (%s:%s) in the affected repositories: %s. Defaulting to legacy search.",
                    notification.getComponentVersionOriginName(),
                    notification.getComponentVersionOriginId(),
                    StringUtils.join(repoKeys, ',')
                ));
                affectedArtifacts = legacySearchForAffectedArtifacts(repoKeys, notification);
            }
        }

        return affectedArtifacts;
    }

    private <T extends BlackDuckNotification> List<AffectedArtifact<T>> legacySearchForAffectedArtifacts(final List<String> repoKeys, final T notification) {
        List<AffectedArtifact<T>> affectedArtifacts = Collections.emptyList();
        try {
            final List<NameVersion> affectedProjectVersions = notification.getAffectedProjectVersions();
            final List<String> affectedRepoKeys = determineAffectedRepos(repoKeys, affectedProjectVersions);

            if (affectedRepoKeys.size() > 0) {
                final ComponentVersionView componentVersionView = notification.getComponentVersionView();
                final int totalLimit = 550;
                final List<OriginView> originViews = blackDuckService.getSomeResponses(componentVersionView, ComponentVersionView.ORIGINS_LINK_RESPONSE, totalLimit);

                // TODO: We should be attempting to get a match from artifactory while getting the responses.
                if (originViews.size() >= totalLimit) {
                    final Optional<ComponentView> componentView = blackDuckService.getResponse(componentVersionView, ComponentVersionView.COMPONENT_LINK_RESPONSE);
                    final String affectedRepos = String.join(",", affectedRepoKeys);
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
        } catch (final IntegrationException e) {
            logger.error(String.format("Failed to get origins for: %s", notification.getComponentVersionView().getHref().orElse("Unknown")), e);
        }

        return affectedArtifacts;
    }

    private List<String> determineAffectedRepos(final List<String> repoKeys, final List<NameVersion> affectedProjectVersions) {
        final List<String> affectedRepos = new ArrayList<>();
        final Map<String, String> nameVersionToRepoKeyMap = projectNameVersionToRepoKey(repoKeys);
        for (final NameVersion nameVersion : affectedProjectVersions) {
            final String projectName = nameVersion.getName();
            final String projectVersionName = nameVersion.getVersion();
            final String projectNameVersionKey = generateProjectNameKey(projectName, projectVersionName);
            final String repoKey = nameVersionToRepoKeyMap.get(projectNameVersionKey);
            affectedRepos.add(repoKey);
        }

        return affectedRepos;
    }

    private Map<String, String> projectNameVersionToRepoKey(final List<String> repoKeys) {
        final Map<String, String> nameVersionToRepoKeyMap = new HashMap<>();
        for (final String repoKey : repoKeys) {
            final String repoProjectName = inspectionPropertyService.getRepoProjectName(repoKey);
            final String repoProjectVersionName = inspectionPropertyService.getRepoProjectVersionName(repoKey);
            final String key = generateProjectNameKey(repoProjectName, repoProjectVersionName);
            nameVersionToRepoKeyMap.put(key, repoKey);
        }

        return nameVersionToRepoKeyMap;
    }

    private String generateProjectNameKey(final String projectName, final String projectVersionName) {
        return projectName + ":" + projectVersionName;
    }
}
