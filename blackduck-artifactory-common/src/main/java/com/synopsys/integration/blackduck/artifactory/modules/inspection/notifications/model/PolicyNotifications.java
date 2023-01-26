/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model;

import java.util.ArrayList;
import java.util.List;

import com.synopsys.integration.blackduck.api.manual.view.NotificationUserView;
import com.synopsys.integration.blackduck.api.manual.view.PolicyOverrideNotificationUserView;
import com.synopsys.integration.blackduck.api.manual.view.RuleViolationClearedNotificationUserView;
import com.synopsys.integration.blackduck.api.manual.view.RuleViolationNotificationUserView;

// TODO: Maybe remove and run separate.
public class PolicyNotifications {
    private final List<RuleViolationNotificationUserView> ruleViolationNotificationUserViews;
    private final List<RuleViolationClearedNotificationUserView> ruleViolationClearedNotificationUserViews;
    private final List<PolicyOverrideNotificationUserView> policyOverrideNotificationUserViews;

    public PolicyNotifications(List<RuleViolationNotificationUserView> ruleViolationUserViews, List<RuleViolationClearedNotificationUserView> ruleViolationClearedUserViews, List<PolicyOverrideNotificationUserView> policyOverrideUserViews) {
        this.ruleViolationNotificationUserViews = ruleViolationUserViews;
        this.ruleViolationClearedNotificationUserViews = ruleViolationClearedUserViews;
        this.policyOverrideNotificationUserViews = policyOverrideUserViews;
    }

    public List<RuleViolationNotificationUserView> getRuleViolationNotificationUserViews() {
        return ruleViolationNotificationUserViews;
    }

    public List<RuleViolationClearedNotificationUserView> getRuleViolationClearedNotificationUserViews() {
        return ruleViolationClearedNotificationUserViews;
    }

    public List<PolicyOverrideNotificationUserView> getPolicyOverrideNotificationUserViews() {
        return policyOverrideNotificationUserViews;
    }

    public List<NotificationUserView> getAllNotificationUserViews() {
        List<NotificationUserView> allNotificationUserViews = new ArrayList<>();
        allNotificationUserViews.addAll(ruleViolationNotificationUserViews);
        allNotificationUserViews.addAll(ruleViolationClearedNotificationUserViews);
        allNotificationUserViews.addAll(policyOverrideNotificationUserViews);
        return allNotificationUserViews;
    }
}
