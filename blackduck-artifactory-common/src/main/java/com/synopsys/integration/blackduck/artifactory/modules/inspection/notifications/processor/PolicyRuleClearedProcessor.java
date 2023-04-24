/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionComponentPolicyStatusType;
import com.synopsys.integration.blackduck.api.manual.component.ComponentVersionStatus;
import com.synopsys.integration.blackduck.api.manual.component.RuleViolationClearedNotificationContent;
import com.synopsys.integration.blackduck.api.manual.view.RuleViolationClearedNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.PolicyNotificationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.RepositoryProjectNameLookup;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.deleteme.ComponentVersionIdUtil;
import com.synopsys.integration.exception.IntegrationException;

public class PolicyRuleClearedProcessor {
    private final PolicyNotificationService policyNotificationService;

    public PolicyRuleClearedProcessor(PolicyNotificationService policyNotificationService) {
        this.policyNotificationService = policyNotificationService;
    }

    public List<ProcessedPolicyNotification> processPolicyRuleClearedNotifications(List<RuleViolationClearedNotificationUserView> notificationUserViews, RepositoryProjectNameLookup repositoryFilter) throws IntegrationException {
        List<ProcessedPolicyNotification> processedPolicyNotifications = new ArrayList<>();
        for (RuleViolationClearedNotificationUserView notificationUserView : notificationUserViews) {
            processedPolicyNotifications.addAll(processPolicyRuleClearedNotification(notificationUserView, repositoryFilter));
        }
        return processedPolicyNotifications;
    }

    private List<ProcessedPolicyNotification> processPolicyRuleClearedNotification(RuleViolationClearedNotificationUserView notificationUserView, RepositoryProjectNameLookup repositoryFilter) throws IntegrationException {
        List<ProcessedPolicyNotification> processedPolicyNotifications = new ArrayList<>();
        RuleViolationClearedNotificationContent content = notificationUserView.getContent();

        Optional<RepoPath> repoKeyPath = repositoryFilter.getRepoKeyPath(content.getProjectName(), content.getProjectVersionName());
        if (repoKeyPath.isPresent()) {
            List<PolicyRuleSeverityType> policySeverityTypes = ProcessorUtil.convertPolicyInfo(content.getPolicyInfos());

            List<ComponentVersionStatus> componentVersionStatuses = content.getComponentVersionStatuses();

            for (ComponentVersionStatus componentVersionStatus : componentVersionStatuses) {
                ProjectVersionComponentPolicyStatusType policySummaryStatusType = policyNotificationService.fetchApprovalStatus(componentVersionStatus.getBomComponentVersionPolicyStatus());
                PolicyStatusReport policyStatusReport = new PolicyStatusReport(policySummaryStatusType, policySeverityTypes);

                String componentName = componentVersionStatus.getComponentName();
                String componentVersionName = componentVersionStatus.getComponentVersionName();
                String componentVersionUrl = componentVersionStatus.getComponentVersion();
                String componentVersionId = ComponentVersionIdUtil.extractComponentVersionId(componentVersionUrl);
                ProcessedPolicyNotification processedNotification = new ProcessedPolicyNotification(componentName, componentVersionName, componentVersionId, policyStatusReport, Collections.singletonList(repoKeyPath.get()));
                processedPolicyNotifications.add(processedNotification);
            }
        }

        return processedPolicyNotifications;
    }
}
