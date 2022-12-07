/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules;

import org.apache.commons.lang3.BooleanUtils;

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationValidator;

public abstract class ModuleConfig extends ConfigurationValidator {
    private final String moduleName;
    private final Boolean enabledProperty;
    private boolean enabled;

    public ModuleConfig(String moduleName, Boolean enabled) {
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

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
