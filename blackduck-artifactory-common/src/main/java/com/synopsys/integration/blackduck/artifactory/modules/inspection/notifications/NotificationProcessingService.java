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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.UriSingleResponse;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ComponentView;
import com.synopsys.integration.blackduck.api.generated.view.PolicyStatusView;
import com.synopsys.integration.blackduck.api.generated.view.VulnerabilityView;
import com.synopsys.integration.blackduck.api.manual.component.ComponentVersionStatus;
import com.synopsys.integration.blackduck.api.manual.component.PolicyInfo;
import com.synopsys.integration.blackduck.api.manual.component.PolicyOverrideNotificationContent;
import com.synopsys.integration.blackduck.api.manual.component.RuleViolationClearedNotificationContent;
import com.synopsys.integration.blackduck.api.manual.component.RuleViolationNotificationContent;
import com.synopsys.integration.blackduck.api.manual.component.VulnerabilityNotificationContent;
import com.synopsys.integration.blackduck.api.manual.view.NotificationUserView;
import com.synopsys.integration.blackduck.api.manual.view.PolicyOverrideNotificationUserView;
import com.synopsys.integration.blackduck.api.manual.view.RuleViolationClearedNotificationUserView;
import com.synopsys.integration.blackduck.api.manual.view.RuleViolationNotificationUserView;
import com.synopsys.integration.blackduck.api.manual.view.VulnerabilityNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.PolicyStatusNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityNotification;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.NameVersion;

