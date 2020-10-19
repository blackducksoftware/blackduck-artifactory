package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.api.enumeration.PolicySeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.api.manual.component.PolicyOverrideNotificationContent;
import com.synopsys.integration.blackduck.api.manual.view.PolicyOverrideNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.NotificationRepositoryFilter;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.exception.IntegrationException;

public class PolicyOverrideProcessor {
    private final BlackDuckService blackDuckService;

    public PolicyOverrideProcessor(BlackDuckService blackDuckService) {
        this.blackDuckService = blackDuckService;
    }

    public List<ProcessedPolicyNotification> processPolicyOverrideNotifications(List<PolicyOverrideNotificationUserView> notificationUserViews, NotificationRepositoryFilter repositoryFilter) throws IntegrationException {
        List<ProcessedPolicyNotification> processedPolicyNotifications = new ArrayList<>();
        for (PolicyOverrideNotificationUserView notificationUserView : notificationUserViews) {
            processPolicyOverrideNotification(notificationUserView, repositoryFilter)
                .ifPresent(processedPolicyNotifications::add);
        }
        return processedPolicyNotifications;
    }

    public Optional<ProcessedPolicyNotification> processPolicyOverrideNotification(PolicyOverrideNotificationUserView notificationUserView, NotificationRepositoryFilter repositoryFilter) throws IntegrationException {
        ProcessedPolicyNotification processedPolicyNotification = null;
        PolicyOverrideNotificationContent content = notificationUserView.getContent();

        Optional<RepoPath> repoKeyPath = repositoryFilter.getRepoKeyPath(content.getProjectName(), content.getProjectVersionName());
        if (repoKeyPath.isPresent()) {
            List<PolicySeverityType> policySeverityTypes = ProcessorUtil.convertPolicyInfo(content.getPolicyInfos());
            PolicySummaryStatusType policySummaryStatusType = ProcessorUtil.fetchApprovalStatus(blackDuckService, content.getBomComponentVersionPolicyStatus());
            PolicyStatusReport policyStatusReport = new PolicyStatusReport(policySummaryStatusType, policySeverityTypes);

            processedPolicyNotification = new ProcessedPolicyNotification(content.getComponentName(), content.getComponentVersionName(), policyStatusReport, Collections.singletonList(repoKeyPath.get()));
        }

        return Optional.ofNullable(processedPolicyNotification);
    }

}
