/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.artifactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.Repositories;
import org.artifactory.search.Searches;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigValidationService;
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationPropertyManager;
import com.synopsys.integration.blackduck.artifactory.configuration.DirectoryConfig;
import com.synopsys.integration.blackduck.artifactory.configuration.PluginConfig;
import com.synopsys.integration.blackduck.artifactory.configuration.model.ConfigValidationReport;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleFactory;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleManager;
import com.synopsys.integration.blackduck.artifactory.modules.PluginAPI;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsModule;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.FeatureAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.service.AnalyticsService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModule;
import com.synopsys.integration.blackduck.artifactory.modules.policy.PolicyModule;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModule;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModuleProperty;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.log.Slf4jIntLogger;

public class PluginService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final DirectoryConfig directoryConfig;
    private final Repositories repositories;
    private final Searches searches;

    private ConfigurationPropertyManager configurationPropertyManager;
    private File blackDuckDirectory;
    private ConfigValidationService configValidationService;

    public PluginService(DirectoryConfig directoryConfig, Repositories repositories, Searches searches) {
        this.directoryConfig = directoryConfig;
        this.repositories = repositories;
        this.searches = searches;
    }

    public PluginAPI initializePlugin() throws IOException, IntegrationException {
        logger.info("initializing blackDuckPlugin...");

        File propertiesFile = getPropertiesFile();
        Properties unprocessedProperties = loadPropertiesFromFile(propertiesFile);
        configurationPropertyManager = new ConfigurationPropertyManager(unprocessedProperties);

        PluginConfig pluginConfig = PluginConfig.createFromProperties(configurationPropertyManager);
        BlackDuckServerConfig blackDuckServerConfig = pluginConfig.getBlackDuckServerConfigBuilder().build();

        this.blackDuckDirectory = setUpBlackDuckDirectory();

        DateTimeManager dateTimeManager = new DateTimeManager(pluginConfig.getDateTimePattern(), pluginConfig.getDateTimeZone());
        PluginRepoPathFactory pluginRepoPathFactory = new PluginRepoPathFactory();
        ArtifactoryPAPIService artifactoryPAPIService = new ArtifactoryPAPIService(pluginRepoPathFactory, repositories, searches);
        ArtifactoryPropertyService artifactoryPropertyService = new ArtifactoryPropertyService(artifactoryPAPIService, dateTimeManager);
        ArtifactSearchService artifactSearchService = new ArtifactSearchService(artifactoryPAPIService, artifactoryPropertyService);
        AnalyticsService analyticsService = AnalyticsService.createFromBlackDuckServerConfig(directoryConfig, blackDuckServerConfig);
        BlackDuckServicesFactory blackDuckServicesFactory = blackDuckServerConfig.createBlackDuckServicesFactory(logger);
        ModuleManager moduleManager = new ModuleManager(analyticsService);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ModuleFactory moduleFactory = new ModuleFactory(configurationPropertyManager, blackDuckServerConfig, artifactoryPAPIService, artifactoryPropertyService, artifactSearchService, dateTimeManager, blackDuckServicesFactory, gson);

        ScanModule scanModule = moduleFactory.createScanModule(blackDuckDirectory);
        InspectionModule inspectionModule = moduleFactory.createInspectionModule();
        PolicyModule policyModule = moduleFactory.createPolicyModule();
        AnalyticsModule analyticsModule = moduleFactory.createAnalyticsModule(analyticsService, moduleManager);

        moduleManager.registerModules(scanModule, inspectionModule, policyModule, analyticsModule);

        configValidationService = new ConfigValidationService(moduleManager, pluginConfig, directoryConfig.getVersionFile());
        ConfigValidationReport configValidationReport = configValidationService.validateConfig();
        if (configValidationReport.getGeneralPropertyReport().hasError()) {
            moduleManager.setAllModulesEnabledState(false);
        }
        LogLevel logLevel = logger.getLogLevel();
        boolean includeValid = logLevel.isLoggable(LogLevel.INFO);
        String statusCheckMessage = configValidationService.generateStatusCheckMessage(configValidationReport, includeValid);
        logger.warn(statusCheckMessage);

        FeatureAnalyticsCollector featureAnalyticsCollector = new FeatureAnalyticsCollector(PluginAPI.class);
        PluginAPI pluginAPI = PluginAPI.createFromModules(moduleManager, featureAnalyticsCollector, scanModule, inspectionModule, policyModule, analyticsModule);
        analyticsService.registerAnalyzable(pluginAPI);

        logger.info("...blackDuckPlugin initialized.");
        return pluginAPI;
    }

    public void reloadBlackDuckDirectory(TriggerType triggerType) throws IOException, IntegrationException {
        LogUtil.start(logger, "blackDuckReloadDirectory", triggerType);

        FileUtils.deleteDirectory(determineBlackDuckDirectory());
        this.blackDuckDirectory = setUpBlackDuckDirectory();

        LogUtil.finish(logger, "blackDuckReloadDirectory", triggerType);
    }

    public String logStatusCheckMessage(TriggerType triggerType) {
        LogUtil.start(logger, "generateStatusCheckMessage", triggerType);
        ConfigValidationReport configValidationReport = configValidationService.validateConfig();
        String statusCheckMessage = configValidationService.generateStatusCheckMessage(configValidationReport, true);
        logger.info(statusCheckMessage);
        LogUtil.finish(logger, "generateStatusCheckMessage", triggerType);

        return statusCheckMessage;
    }

    private File setUpBlackDuckDirectory() throws IOException, IntegrationException {
        try {
            File directory = determineBlackDuckDirectory();

            if (!directory.exists() && !directory.mkdir()) {
                throw new IntegrationException(String.format("Failed to create the BlackDuck directory: %s", directory.getCanonicalPath()));
            }

            return directory;
        } catch (IOException | IntegrationException e) {
            logger.error(String.format("Exception while setting up the Black Duck directory %s", blackDuckDirectory), e);
            throw e;
        }
    }

    private File determineBlackDuckDirectory() {
        File directory;
        String scanBinariesDirectory = configurationPropertyManager.getProperty(ScanModuleProperty.BINARIES_DIRECTORY_PATH);
        if (StringUtils.isNotEmpty(scanBinariesDirectory)) {
            directory = new File(directoryConfig.getHomeDirectory(), scanBinariesDirectory);
        } else {
            directory = new File(directoryConfig.getEtcDirectory(), "blackducksoftware");
        }

        return directory;
    }

    private File getPropertiesFile() {
        String propertiesFilePathOverride = directoryConfig.getPropertiesFilePathOverride();
        File propertiesFile;

        if (StringUtils.isNotBlank(propertiesFilePathOverride)) {
            propertiesFile = new File(propertiesFilePathOverride);
        } else {
            propertiesFile = new File(directoryConfig.getPluginsLibDirectory(), "blackDuckPlugin.properties");
        }

        return propertiesFile;
    }

    private Properties loadPropertiesFromFile(File propertiesFile) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(propertiesFile)) {
            properties.load(fileInputStream);
        }

        return properties;
    }
}
