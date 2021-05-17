/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package modules.inspection.notifications.processor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionComponentPolicyStatusType;
import com.synopsys.integration.blackduck.api.manual.component.ComponentVersionStatus;
import com.synopsys.integration.blackduck.api.manual.component.PolicyInfo;
import com.synopsys.integration.blackduck.api.manual.component.RuleViolationClearedNotificationContent;
import com.synopsys.integration.blackduck.api.manual.view.RuleViolationClearedNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.PolicyNotificationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.RepositoryProjectNameLookup;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.PolicyRuleClearedProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.ProcessedPolicyNotification;
import com.synopsys.integration.exception.IntegrationException;

class PolicyRuleClearedProcessorTest {
    @Test
    void processPolicyRuleClearedNotifications() throws IntegrationException {
        PolicyNotificationService policyNotificationService = Mockito.mock(PolicyNotificationService.class);
        Mockito.when(policyNotificationService.fetchApprovalStatus(Mockito.any())).thenReturn(ProjectVersionComponentPolicyStatusType.NOT_IN_VIOLATION);

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
        componentVersionStatus.setComponentVersion("https://synopsys.com/api/components/08f3bea3-fbfb-4f01-97dd-3f49419f3ea9/versions/e7142eee-d1a2-4b8e-ba87-01f84ac82b1f");
        content.setComponentVersionStatuses(Collections.singletonList(componentVersionStatus));
        notificationUserView.setContent(content);

        List<ProcessedPolicyNotification> processedPolicyNotifications = policyRuleClearedProcessor.processPolicyRuleClearedNotifications(Collections.singletonList(notificationUserView), repositoryFilter);

        Assertions.assertEquals(1, processedPolicyNotifications.size());

        ProcessedPolicyNotification processedPolicyNotification = processedPolicyNotifications.get(0);

        Assertions.assertEquals("component-name", processedPolicyNotification.getComponentName());
        Assertions.assertEquals("component-version-name-1.0", processedPolicyNotification.getComponentVersionName());
        Assertions.assertEquals("e7142eee-d1a2-4b8e-ba87-01f84ac82b1f", processedPolicyNotification.getComponentVersionId());
        Assertions.assertEquals(Collections.singletonList(repoPath), processedPolicyNotification.getAffectedRepoKeyPaths());
        Assertions.assertEquals(Collections.singletonList(PolicyRuleSeverityType.UNSPECIFIED), processedPolicyNotification.getPolicyStatusReport().getPolicyRuleSeverityTypes());
        Assertions.assertEquals(ProjectVersionComponentPolicyStatusType.NOT_IN_VIOLATION, processedPolicyNotification.getPolicyStatusReport().getPolicyStatusType());
    }
}
