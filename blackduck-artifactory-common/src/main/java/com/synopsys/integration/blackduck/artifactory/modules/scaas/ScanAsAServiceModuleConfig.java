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

import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyGroupReport;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;

public class ScanAsAServiceModuleConfig extends ModuleConfig {
    public ScanAsAServiceModuleConfig(Boolean enabled) {
        super(ScanAsAServiceModule.class.getSimpleName(), enabled);
    }

    public static ScanAsAServiceModuleConfig createFromProperties() {
        return new ScanAsAServiceModuleConfig(
                Boolean.TRUE
        );
    }

    @Override
    public void validate(PropertyGroupReport propertyGroupReport) {

    }
}
