package com.synopsys.integration.blackduck.artifactory.modules;

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.service.AnalyticsService;
import com.synopsys.integration.blackduck.artifactory.modules.mock.MockModule;
import com.synopsys.integration.blackduck.artifactory.modules.mock.MockModuleConfig;

public class ModuleManagerTest {
    private ModuleManager moduleManager;

    @BeforeEach
    public void init() {
        final ModuleConfig validModuleConfig = new MockModuleConfig("Mock1", true, new MockConfigProperty("test1"), "test!");
        final Module validModule = new MockModule(validModuleConfig);

        final ModuleConfig validModuleConfig2 = new MockModuleConfig("Mock2", true, new MockConfigProperty("test2"), "test2!");
        final Module validModule2 = new MockModule(validModuleConfig2);

        final ModuleConfig invalidModuleConfig = new MockModuleConfig("Mock3", true, new MockConfigProperty("test3"), null);
        final Module invalidModule = new MockModule(invalidModuleConfig);

        final AnalyticsService analyticsService = mock(AnalyticsService.class);
        moduleManager = new ModuleManager(analyticsService);
        moduleManager.registerModules(validModule, validModule2, invalidModule);
    }

    @Test
    public void getModuleConfigs() {
        Assert.assertEquals(2, moduleManager.getModuleConfigs().size());
    }

    @Test
    public void getAllModuleConfigs() {
        Assert.assertEquals(3, moduleManager.getAllModuleConfigs().size());
    }

    @Test
    public void getModuleConfigsByName() {
        Assert.assertEquals(1, moduleManager.getModuleConfigsByName("Mock1").size());
        Assert.assertEquals(1, moduleManager.getModuleConfigsByName("Mock2").size());
        Assert.assertEquals(0, moduleManager.getModuleConfigsByName("Mock3").size());
        Assert.assertEquals(0, moduleManager.getModuleConfigsByName("Doesn't exist").size());
    }

    private class MockConfigProperty implements ConfigurationProperty {
        private final String key;

        public MockConfigProperty(final String key) {
            this.key = key;
        }

        @Override
        public String getKey() {
            return key;
        }
    }
}
