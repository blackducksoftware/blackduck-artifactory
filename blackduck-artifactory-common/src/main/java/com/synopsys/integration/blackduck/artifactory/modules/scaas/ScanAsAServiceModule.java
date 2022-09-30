/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scaas;

import java.util.List;

import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.AnalyticsCollector;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ScanAsAServiceModule implements Module {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ScanAsAServiceModuleConfig scanAsAServiceModuleConfig;

    public ScanAsAServiceModule(ScanAsAServiceModuleConfig scanAsAServiceModuleConfig) {
        this.scanAsAServiceModuleConfig = scanAsAServiceModuleConfig;
    }

    @Override
    public ScanAsAServiceModuleConfig getModuleConfig() {
        return scanAsAServiceModuleConfig;
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return null;
    }
}
