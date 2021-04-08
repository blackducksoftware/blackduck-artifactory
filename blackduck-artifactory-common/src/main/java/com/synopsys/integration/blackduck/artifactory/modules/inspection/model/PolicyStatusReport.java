/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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
