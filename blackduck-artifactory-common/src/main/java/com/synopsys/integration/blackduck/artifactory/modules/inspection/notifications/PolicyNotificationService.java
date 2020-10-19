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
