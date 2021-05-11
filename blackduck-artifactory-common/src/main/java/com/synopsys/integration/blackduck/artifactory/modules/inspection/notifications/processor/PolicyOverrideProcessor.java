/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
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
import com.synopsys.integration.blackduck.api.manual.component.PolicyOverrideNotificationContent;
import com.synopsys.integration.blackduck.api.manual.view.PolicyOverrideNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.PolicyNotificationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.RepositoryProjectNameLookup;
import com.synopsys.integration.exception.IntegrationException;

public class PolicyOverrideProcessor {
    private final PolicyNotificationService policyNotificationService;

    public PolicyOverrideProcessor(PolicyNotificationService policyNotificationService) {
        this.policyNotificationService = policyNotificationService;
    }

    public List<ProcessedPolicyNotification> processPolicyOverrideNotifications(List<PolicyOverrideNotificationUserView> notificationUserViews, RepositoryProjectNameLookup repositoryProjectNameLookup) throws IntegrationException {
        List<ProcessedPolicyNotification> processedPolicyNotifications = new ArrayList<>();
        for (PolicyOverrideNotificationUserView notificationUserView : notificationUserViews) {
            processPolicyOverrideNotification(notificationUserView, repositoryProjectNameLookup)
                .ifPresent(processedPolicyNotifications::add);
        }
        return processedPolicyNotifications;
    }

    private Optional<ProcessedPolicyNotification> processPolicyOverrideNotification(PolicyOverrideNotificationUserView notificationUserView, RepositoryProjectNameLookup repositoryProjectNameLookup) throws IntegrationException {
        ProcessedPolicyNotification processedPolicyNotification = null;
        PolicyOverrideNotificationContent content = notificationUserView.getContent();

        Optional<RepoPath> repoKeyPath = repositoryProjectNameLookup.getRepoKeyPath(content.getProjectName(), content.getProjectVersionName());
        if (repoKeyPath.isPresent()) {
            List<PolicyRuleSeverityType> policySeverityTypes = ProcessorUtil.convertPolicyInfo(content.getPolicyInfos());
            ProjectVersionComponentPolicyStatusType policySummaryStatusType = policyNotificationService.fetchApprovalStatus(content.getBomComponentVersionPolicyStatus());
            PolicyStatusReport policyStatusReport = new PolicyStatusReport(policySummaryStatusType, policySeverityTypes);

            processedPolicyNotification = new ProcessedPolicyNotification(content.getComponentName(), content.getComponentVersionName(), policyStatusReport, Collections.singletonList(repoKeyPath.get()));
        }

        return Optional.ofNullable(processedPolicyNotification);
    }

}
