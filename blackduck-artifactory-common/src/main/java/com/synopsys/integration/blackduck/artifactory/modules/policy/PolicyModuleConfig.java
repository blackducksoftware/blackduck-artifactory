/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2019 Synopsys, Inc.
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

import com.synopsys.integration.blackduck.api.enumeration.PolicySeverityType;
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationPropertyManager;
import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyGroupReport;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;

public class PolicyModuleConfig extends ModuleConfig {
    private final List<PolicySeverityType> policySeverityTypes;

    public PolicyModuleConfig(final Boolean enabled, final List<PolicySeverityType> policySeverityTypes) {
        super(PolicyModule.class.getSimpleName(), enabled);
        this.policySeverityTypes = policySeverityTypes;
    }

    public static PolicyModuleConfig createFromProperties(final ConfigurationPropertyManager configurationPropertyManager) {
        final Boolean enabled = configurationPropertyManager.getBooleanProperty(PolicyModuleProperty.ENABLED);
        final List<PolicySeverityType> policySeverityTypes = configurationPropertyManager.getPropertyAsList(PolicyModuleProperty.SEVERITY_TYPES).stream()
                                                                 .map(PolicySeverityType::valueOf)
                                                                 .collect(Collectors.toList());

        return new PolicyModuleConfig(enabled, policySeverityTypes);
    }

    @Override
    public void validate(final PropertyGroupReport propertyGroupReport) {
        validateBoolean(propertyGroupReport, PolicyModuleProperty.ENABLED, isEnabledUnverified());
        validateList(propertyGroupReport, PolicyModuleProperty.SEVERITY_TYPES, policySeverityTypes, "No severity types were provided to block on.");
    }

    public List<PolicySeverityType> getPolicySeverityTypes() {
        return policySeverityTypes;
    }
}
