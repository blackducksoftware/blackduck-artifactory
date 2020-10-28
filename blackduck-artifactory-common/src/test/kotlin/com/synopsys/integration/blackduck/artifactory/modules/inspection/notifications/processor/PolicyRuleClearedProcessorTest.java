package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyStatusType;
import com.synopsys.integration.blackduck.api.manual.component.ComponentVersionStatus;
import com.synopsys.integration.blackduck.api.manual.component.PolicyInfo;
import com.synopsys.integration.blackduck.api.manual.component.RuleViolationClearedNotificationContent;
import com.synopsys.integration.blackduck.api.manual.view.RuleViolationClearedNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.PolicyNotificationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.RepositoryProjectNameLookup;
import com.synopsys.integration.exception.IntegrationException;

class PolicyRuleClearedProcessorTest {
    @Test
    void processPolicyRuleClearedNotifications() throws IntegrationException {
        PolicyNotificationService policyNotificationService = Mockito.mock(PolicyNotificationService.class);
        Mockito.when(policyNotificationService.fetchApprovalStatus(Mockito.any())).thenReturn(PolicyStatusType.NOT_IN_VIOLATION);

        RepositoryProjectNameLookup repositoryFilter = Mockito.mock(RepositoryProjectNameLookup.class);
        RepoPath repoPath = new PluginRepoPathFactory(false).create("repo-1");
        Mockito.when(repositoryFilter.getRepoKeyPath(Mockito.any(), Mockito.any())).thenReturn(Optional.of(repoPath));

        PolicyRuleClearedProcessor policyRuleClearedProcessor = new PolicyRuleClearedProcessor(policyNotificationService);

        RuleViolationClearedNotificationUserView notificationUserView = new RuleViolationClearedNotificationUserView();
        RuleViolationClearedNotificationContent content = new RuleViolationClearedNotificationContent();
        content.setProjectName("project-name");
        content.setProjectVersionName("project-version-name");
        content.setPolicyInfos(Collections.singletonList(new PolicyInfo()));
        ComponentVersionStatus componentVersionStatus = new ComponentVersionStatus();
        componentVersionStatus.setComponentName("component-name");
        componentVersionStatus.setComponentVersionName("component-version-name-1.0");
        componentVersionStatus.setBomComponentVersionPolicyStatus("https://synopsys.com/bomComponentVersionPolicyStatus");
        content.setComponentVersionStatuses(Collections.singletonList(componentVersionStatus));
        notificationUserView.setContent(content);

        List<ProcessedPolicyNotification> processedPolicyNotifications = policyRuleClearedProcessor.processPolicyRuleClearedNotifications(Collections.singletonList(notificationUserView), repositoryFilter);

        Assertions.assertEquals(1, processedPolicyNotifications.size());

        ProcessedPolicyNotification processedPolicyNotification = processedPolicyNotifications.get(0);

        Assertions.assertEquals("component-name", processedPolicyNotification.getComponentName());
        Assertions.assertEquals("component-version-name-1.0", processedPolicyNotification.getComponentVersionName());
        Assertions.assertEquals(Collections.singletonList(repoPath), processedPolicyNotification.getAffectedRepoKeyPaths());
        Assertions.assertEquals(Collections.singletonList(PolicyRuleSeverityType.UNSPECIFIED), processedPolicyNotification.getPolicyStatusReport().getPolicyRuleSeverityTypes());
        Assertions.assertEquals(PolicyStatusType.NOT_IN_VIOLATION, processedPolicyNotification.getPolicyStatusReport().getPolicyStatusType());
    }
}