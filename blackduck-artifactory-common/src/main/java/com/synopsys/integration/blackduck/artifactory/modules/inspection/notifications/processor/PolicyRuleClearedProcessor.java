/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyStatusType;
import com.synopsys.integration.blackduck.api.manual.component.ComponentVersionStatus;
import com.synopsys.integration.blackduck.api.manual.component.RuleViolationClearedNotificationContent;
import com.synopsys.integration.blackduck.api.manual.view.RuleViolationClearedNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.PolicyNotificationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.RepositoryProjectNameLookup;
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
                PolicyStatusType policySummaryStatusType = policyNotificationService.fetchApprovalStatus(componentVersionStatus.getBomComponentVersionPolicyStatus());
                PolicyStatusReport policyStatusReport = new PolicyStatusReport(policySummaryStatusType, policySeverityTypes);

                String componentName = componentVersionStatus.getComponentName();
                String componentVersionName = componentVersionStatus.getComponentVersionName();
                ProcessedPolicyNotification processedNotification = new ProcessedPolicyNotification(componentName, componentVersionName, policyStatusReport, Collections.singletonList(repoKeyPath.get()));
                processedPolicyNotifications.add(processedNotification);
            }
        }

        return processedPolicyNotifications;
    }
}
