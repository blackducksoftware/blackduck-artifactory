package com.synopsys.integration.blackduck.artifactory.modules

import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsService
import com.synopsys.integration.blackduck.artifactory.modules.mock.MockModule
import com.synopsys.integration.blackduck.artifactory.modules.mock.MockModuleConfig
import groovy.mock.interceptor.MockFor
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ModuleRegistryTest {
    private ModuleRegistry moduleRegistry

    @BeforeEach
    void init() {
        final ModuleConfig validModuleConfig = new MockModuleConfig("Mock1", true, "test!")
        final Module validModule = new MockModule(validModuleConfig)

        final ModuleConfig validModuleConfig2 = new MockModuleConfig("Mock2", true, "test2!")
        final Module validModule2 = new MockModule(validModuleConfig2)

        final ModuleConfig invalidModuleConfig = new MockModuleConfig("Mock3", true, null)
        final Module invalidModule = new MockModule(invalidModuleConfig)

        final def analyticsServiceMock = new MockFor(AnalyticsService)
        analyticsServiceMock.use {
            final AnalyticsService analyticsService = new AnalyticsService(null, null)
            moduleRegistry = new ModuleRegistry(analyticsService)
        }

        moduleRegistry.registerModules(validModule, validModule2, invalidModule)
    }

    @Test
    void registerModules() {
        Assert.assertEquals(2, moduleRegistry.registeredModules.size())
        Assert.assertEquals(3, moduleRegistry.allModules.size())
    }

    @Test
    void getModuleConfigs() {
        Assert.assertEquals(2, moduleRegistry.getModuleConfigs().size())
    }

    @Test
    void getAllModuleConfigs() {
        Assert.assertEquals(3, moduleRegistry.getAllModuleConfigs().size())
    }

    @Test
    void getModuleConfigsByName() {
        Assert.assertEquals(1, moduleRegistry.getModuleConfigsByName("Mock1").size())
        Assert.assertEquals(1, moduleRegistry.getModuleConfigsByName("Mock2").size())
        Assert.assertEquals(0, moduleRegistry.getModuleConfigsByName("Mock3").size())
        Assert.assertEquals(0, moduleRegistry.getModuleConfigsByName("Doesn't exist").size())
    }

}