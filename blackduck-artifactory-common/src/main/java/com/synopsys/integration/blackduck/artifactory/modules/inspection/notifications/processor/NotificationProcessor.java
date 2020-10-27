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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor;

import java.util.ArrayList;
import java.util.List;

import com.synopsys.integration.blackduck.api.manual.view.VulnerabilityNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.RepositoryProjectNameLookup;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.PolicyNotifications;
import com.synopsys.integration.exception.IntegrationException;

public class NotificationProcessor {
    private final PolicyOverrideProcessor policyOverrideProcessor;
    private final PolicyRuleClearedProcessor policyRuleClearedProcessor;
    private final PolicyViolationProcessor policyViolationProcessor;
    private final VulnerabilityProcessor vulnerabilityProcessor;

    public NotificationProcessor(PolicyOverrideProcessor policyOverrideProcessor, PolicyRuleClearedProcessor policyRuleClearedProcessor, PolicyViolationProcessor policyViolationProcessor, VulnerabilityProcessor vulnerabilityProcessor) {
        this.policyOverrideProcessor = policyOverrideProcessor;
        this.policyRuleClearedProcessor = policyRuleClearedProcessor;
        this.policyViolationProcessor = policyViolationProcessor;
        this.vulnerabilityProcessor = vulnerabilityProcessor;
    }

    public List<ProcessedPolicyNotification> processPolicyNotifications(PolicyNotifications policyNotifications, RepositoryProjectNameLookup repositoryProjectNameLookup) throws IntegrationException {
        List<ProcessedPolicyNotification> processedOverrideNotifications = policyOverrideProcessor.processPolicyOverrideNotifications(policyNotifications.getPolicyOverrideNotificationUserViews(), repositoryProjectNameLookup);
        List<ProcessedPolicyNotification> processedRuleClearedNotifications = policyRuleClearedProcessor.processPolicyRuleClearedNotifications(policyNotifications.getRuleViolationClearedNotificationUserViews(), repositoryProjectNameLookup);
        List<ProcessedPolicyNotification> processedViolationNotifications = policyViolationProcessor.processPolicyViolationNotifications(policyNotifications.getRuleViolationNotificationUserViews(), repositoryProjectNameLookup);
        List<ProcessedPolicyNotification> processedPolicyNotifications = new ArrayList<>();
        processedPolicyNotifications.addAll(processedOverrideNotifications);
        processedPolicyNotifications.addAll(processedRuleClearedNotifications);
        processedPolicyNotifications.addAll(processedViolationNotifications);

        return processedPolicyNotifications;
    }

    public List<ProcessedVulnerabilityNotification> processVulnerabilityNotifications(List<VulnerabilityNotificationUserView> notificationUserViews, RepositoryProjectNameLookup repositoryProjectNameLookup) throws IntegrationException {
        return vulnerabilityProcessor.processVulnerabilityNotifications(notificationUserViews, repositoryProjectNameLookup);
    }
}
