/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.artifactory.modules.analytics;

import java.util.Collections;
import java.util.List;

import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleRegistry;

public class AnalyticsModule implements Module {
    public final static String SUBMIT_ANALYTICS_CRON = "0 0 0 1/1 * ? *"; // Every day at 12 am

    private final AnalyticsModuleConfig analyticsModuleConfig;
    private final AnalyticsService analyticsService;
    private final SimpleAnalyticsCollector simpleAnalyticsCollector;
    private final ModuleRegistry moduleRegistry;

    private int submissionAttemptCounter = 0;

    public AnalyticsModule(final AnalyticsModuleConfig analyticsModuleConfig, final AnalyticsService analyticsService, final SimpleAnalyticsCollector simpleAnalyticsCollector, final ModuleRegistry moduleRegistry) {
        this.analyticsModuleConfig = analyticsModuleConfig;
        this.analyticsService = analyticsService;
        this.simpleAnalyticsCollector = simpleAnalyticsCollector;
        this.moduleRegistry = moduleRegistry;
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
        moduleRegistry.getModuleConfigs().forEach(this::updateModuleStatus);

        final boolean analyticsSuccess = analyticsService.submitAnalytics();
        if (!analyticsSuccess) {
            submissionAttemptCounter++;

            if (submissionAttemptCounter >= 30) {
                // Disabled the analytics module because attempts to phone home have failed an egregious number of times
                analyticsModuleConfig.setEnabled(false);
            }
        }

        return analyticsSuccess;
    }

    private void updateModuleStatus(final ModuleConfig moduleConfig) {
        final String key = String.format("modules.%s.enabled", moduleConfig.getModuleName());
        simpleAnalyticsCollector.putMetadata(key, moduleConfig.isEnabled());
    }

}
