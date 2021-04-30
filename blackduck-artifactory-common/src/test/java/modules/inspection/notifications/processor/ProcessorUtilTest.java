/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package modules.inspection.notifications.processor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.synopsys.integration.blackduck.api.manual.component.PolicyInfo;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.ProcessorUtil;

class ProcessorUtilTest {
    @Test
    void convertPolicyInfo() {
        List<PolicyInfo> policyInfos = Arrays.stream(PolicyRuleSeverityType.values())
                                           .map(Enum::toString)
                                           .map(severity -> {
                                               PolicyInfo policyInfo = new PolicyInfo();
                                               policyInfo.setSeverity(severity);
                                               return policyInfo;
                                           })
                                           .collect(Collectors.toList());
        // BlackDuck won't provide a severity of UNSPECIFIED, it will just be null.
        policyInfos.add(new PolicyInfo());

        List<PolicyRuleSeverityType> policySeverityTypes = ProcessorUtil.convertPolicyInfo(policyInfos);

        Assertions.assertEquals(7, policySeverityTypes.size(), "Expected the total number of possible PolicySeverityType + 1.");

        verifyAndRemove(policySeverityTypes, PolicyRuleSeverityType.BLOCKER);
        verifyAndRemove(policySeverityTypes, PolicyRuleSeverityType.CRITICAL);
        verifyAndRemove(policySeverityTypes, PolicyRuleSeverityType.MAJOR);
        verifyAndRemove(policySeverityTypes, PolicyRuleSeverityType.MINOR);
        verifyAndRemove(policySeverityTypes, PolicyRuleSeverityType.TRIVIAL);

        // We do this verification twice since an UNSPECIFIED value would be in there explicitly and in the case where the severity is null.
        verifyAndRemove(policySeverityTypes, PolicyRuleSeverityType.UNSPECIFIED);
        verifyAndRemove(policySeverityTypes, PolicyRuleSeverityType.UNSPECIFIED);

        Assertions.assertEquals(0, policySeverityTypes.size(), "A PolicySeverityType in the result was not verified.");
    }

    private void verifyAndRemove(List<PolicyRuleSeverityType> policySeverityTypes, PolicyRuleSeverityType policyRuleSeverityType) {
        Assertions.assertTrue(policySeverityTypes.contains(policyRuleSeverityType));
        policySeverityTypes.remove(policyRuleSeverityType);
    }
}