public class NotificationProcessingService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final BlackDuckService blackDuckService;

    public NotificationProcessingService(final BlackDuckService blackDuckService) {
        this.blackDuckService = blackDuckService;
    }

    public List<PolicyStatusNotification> getPolicyStatusNotifications(final List<NotificationUserView> notificationUserViews) {
        return notificationUserViews.stream()
                   .map(this::convertToPolicyStatusNotification)
                   .flatMap(List::stream)
                   .collect(Collectors.toList());
    }

    private List<PolicyStatusNotification> convertToPolicyStatusNotification(final NotificationUserView notificationUserView) {
        final List<PolicyStatusNotification> policyStatusNotifications = new ArrayList<>();

        try {
            if (notificationUserView instanceof RuleViolationNotificationUserView) {
                final RuleViolationNotificationUserView ruleViolationNotificationView = (RuleViolationNotificationUserView) notificationUserView;
                final RuleViolationNotificationContent content = ruleViolationNotificationView.getContent();
                final List<ComponentVersionStatus> componentVersionStatuses = content.getComponentVersionStatuses();
                final List<PolicyStatusNotification> notifications = createPolicyStatusNotifications(componentVersionStatuses, content.getProjectName(), content.getProjectVersionName(), content.getPolicyInfos());
                policyStatusNotifications.addAll(notifications);
            } else if (notificationUserView instanceof RuleViolationClearedNotificationUserView) {
                final RuleViolationClearedNotificationUserView ruleViolationClearedNotificationUserView = (RuleViolationClearedNotificationUserView) notificationUserView;
                final RuleViolationClearedNotificationContent content = ruleViolationClearedNotificationUserView.getContent();
                final List<ComponentVersionStatus> componentVersionStatuses = content.getComponentVersionStatuses();
                final List<PolicyStatusNotification> notifications = createPolicyStatusNotifications(componentVersionStatuses, content.getProjectName(), content.getProjectVersionName(), content.getPolicyInfos());
                policyStatusNotifications.addAll(notifications);
            } else if (notificationUserView instanceof PolicyOverrideNotificationUserView) {
                final PolicyOverrideNotificationUserView policyOverrideNotificationUserView = (PolicyOverrideNotificationUserView) notificationUserView;
                final PolicyOverrideNotificationContent content = policyOverrideNotificationUserView.getContent();
                final NameVersion affectedProjectVersion = new NameVersion(content.getProjectName(), content.getProjectVersionName());
                final PolicyStatusNotification policyStatusNotification = createPolicyStatusNotification(Collections.singletonList(affectedProjectVersion), content.getComponentVersion(), content.getBomComponentVersionPolicyStatus(),
                    content.getPolicyInfos());
                policyStatusNotifications.add(policyStatusNotification);
            }
        } catch (final IntegrationException e) {
            logger.debug("Failed to extract policy data from notification.", e);
        }

        return policyStatusNotifications;
    }

    private List<PolicyStatusNotification> createPolicyStatusNotifications(final List<ComponentVersionStatus> componentVersionStatuses, final String projectName, final String projectVersionName, final List<PolicyInfo> policyInfos) {
        final NameVersion affectedProjectVersion = new NameVersion(projectName, projectVersionName);
        final List<NameVersion> affectedProjectVersions = Collections.singletonList(affectedProjectVersion);
        final List<PolicyStatusNotification> policyStatusNotifications = new ArrayList<>();
        for (final ComponentVersionStatus componentVersionStatus : componentVersionStatuses) {
            try {
                final PolicyStatusNotification policyStatusNotification = createPolicyStatusNotification(affectedProjectVersions, componentVersionStatus.getComponentVersion(), componentVersionStatus.getBomComponentVersionPolicyStatus(),
                    policyInfos);
                policyStatusNotifications.add(policyStatusNotification);
            } catch (final IntegrationException e) {
                logger.debug(String.format("Failed to get policy status for component '%s==%s' in project '%s' version '%s' from notification. The project version might not exist.", componentVersionStatus.getComponentName(),
                    componentVersionStatus.getComponentVersion(), projectName,
                    projectVersionName));
            }
        }

        return policyStatusNotifications;
    }

    private PolicyStatusNotification createPolicyStatusNotification(final List<NameVersion> affectedProjectVersions, final String componentVersionUrl, final String policyStatusUrl, final List<PolicyInfo> policyInfos)
        throws IntegrationException {
        final UriSingleResponse<ComponentVersionView> componentVersionViewUriSingleResponse = new UriSingleResponse<>(componentVersionUrl, ComponentVersionView.class);
        final ComponentVersionView componentVersionView = blackDuckService.getResponse(componentVersionViewUriSingleResponse);
        final ComponentView componentView = blackDuckService.getResponse(componentVersionView, ComponentVersionView.COMPONENT_LINK_RESPONSE).orElseThrow(
            () -> new IntegrationException(String.format("Failed to get the %s link from %s", ComponentVersionView.COMPONENT_LINK, componentVersionView.getHref().orElse("unknown ComponentVersionView.")))
        );
        final UriSingleResponse<PolicyStatusView> policyStatusViewUriSingleResponse = new UriSingleResponse<>(policyStatusUrl, PolicyStatusView.class);
        final PolicyStatusView policyStatus = blackDuckService.getResponse(policyStatusViewUriSingleResponse);

        return new PolicyStatusNotification(affectedProjectVersions, componentVersionView, componentView, policyStatus, policyInfos);
    }

    public List<VulnerabilityNotification> getVulnerabilityNotifications(final List<NotificationUserView> notificationUserViews) {
        return notificationUserViews.stream()
                   .filter(VulnerabilityNotificationUserView.class::isInstance)
                   .map(VulnerabilityNotificationUserView.class::cast)
                   .map(this::aggregateVulnerabilityNotification)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .collect(Collectors.toList());
    }

    private Optional<VulnerabilityNotification> aggregateVulnerabilityNotification(final VulnerabilityNotificationUserView notificationUserView) {
        VulnerabilityNotification vulnerabilityNotification = null;
        try {
            final VulnerabilityNotificationContent content = notificationUserView.getContent();
            final String componentVersionOriginId = content.getComponentVersionOriginId();
            final String componentVersionOriginName = content.getComponentVersionOriginName();
            final UriSingleResponse<ComponentVersionView> componentVersionViewUri = new UriSingleResponse<>(content.getComponentVersion(), ComponentVersionView.class);
            final ComponentVersionView componentVersionView = blackDuckService.getResponse(componentVersionViewUri);
            final Optional<String> vulnerabilitiesLink = componentVersionView.getFirstLink(ComponentVersionView.VULNERABILITIES_LINK);

            if (vulnerabilitiesLink.isPresent()) {
                final List<VulnerabilityView> componentVulnerabilities = blackDuckService.getAllResponses(vulnerabilitiesLink.get(), VulnerabilityView.class);
                final VulnerabilityAggregate vulnerabilityAggregate = VulnerabilityAggregate.fromVulnerabilityViews(componentVulnerabilities);
                final List<NameVersion> affectedProjectVersions = content.getAffectedProjectVersions().stream()
                                                                      .map(affectedProjectVersion -> new NameVersion(affectedProjectVersion.getProjectName(), affectedProjectVersion.getProjectVersionName()))
                                                                      .collect(Collectors.toList());
                vulnerabilityNotification = new VulnerabilityNotification(affectedProjectVersions, componentVersionView, vulnerabilityAggregate, componentVersionOriginName, componentVersionOriginId);
            } else {
                throw new IntegrationException(String.format("Failed to find vulnerabilities link for component with origin id '%s:%s'", componentVersionOriginName, componentVersionOriginId));
            }
        } catch (final IntegrationException e) {
            logger.debug("An error occurred when attempting to aggregate vulnerabilities from a notification.", e);
        }

        return Optional.ofNullable(vulnerabilityNotification);
    }
}
