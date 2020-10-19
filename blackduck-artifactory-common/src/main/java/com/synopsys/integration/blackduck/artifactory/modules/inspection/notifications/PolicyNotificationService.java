package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.synopsys.integration.blackduck.api.generated.discovery.ApiDiscovery;
import com.synopsys.integration.blackduck.api.generated.enumeration.NotificationType;
import com.synopsys.integration.blackduck.api.generated.view.UserView;
import com.synopsys.integration.blackduck.api.manual.view.NotificationUserView;
import com.synopsys.integration.blackduck.api.manual.view.PolicyOverrideNotificationUserView;
import com.synopsys.integration.blackduck.api.manual.view.RuleViolationClearedNotificationUserView;
import com.synopsys.integration.blackduck.api.manual.view.RuleViolationNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.PolicyNotifications;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.NotificationService;
import com.synopsys.integration.exception.IntegrationException;

public class PolicyNotificationService {

    private final BlackDuckService blackDuckService;
    private final NotificationService notificationService;

    public PolicyNotificationService(BlackDuckService blackDuckService, NotificationService notificationService) {
        this.blackDuckService = blackDuckService;
        this.notificationService = notificationService;
    }

    public PolicyNotifications fetchPolicyNotifications(Date startDate, Date endDate) throws IntegrationException {
        UserView currentUser = blackDuckService.getResponse(ApiDiscovery.CURRENT_USER_LINK_RESPONSE);

        List<RuleViolationNotificationUserView> ruleViolationNotifications = fetchNotificationsOfType(currentUser, startDate, endDate, NotificationType.RULE_VIOLATION).stream()
                                                                                 .filter(RuleViolationNotificationUserView.class::isInstance)
                                                                                 .map(RuleViolationNotificationUserView.class::cast)
                                                                                 .collect(Collectors.toList());

        List<RuleViolationClearedNotificationUserView> ruleViolationClearedNotifications = fetchNotificationsOfType(currentUser, startDate, endDate, NotificationType.RULE_VIOLATION_CLEARED).stream()
                                                                                               .filter(RuleViolationClearedNotificationUserView.class::isInstance)
                                                                                               .map(RuleViolationClearedNotificationUserView.class::cast)
                                                                                               .collect(Collectors.toList());

        List<PolicyOverrideNotificationUserView> policyOverrideNotifications = fetchNotificationsOfType(currentUser, startDate, endDate, NotificationType.POLICY_OVERRIDE).stream()
                                                                                   .filter(PolicyOverrideNotificationUserView.class::isInstance)
                                                                                   .map(PolicyOverrideNotificationUserView.class::cast)
                                                                                   .collect(Collectors.toList());

        return new PolicyNotifications(ruleViolationNotifications, ruleViolationClearedNotifications, policyOverrideNotifications);
    }

    private List<NotificationUserView> fetchNotificationsOfType(UserView currentUser, Date startDate, Date endDate, NotificationType notificationType) throws IntegrationException {
        return notificationService.getFilteredUserNotifications(currentUser, startDate, endDate, Collections.singletonList(notificationType.toString()));
    }
}
