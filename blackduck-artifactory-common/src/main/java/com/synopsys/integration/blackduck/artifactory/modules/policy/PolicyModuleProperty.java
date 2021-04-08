/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.policy;

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty;

public enum PolicyModuleProperty implements ConfigurationProperty {
    ENABLED("enabled"),
    SEVERITY_TYPES("severity.types");

    private final String key;

    PolicyModuleProperty(String key) {
        this.key = "blackduck.artifactory.policy." + key;
    }

    @Override
    public String getKey() {
        return key;
    }
}
