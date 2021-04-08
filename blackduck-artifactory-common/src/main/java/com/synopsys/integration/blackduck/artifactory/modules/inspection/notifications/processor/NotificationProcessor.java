/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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
