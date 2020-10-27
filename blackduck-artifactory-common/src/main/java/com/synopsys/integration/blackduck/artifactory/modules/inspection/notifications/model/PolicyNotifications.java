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
