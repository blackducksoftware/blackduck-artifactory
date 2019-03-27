/**
 * blackduck-artifactory-common
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.notification.content.detail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.synopsys.integration.blackduck.api.generated.enumeration.NotificationType;
import com.synopsys.integration.blackduck.api.manual.component.AffectedProjectVersion;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.notification.CommonNotificationView;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.notification.NotificationDetailResult;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.notification.content.BomEditContent;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.notification.content.ComponentVersionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.notification.content.NotificationContent;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.notification.content.PolicyInfo;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.notification.content.PolicyOverrideNotificationContent;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.notification.content.RuleViolationClearedNotificationContent;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.notification.content.RuleViolationNotificationContent;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.notification.content.VulnerabilityNotificationContent;

public class NotificationContentDetailFactory {
    private final Gson gson;

    public NotificationContentDetailFactory(final Gson gson) {
        this.gson = gson;
    }

    public NotificationDetailResult generateContentDetails(final CommonNotificationView view) {
        final NotificationType type = view.getType();
        final String notificationJson = view.getJson();
        final JsonObject jsonObject = gson.fromJson(notificationJson, JsonObject.class);

        NotificationContent notificationContent = null;
        String notificationGroup = null;
        final List<NotificationContentDetail> notificationContentDetails = new ArrayList<>();

        if (NotificationType.POLICY_OVERRIDE.equals(type)) {
            notificationContent = gson.fromJson(jsonObject.get("content"), PolicyOverrideNotificationContent.class);
            notificationGroup = NotificationContentDetail.CONTENT_KEY_GROUP_POLICY;
            populateContentDetails(notificationContentDetails, notificationGroup, (PolicyOverrideNotificationContent) notificationContent);
        } else if (NotificationType.RULE_VIOLATION.equals(type)) {
            notificationContent = gson.fromJson(jsonObject.get("content"), RuleViolationNotificationContent.class);
            notificationGroup = NotificationContentDetail.CONTENT_KEY_GROUP_POLICY;
            populateContentDetails(notificationContentDetails, notificationGroup, (RuleViolationNotificationContent) notificationContent);
        } else if (NotificationType.RULE_VIOLATION_CLEARED.equals(type)) {
            notificationContent = gson.fromJson(jsonObject.get("content"), RuleViolationClearedNotificationContent.class);
            notificationGroup = NotificationContentDetail.CONTENT_KEY_GROUP_POLICY;
            populateContentDetails(notificationContentDetails, notificationGroup, (RuleViolationClearedNotificationContent) notificationContent);
        } else if (NotificationType.VULNERABILITY.equals(type)) {
            notificationContent = gson.fromJson(jsonObject.get("content"), VulnerabilityNotificationContent.class);
            notificationGroup = NotificationContentDetail.CONTENT_KEY_GROUP_VULNERABILITY;
            populateContentDetails(notificationContentDetails, notificationGroup, (VulnerabilityNotificationContent) notificationContent);
        } else if (NotificationType.BOM_EDIT.equals(type)) {
            notificationContent = gson.fromJson(jsonObject.get("content"), BomEditContent.class);
            notificationGroup = NotificationContentDetail.CONTENT_KEY_GROUP_BOM_EDIT;
            populateContentDetails(notificationContentDetails, notificationGroup, (BomEditContent) notificationContent);
        }

        return new NotificationDetailResult(notificationContent, view.getContentType(), view.getCreatedAt(), view.getType(), notificationGroup, view.getNotificationState(), notificationContentDetails);
    }

    public void populateContentDetails(final List<NotificationContentDetail> notificationContentDetails, final String notificationGroup, final PolicyOverrideNotificationContent content) {
        for (final PolicyInfo policyInfo : content.policyInfos) {
            final String componentValue;
            if (content.componentVersion != null) {
                componentValue = null;
            } else {
                componentValue = content.component;
            }
            final NotificationContentDetail detail = NotificationContentDetail.createDetail(notificationGroup, Optional.of(content.projectName), Optional.of(content.projectVersionName), Optional.of(content.projectVersion),
                Optional.of(content.componentName), Optional.ofNullable(componentValue), Optional.ofNullable(content.componentVersionName), Optional.ofNullable(content.componentVersion), Optional.of(policyInfo.policyName),
                Optional.of(policyInfo.policy), Optional.empty(), Optional.empty(), Optional.empty(), Optional.ofNullable(content.bomComponent));
            notificationContentDetails.add(detail);
        }
    }

    public void populateContentDetails(final List<NotificationContentDetail> notificationContentDetails, final String notificationGroup, final RuleViolationNotificationContent content) {
        final Map<String, String> uriToName = content.policyInfos.stream().collect(Collectors.toMap(policyInfo -> policyInfo.policy, policyInfo -> policyInfo.policyName));
        for (final ComponentVersionStatus componentVersionStatus : content.componentVersionStatuses) {
            for (final String policyUri : componentVersionStatus.policies) {
                final String policyName = uriToName.get(policyUri);
                if (StringUtils.isNotBlank(policyName)) {
                    final String componentValue;
                    if (componentVersionStatus.componentVersion != null) {
                        componentValue = null;
                    } else {
                        componentValue = componentVersionStatus.component;
                    }
                    final NotificationContentDetail detail = NotificationContentDetail.createDetail(notificationGroup, Optional.of(content.projectName), Optional.of(content.projectVersionName), Optional.of(content.projectVersion),
                        Optional.of(componentVersionStatus.componentName), Optional.ofNullable(componentValue), Optional.ofNullable(componentVersionStatus.componentVersionName),
                        Optional.ofNullable(componentVersionStatus.componentVersion),
                        Optional.of(policyName), Optional.of(policyUri), Optional.empty(), Optional.ofNullable(componentVersionStatus.componentIssueLink), Optional.empty(), Optional.ofNullable(componentVersionStatus.bomComponent));
                    notificationContentDetails.add(detail);
                }
            }
        }
    }

    public void populateContentDetails(final List<NotificationContentDetail> notificationContentDetails, final String notificationGroup, final RuleViolationClearedNotificationContent content) {
        final Map<String, String> uriToName = content.policyInfos.stream().collect(Collectors.toMap(policyInfo -> policyInfo.policy, policyInfo -> policyInfo.policyName));
        for (final ComponentVersionStatus componentVersionStatus : content.componentVersionStatuses) {
            for (final String policyUri : componentVersionStatus.policies) {
                final String policyName = uriToName.get(policyUri);
                if (StringUtils.isNotBlank(policyName)) {
                    final String componentValue;
                    if (componentVersionStatus.componentVersion != null) {
                        componentValue = null;
                    } else {
                        componentValue = componentVersionStatus.component;
                    }
                    final NotificationContentDetail detail = NotificationContentDetail.createDetail(notificationGroup, Optional.of(content.projectName), Optional.of(content.projectVersionName), Optional.of(content.projectVersion),
                        Optional.of(componentVersionStatus.componentName), Optional.ofNullable(componentValue), Optional.ofNullable(componentVersionStatus.componentVersionName),
                        Optional.ofNullable(componentVersionStatus.componentVersion),
                        Optional.of(policyName), Optional.of(policyUri), Optional.empty(), Optional.of(componentVersionStatus.componentIssueLink), Optional.empty(), Optional.ofNullable(componentVersionStatus.bomComponent));
                    notificationContentDetails.add(detail);
                }
            }
        }
    }

    public void populateContentDetails(final List<NotificationContentDetail> notificationContentDetails, final String notificationGroup, final VulnerabilityNotificationContent content) {
        for (final AffectedProjectVersion projectVersion : content.affectedProjectVersions) {
            final NotificationContentDetail detail = NotificationContentDetail
                                                         .createDetail(notificationGroup, Optional.of(projectVersion.getProjectName()), Optional.of(projectVersion.getProjectVersionName()),
                                                             Optional.of(projectVersion.getProjectVersion()),
                                                             Optional.of(content.componentName), Optional.empty(), Optional.of(content.versionName), Optional.of(content.componentVersion), Optional.empty(), Optional.empty(),
                                                             Optional.ofNullable(content.componentVersionOriginName), Optional.ofNullable(projectVersion.getComponentIssueUrl()), Optional.ofNullable(content.componentVersionOriginId),
                                                             Optional.ofNullable(projectVersion.getBomComponent()));
            notificationContentDetails.add(detail);
        }
    }

    private void populateContentDetails(final List<NotificationContentDetail> notificationContentDetails, final String notificationGroup, final BomEditContent notificationContent) {
        final NotificationContentDetail detail = NotificationContentDetail
                                                     .createDetail(notificationGroup, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                                                         Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.ofNullable(notificationContent.bomComponent));
        notificationContentDetails.add(detail);
    }

}
