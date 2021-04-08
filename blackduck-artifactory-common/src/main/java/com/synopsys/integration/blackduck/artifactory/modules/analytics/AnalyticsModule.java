/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.analytics;

import java.util.Collections;
import java.util.List;

import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleManager;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.FeatureAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.SimpleAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.service.AnalyticsService;

public class AnalyticsModule implements Module {
    public static final String SUBMIT_ANALYTICS_CRON = "0 0 0 1/1 * ? *"; // Every day at 12 am

    private final AnalyticsModuleConfig analyticsModuleConfig;
    private final AnalyticsService analyticsService;
    private final SimpleAnalyticsCollector simpleAnalyticsCollector;
    private final ModuleManager moduleManager;

    private int submissionAttemptCounter = 0;

    public AnalyticsModule(AnalyticsModuleConfig analyticsModuleConfig, AnalyticsService analyticsService, SimpleAnalyticsCollector simpleAnalyticsCollector, ModuleManager moduleManager) {
        this.analyticsModuleConfig = analyticsModuleConfig;
        this.analyticsService = analyticsService;
        this.simpleAnalyticsCollector = simpleAnalyticsCollector;
        this.moduleManager = moduleManager;
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return Collections.singletonList(simpleAnalyticsCollector);
    }

    @Override
    public AnalyticsModuleConfig getModuleConfig() {
        return analyticsModuleConfig;
    }

    /**
     * Submits a payload to phone home with data from all the collectors ({@link FeatureAnalyticsCollector})
     * This should be used infrequently such as once a day due to quota
     */
    public Boolean submitAnalytics() {
        moduleManager.getModuleConfigs().forEach(this::updateModuleStatus);

        boolean analyticsSuccess = analyticsService.submitAnalytics();
        if (!analyticsSuccess) {
            submissionAttemptCounter++;

            if (submissionAttemptCounter >= 30) {
                // Disabled the analytics module because attempts to phone home have failed an egregious number of times
                analyticsModuleConfig.setEnabled(false);
            }
        }

        return analyticsSuccess;
    }

    private void updateModuleStatus(ModuleConfig moduleConfig) {
        String key = String.format("modules.%s.enabled", moduleConfig.getModuleName());
        simpleAnalyticsCollector.putMetadata(key, moduleConfig.isEnabled());
    }

}
