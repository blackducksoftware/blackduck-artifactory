/**
 * blackduck-artifactory-common
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
import com.synopsys.integration.util.BuilderStatus;

public class PolicyModuleConfig extends ModuleConfig {
    private final Boolean metadataBlockEnabled;

    public PolicyModuleConfig(final Boolean enabled, final Boolean metadataBlockEnabled) {
        super(PolicyModule.class.getSimpleName(), enabled);
        this.metadataBlockEnabled = metadataBlockEnabled;
    }

    public static PolicyModuleConfig createFromProperties(final ConfigurationPropertyManager configurationPropertyManager) {
        final Boolean enabled = configurationPropertyManager.getBooleanProperty(PolicyModuleProperty.ENABLED);
        final Boolean metadataBlockEnabled = configurationPropertyManager.getBooleanProperty(PolicyModuleProperty.METADATA_BLOCK);

        return new PolicyModuleConfig(enabled, metadataBlockEnabled);
    }

    public Boolean isMetadataBlockEnabled() {
        return metadataBlockEnabled;
    }

    @Override
    public void validate(final BuilderStatus builderStatus) {
        // Until feature is re-enabled, don't verify the property's existence
        validateBoolean(builderStatus, PolicyModuleProperty.ENABLED, isEnabledUnverified());
        if (metadataBlockEnabled != null) {
            // TODO: This is temporary until the feature it released or removed
            builderStatus.addErrorMessage("The metadata block feature is not available right now. Please remove from configuration");
        }
    }
}
