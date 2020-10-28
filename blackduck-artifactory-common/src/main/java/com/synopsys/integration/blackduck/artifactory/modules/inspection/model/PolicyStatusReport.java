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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.model;

import java.util.List;
import java.util.stream.Collectors;

import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyStatusType;
import com.synopsys.integration.blackduck.api.generated.view.ComponentPolicyRulesView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionComponentView;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.exception.IntegrationException;

public class PolicyStatusReport {
    private final PolicyStatusType policyStatusType;
    private final List<PolicyRuleSeverityType> policyRuleSeverityTypes;

    public static PolicyStatusReport fromVersionBomComponentView(ProjectVersionComponentView versionBomComponentView, BlackDuckApiClient blackDuckApiClient) throws IntegrationException {
        PolicyStatusType policySummaryStatusType = versionBomComponentView.getPolicyStatus();

        List<ComponentPolicyRulesView> versionBomPolicyRuleViews = blackDuckApiClient.getAllResponses(versionBomComponentView, ProjectVersionComponentView.POLICY_RULES_LINK_RESPONSE);
        List<PolicyRuleSeverityType> policySeverityTypes = versionBomPolicyRuleViews.stream()
                                                               .map(ComponentPolicyRulesView::getSeverity)
                                                               .collect(Collectors.toList());

        return new PolicyStatusReport(policySummaryStatusType, policySeverityTypes);
    }

    public PolicyStatusReport(PolicyStatusType policyStatusType, List<PolicyRuleSeverityType> policyRuleSeverityTypes) {
        this.policyStatusType = policyStatusType;
        this.policyRuleSeverityTypes = policyRuleSeverityTypes;
    }

    public PolicyStatusType getPolicyStatusType() {
        return policyStatusType;
    }

    public List<PolicyRuleSeverityType> getPolicyRuleSeverityTypes() {
        return policyRuleSeverityTypes;
    }
}
