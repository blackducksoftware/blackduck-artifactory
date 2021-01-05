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
