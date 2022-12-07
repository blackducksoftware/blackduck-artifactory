/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.analytics;

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty;

public enum AnalyticsModuleProperty implements ConfigurationProperty {
    ENABLED("enabled");

    private final String key;

    AnalyticsModuleProperty(String key) {
        this.key = "blackduck.artifactory.analytics." + key;
    }

    @Override
    public String getKey() {
        return key;
    }
}
