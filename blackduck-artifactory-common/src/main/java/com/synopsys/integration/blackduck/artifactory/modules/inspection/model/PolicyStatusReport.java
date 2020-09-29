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

import org.apache.commons.lang3.EnumUtils;

import com.synopsys.integration.blackduck.api.enumeration.PolicySeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomPolicyRuleView;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.exception.IntegrationException;

public class PolicyStatusReport {
    private final PolicySummaryStatusType policySummaryStatusType;
    private final List<PolicySeverityType> policySeverityTypes;

    public static PolicyStatusReport fromVersionBomComponentView(VersionBomComponentView versionBomComponentView, BlackDuckService blackDuckService) throws IntegrationException {
        PolicySummaryStatusType policySummaryStatusType = versionBomComponentView.getPolicyStatus();

        List<VersionBomPolicyRuleView> versionBomPolicyRuleViews = blackDuckService.getResponses(versionBomComponentView, VersionBomComponentView.POLICY_RULES_LINK_RESPONSE, true);
        List<PolicySeverityType> policySeverityTypes = versionBomPolicyRuleViews.stream()
                                                                 .map(VersionBomPolicyRuleView::getSeverity)
                                                                 .map(severity -> EnumUtils.getEnumIgnoreCase(PolicySeverityType.class, severity))
                                                                 .collect(Collectors.toList());

        return new PolicyStatusReport(policySummaryStatusType, policySeverityTypes);
    }

    public PolicyStatusReport(PolicySummaryStatusType policySummaryStatusType, List<PolicySeverityType> policySeverityTypes) {
        this.policySummaryStatusType = policySummaryStatusType;
        this.policySeverityTypes = policySeverityTypes;
    }

    public PolicySummaryStatusType getPolicySummaryStatusType() {
        return policySummaryStatusType;
    }

    public List<PolicySeverityType> getPolicySeverityTypes() {
        return policySeverityTypes;
    }
}
