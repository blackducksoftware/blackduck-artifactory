/**
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
package com.synopsys.integration.blackduck.artifactory.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsService;
import com.synopsys.integration.builder.BuilderStatus;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

/**
 * A list of modules that have passed verification and registers them for analytics collection
 */
public class ModuleRegistry {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final List<Module> registeredModules = new ArrayList<>();
    private final List<Module> allModules = new ArrayList<>();

    private final AnalyticsService analyticsService;

    public ModuleRegistry(final AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    private void registerModule(final Module module) {
        final ModuleConfig moduleConfig = module.getModuleConfig();
        final BuilderStatus builderStatus = new BuilderStatus();
        moduleConfig.validate(builderStatus);

        allModules.add(module);
        if (builderStatus.isValid()) {
            registeredModules.add(module);
            analyticsService.registerAnalyzable(module);
            logger.info(String.format("Successfully registered '%s'", moduleConfig.getModuleName()));
        } else {
            moduleConfig.setEnabled(false);
            logger.warn(String.format("Can't register module '%s' due to an invalid configuration. See details below. %s%s", moduleConfig.getModuleName(), System.lineSeparator(), builderStatus.getFullErrorMessage(System.lineSeparator())));
        }
    }

    public void registerModules(final Module... modules) {
        for (final Module module : modules) {
            registerModule(module);
        }
    }

    /**
     * @return a list of ModuleConfig from registered modules with valid configurations
     */
    public List<ModuleConfig> getModuleConfigs() {
        return registeredModules.stream()
                   .map(Module::getModuleConfig)
                   .collect(Collectors.toList());
    }

    /**
     * @return a list of all ModuleConfig regardless of validation status
     */
    public List<ModuleConfig> getAllModuleConfigs() {
        return allModules.stream()
                   .map(Module::getModuleConfig)
                   .collect(Collectors.toList());
    }

    public List<ModuleConfig> getModuleConfigsByName(final String moduleName) {
        return getModuleConfigs().stream()
                   .filter(moduleConfig -> moduleConfig.getModuleName().equalsIgnoreCase(moduleName))
                   .collect(Collectors.toList());
    }
}
