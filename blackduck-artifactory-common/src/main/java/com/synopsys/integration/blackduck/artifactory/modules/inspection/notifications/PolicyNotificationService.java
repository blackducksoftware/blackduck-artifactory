/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.synopsys.integration.blackduck.api.generated.discovery.ApiDiscovery;
import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionComponentPolicyStatusType;
import com.synopsys.integration.blackduck.api.generated.view.ComponentPolicyStatusView;
import com.synopsys.integration.blackduck.api.generated.view.UserView;
import com.synopsys.integration.blackduck.api.manual.enumeration.NotificationType;
import com.synopsys.integration.blackduck.api.manual.view.NotificationUserView;
import com.synopsys.integration.blackduck.api.manual.view.PolicyOverrideNotificationUserView;
import com.synopsys.integration.blackduck.api.manual.view.RuleViolationClearedNotificationUserView;
import com.synopsys.integration.blackduck.api.manual.view.RuleViolationNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.PolicyNotifications;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.blackduck.service.dataservice.NotificationService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;

public class PolicyNotificationService {
    private final BlackDuckApiClient blackDuckApiClient;
    private final NotificationService notificationService;

    public PolicyNotificationService(BlackDuckApiClient blackDuckApiClient, NotificationService notificationService) {
        this.blackDuckApiClient = blackDuckApiClient;
        this.notificationService = notificationService;
    }

    public PolicyNotifications fetchPolicyNotifications(Date startDate, Date endDate) throws IntegrationException {
        UserView currentUser = blackDuckApiClient.getResponse(ApiDiscovery.CURRENT_USER_LINK_RESPONSE);

        List<RuleViolationNotificationUserView> ruleViolationNotifications =
            fetchNotificationsOfType(currentUser, startDate, endDate, NotificationType.RULE_VIOLATION)
                .stream()
                .filter(RuleViolationNotificationUserView.class::isInstance)
                .map(RuleViolationNotificationUserView.class::cast)
                .collect(Collectors.toList());

        List<RuleViolationClearedNotificationUserView> ruleViolationClearedNotifications =
            fetchNotificationsOfType(currentUser, startDate, endDate, NotificationType.RULE_VIOLATION_CLEARED)
                .stream()
                .filter(RuleViolationClearedNotificationUserView.class::isInstance)
                .map(RuleViolationClearedNotificationUserView.class::cast)
                .collect(Collectors.toList());

        List<PolicyOverrideNotificationUserView> policyOverrideNotifications =
            fetchNotificationsOfType(currentUser, startDate, endDate, NotificationType.POLICY_OVERRIDE)
                .stream()
                .filter(PolicyOverrideNotificationUserView.class::isInstance)
                .map(PolicyOverrideNotificationUserView.class::cast)
                .collect(Collectors.toList());

        return new PolicyNotifications(ruleViolationNotifications, ruleViolationClearedNotifications, policyOverrideNotifications);
    }

    private List<NotificationUserView> fetchNotificationsOfType(UserView currentUser, Date startDate, Date endDate, NotificationType notificationType) throws IntegrationException {
        return notificationService.getFilteredUserNotifications(currentUser, startDate, endDate, Collections.singletonList(notificationType.toString()));
    }

    public ProjectVersionComponentPolicyStatusType fetchApprovalStatus(String bomComponentVersionPolicyStatus) throws IntegrationException {
        HttpUrl componentPolicyStatusViewUrl = new HttpUrl(bomComponentVersionPolicyStatus);
        ComponentPolicyStatusView policyStatus = blackDuckApiClient.getResponse(componentPolicyStatusViewUrl, ComponentPolicyStatusView.class);
        return policyStatus.getApprovalStatus();
    }
}
