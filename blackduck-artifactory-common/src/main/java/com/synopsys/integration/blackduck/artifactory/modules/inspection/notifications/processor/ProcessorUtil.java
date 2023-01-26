/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.synopsys.integration.blackduck.api.manual.component.PolicyInfo;

public class ProcessorUtil {
    private ProcessorUtil() {
        // Hiding constructor
    }

    public static List<PolicyRuleSeverityType> convertPolicyInfo(List<PolicyInfo> policyInfos) {
        return policyInfos.stream()
                   .map(PolicyInfo::getSeverity)
                   .map(severity -> {
                       if (StringUtils.isBlank(severity)) {
                           return PolicyRuleSeverityType.UNSPECIFIED;
                       } else {
                           return PolicyRuleSeverityType.valueOf(severity);
                       }
                   })
                   .collect(Collectors.toList());
    }
}
