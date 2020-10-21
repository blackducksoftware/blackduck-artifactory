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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.api.enumeration.PolicySeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.api.manual.component.PolicyOverrideNotificationContent;
import com.synopsys.integration.blackduck.api.manual.view.PolicyOverrideNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.RepositoryProjectNameLookup;
import com.synopsys.integration.exception.IntegrationException;

public class PolicyOverrideProcessor {
    private final ProcessorUtil processorUtil;

    public PolicyOverrideProcessor(ProcessorUtil processorUtil) {
        this.processorUtil = processorUtil;
    }

    public List<ProcessedPolicyNotification> processPolicyOverrideNotifications(List<PolicyOverrideNotificationUserView> notificationUserViews, RepositoryProjectNameLookup repositoryFilter) throws IntegrationException {
        List<ProcessedPolicyNotification> processedPolicyNotifications = new ArrayList<>();
        for (PolicyOverrideNotificationUserView notificationUserView : notificationUserViews) {
            processPolicyOverrideNotification(notificationUserView, repositoryFilter)
                .ifPresent(processedPolicyNotifications::add);
        }
        return processedPolicyNotifications;
    }

    private Optional<ProcessedPolicyNotification> processPolicyOverrideNotification(PolicyOverrideNotificationUserView notificationUserView, RepositoryProjectNameLookup repositoryFilter) throws IntegrationException {
        ProcessedPolicyNotification processedPolicyNotification = null;
        PolicyOverrideNotificationContent content = notificationUserView.getContent();

        Optional<RepoPath> repoKeyPath = repositoryFilter.getRepoKeyPath(content.getProjectName(), content.getProjectVersionName());
        if (repoKeyPath.isPresent()) {
            List<PolicySeverityType> policySeverityTypes = ProcessorUtil.convertPolicyInfo(content.getPolicyInfos());
            PolicySummaryStatusType policySummaryStatusType = processorUtil.fetchApprovalStatus(content.getBomComponentVersionPolicyStatus());
            PolicyStatusReport policyStatusReport = new PolicyStatusReport(policySummaryStatusType, policySeverityTypes);

            processedPolicyNotification = new ProcessedPolicyNotification(content.getComponentName(), content.getComponentVersionName(), policyStatusReport, Collections.singletonList(repoKeyPath.get()));
        }

        return Optional.ofNullable(processedPolicyNotification);
    }

}
