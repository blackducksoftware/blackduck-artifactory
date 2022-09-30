/*
 * Copyright (C) 2022 Synopsys Inc.
 * http://www.synopsys.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Synopsys ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Synopsys.
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
