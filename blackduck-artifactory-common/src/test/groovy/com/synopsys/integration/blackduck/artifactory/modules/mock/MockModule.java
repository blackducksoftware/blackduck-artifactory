package com.synopsys.integration.blackduck.artifactory.modules.mock;

import java.util.List;

import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsCollector;

public class MockModule implements Module {
    private final ModuleConfig moduleConfig;

    public MockModule(final ModuleConfig moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    @Override
    public ModuleConfig getModuleConfig() {
        return moduleConfig;
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return null;
    }
}
