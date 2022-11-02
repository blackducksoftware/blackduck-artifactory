/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scaas;

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty;

public enum ScanAsAServiceModuleProperty implements ConfigurationProperty {
    ENABLED("enabled"),
    BLOCKING_STRATEGY("blocking.strategy"),
    BLOCKING_REPOS("blocking.repos"),
    BLOCKING_REPOS_CSV_PATH("blocking.repos.csv.path"),
    ;

    private static final String SCAN_AS_A_SERVICE_MODULE_PROPERTY_PREFIX = "blackduck.artifactory.scaas.";
    private final String key;

    ScanAsAServiceModuleProperty(String key) {
        this.key = SCAN_AS_A_SERVICE_MODULE_PROPERTY_PREFIX + key;
    }

    @Override
    public String getKey() {
        return key;
    }
}
