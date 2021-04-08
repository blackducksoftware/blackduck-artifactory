/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.policy;

import java.util.List;
import java.util.stream.Collectors;

import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationPropertyManager;
import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyGroupReport;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;

public class PolicyModuleConfig extends ModuleConfig {
    private final List<PolicyRuleSeverityType> policySeverityTypes;

    public PolicyModuleConfig(Boolean enabled, List<PolicyRuleSeverityType> policySeverityTypes) {
        super(PolicyModule.class.getSimpleName(), enabled);
        this.policySeverityTypes = policySeverityTypes;
    }

    public static PolicyModuleConfig createFromProperties(ConfigurationPropertyManager configurationPropertyManager) {
        Boolean enabled = configurationPropertyManager.getBooleanProperty(PolicyModuleProperty.ENABLED);
        List<PolicyRuleSeverityType> policySeverityTypes = configurationPropertyManager.getPropertyAsList(PolicyModuleProperty.SEVERITY_TYPES).stream()
                                                               .map(PolicyRuleSeverityType::valueOf)
                                                               .collect(Collectors.toList());

        return new PolicyModuleConfig(enabled, policySeverityTypes);
    }

    @Override
    public void validate(PropertyGroupReport propertyGroupReport) {
        validateBoolean(propertyGroupReport, PolicyModuleProperty.ENABLED, isEnabledUnverified());
        validateList(propertyGroupReport, PolicyModuleProperty.SEVERITY_TYPES, policySeverityTypes, "No severity types were provided to block on.");
    }

    public List<PolicyRuleSeverityType> getPolicySeverityTypes() {
        return policySeverityTypes;
    }
}
