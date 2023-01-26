/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.analytics;

import java.util.List;

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationPropertyManager;
import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyGroupReport;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;

public class AnalyticsModuleConfig extends ModuleConfig {
    public AnalyticsModuleConfig(Boolean enabled) {
        super(AnalyticsModule.class.getSimpleName(), enabled);
    }

    public static AnalyticsModuleConfig createFromProperties(ConfigurationPropertyManager configurationPropertyManager) {
        Boolean enabled = configurationPropertyManager.getBooleanProperty(AnalyticsModuleProperty.ENABLED);

        return new AnalyticsModuleConfig(enabled);
    }

    @Override
    public void validate(PropertyGroupReport propertyGroupReport, List<String> enabledModules) {
        validateBoolean(propertyGroupReport, AnalyticsModuleProperty.ENABLED, isEnabledUnverified());
    }
}
