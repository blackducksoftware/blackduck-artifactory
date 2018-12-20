/**
 * blackduck-artifactory-common
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
package com.synopsys.integration.blackduck.artifactory.modules;

import org.apache.commons.lang3.BooleanUtils;

import com.synopsys.integration.blackduck.artifactory.ConfigurationValidator;

public abstract class ModuleConfig extends ConfigurationValidator {
    private final String moduleName;
    private final Boolean enabledProperty;
    private boolean enabled;

    public ModuleConfig(final String moduleName, final Boolean enabled) {
        this.moduleName = moduleName;
        this.enabledProperty = enabled;
        this.enabled = BooleanUtils.toBoolean(enabledProperty);
    }

    public String getModuleName() {
        return moduleName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * For validation
     */
    public Boolean isEnabledUnverified() {
        return enabledProperty;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }
}
