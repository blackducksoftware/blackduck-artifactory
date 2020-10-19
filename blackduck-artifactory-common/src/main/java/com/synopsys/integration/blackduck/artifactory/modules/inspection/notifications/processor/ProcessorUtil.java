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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.blackduck.api.UriSingleResponse;
import com.synopsys.integration.blackduck.api.enumeration.PolicySeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.api.generated.view.PolicyStatusView;
import com.synopsys.integration.blackduck.api.manual.component.ComponentVersionStatus;
import com.synopsys.integration.blackduck.api.manual.component.PolicyInfo;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.exception.IntegrationException;

public class ProcessorUtil {
    public static List<PolicySeverityType> convertPolicyInfo(List<PolicyInfo> policyInfos) {
        return policyInfos.stream()
                   .map(PolicyInfo::getSeverity)
                   .map(severity -> {
                       if (StringUtils.isBlank(severity)) {
                           return PolicySeverityType.UNSPECIFIED;
                       } else {
                           return PolicySeverityType.valueOf(severity);
                       }
                   })
                   .collect(Collectors.toList());
    }

    public static PolicySummaryStatusType fetchApprovalStatus(BlackDuckService blackDuckService, ComponentVersionStatus componentVersionStatus) throws IntegrationException {
        return fetchApprovalStatus(blackDuckService, componentVersionStatus.getBomComponentVersionPolicyStatus());
    }

    public static PolicySummaryStatusType fetchApprovalStatus(BlackDuckService blackDuckService, String bomComponentVersionPolicyStatus) throws IntegrationException {
        UriSingleResponse<PolicyStatusView> policyStatusViewUriSingleResponse = new UriSingleResponse<>(bomComponentVersionPolicyStatus, PolicyStatusView.class);
        PolicyStatusView policyStatus = blackDuckService.getResponse(policyStatusViewUriSingleResponse);
        return policyStatus.getApprovalStatus();
    }
}
