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

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationPropertyManager;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;
import com.synopsys.integration.builder.BuilderStatus;

public class PolicyModuleConfig extends ModuleConfig {
    public PolicyModuleConfig(final Boolean enabled) {
        super(PolicyModule.class.getSimpleName(), enabled);
    }

    public static PolicyModuleConfig createFromProperties(final ConfigurationPropertyManager configurationPropertyManager) {
        final Boolean enabled = configurationPropertyManager.getBooleanProperty(PolicyModuleProperty.ENABLED);

        return new PolicyModuleConfig(enabled);
    }

    @Override
    public void validate(final BuilderStatus builderStatus) {
        validateBoolean(builderStatus, PolicyModuleProperty.ENABLED, isEnabledUnverified());
    }
}
