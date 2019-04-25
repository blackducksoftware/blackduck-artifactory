/*
 * blackduck-artifactory-common
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.synopsys.integration.blackduck.artifactory.modules

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty
import com.synopsys.integration.blackduck.artifactory.modules.analytics.serivce.AnalyticsService
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
        final ModuleConfig validModuleConfig = new MockModuleConfig("Mock1", true, new MockConfigProperty("test1"), "test!")
        final Module validModule = new MockModule(validModuleConfig)

        final ModuleConfig validModuleConfig2 = new MockModuleConfig("Mock2", true, new MockConfigProperty("test2"), "test2!")
        final Module validModule2 = new MockModule(validModuleConfig2)

        final ModuleConfig invalidModuleConfig = new MockModuleConfig("Mock3", true, new MockConfigProperty("test3"), null)
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

    private class MockConfigProperty implements ConfigurationProperty {
        private final String key

        public MockConfigProperty(final String key) {
            this.key = key;
        }

        @Override
        String getKey() {
            return key
        }
    }

}