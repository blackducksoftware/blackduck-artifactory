package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.api.enumeration.PolicySeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.api.manual.component.PolicyInfo;
import com.synopsys.integration.blackduck.api.manual.component.PolicyOverrideNotificationContent;
import com.synopsys.integration.blackduck.api.manual.view.PolicyOverrideNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.PolicyNotificationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.RepositoryProjectNameLookup;
import com.synopsys.integration.exception.IntegrationException;

class PolicyOverrideProcessorTest {
    @Test
    void processPolicyOverrideNotifications() throws IntegrationException {
        PolicyNotificationService policyNotificationService = Mockito.mock(PolicyNotificationService.class);
        Mockito.when(policyNotificationService.fetchApprovalStatus(Mockito.any())).thenReturn(PolicySummaryStatusType.IN_VIOLATION);

        RepositoryProjectNameLookup repositoryFilter = Mockito.mock(RepositoryProjectNameLookup.class);
        RepoPath repoPath = new PluginRepoPathFactory(false).create("repo-1");
        Mockito.when(repositoryFilter.getRepoKeyPath(Mockito.any(), Mockito.any())).thenReturn(Optional.of(repoPath));

        PolicyOverrideProcessor policyOverrideProcessor = new PolicyOverrideProcessor(policyNotificationService);

        PolicyOverrideNotificationUserView notificationUserView = new PolicyOverrideNotificationUserView();
        PolicyOverrideNotificationContent content = new PolicyOverrideNotificationContent();
        content.setProjectName("project-name");
        content.setProjectVersionName("project-version-name");
        content.setPolicyInfos(Collections.singletonList(new PolicyInfo()));
        content.setComponentName("component-name");
        content.setComponentVersionName("component-version-name-1.0");
        content.setBomComponentVersionPolicyStatus("https://synopsys.com/bomComponentVersionPolicyStatus");
        notificationUserView.setContent(content);

        List<ProcessedPolicyNotification> processedPolicyNotifications = policyOverrideProcessor.processPolicyOverrideNotifications(Collections.singletonList(notificationUserView), repositoryFilter);

        Assertions.assertEquals(1, processedPolicyNotifications.size());

        ProcessedPolicyNotification processedPolicyNotification = processedPolicyNotifications.get(0);

        Assertions.assertEquals("component-name", processedPolicyNotification.getComponentName());
        Assertions.assertEquals("component-version-name-1.0", processedPolicyNotification.getComponentVersionName());
        Assertions.assertEquals(Collections.singletonList(repoPath), processedPolicyNotification.getAffectedRepoKeyPaths());
        Assertions.assertEquals(Collections.singletonList(PolicySeverityType.UNSPECIFIED), processedPolicyNotification.getPolicyStatusReport().getPolicySeverityTypes());
        Assertions.assertEquals(PolicySummaryStatusType.IN_VIOLATION, processedPolicyNotification.getPolicyStatusReport().getPolicySummaryStatusType());
    }
}