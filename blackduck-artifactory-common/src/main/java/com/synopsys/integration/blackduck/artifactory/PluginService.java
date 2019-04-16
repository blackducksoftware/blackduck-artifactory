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

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationPropertyManager;
import com.synopsys.integration.blackduck.artifactory.configuration.DirectoryConfig;
import com.synopsys.integration.blackduck.artifactory.configuration.PluginConfig;
import com.synopsys.integration.blackduck.artifactory.configuration.StatusCheckService;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleFactory;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleManager;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleRegistry;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsModule;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.FeatureAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.serivce.AnalyticsService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModule;
import com.synopsys.integration.blackduck.artifactory.modules.policy.PolicyModule;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModule;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModuleProperty;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class PluginService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final DirectoryConfig directoryConfig;
    private final Repositories repositories;
    private final Searches searches;

    private ConfigurationPropertyManager configurationPropertyManager;
    private File blackDuckDirectory;
    private StatusCheckService statusCheckService;

    public PluginService(final DirectoryConfig directoryConfig, final Repositories repositories, final Searches searches) {
        this.directoryConfig = directoryConfig;
        this.repositories = repositories;
        this.searches = searches;
    }

    public ModuleManager initializePlugin() throws IOException, IntegrationException {
        logger.info("initializing blackDuckPlugin...");

        final File propertiesFile = getPropertiesFile();
        final Properties unprocessedProperties = loadPropertiesFromFile(propertiesFile);
        configurationPropertyManager = new ConfigurationPropertyManager(unprocessedProperties);

        final PluginConfig pluginConfig = PluginConfig.createFromProperties(configurationPropertyManager);
        final BlackDuckServerConfig blackDuckServerConfig = pluginConfig.getBlackDuckServerConfigBuilder().build();

        this.blackDuckDirectory = setUpBlackDuckDirectory();

        final DateTimeManager dateTimeManager = new DateTimeManager(pluginConfig.getDateTimePattern(), pluginConfig.getDateTimeZone());
        final ArtifactoryPAPIService artifactoryPAPIService = new ArtifactoryPAPIService(repositories, searches);
        final ArtifactoryPropertyService artifactoryPropertyService = new ArtifactoryPropertyService(artifactoryPAPIService, dateTimeManager);
        final AnalyticsService analyticsService = AnalyticsService.createFromBlackDuckServerConfig(directoryConfig, blackDuckServerConfig);
        final BlackDuckServicesFactory blackDuckServicesFactory = blackDuckServerConfig.createBlackDuckServicesFactory(logger);
        final ModuleFactory moduleFactory = new ModuleFactory(configurationPropertyManager, blackDuckServerConfig, artifactoryPAPIService, artifactoryPropertyService, dateTimeManager, blackDuckServicesFactory);
        final ModuleRegistry moduleRegistry = new ModuleRegistry(analyticsService);

        final ScanModule scanModule = moduleFactory.createScanModule(blackDuckDirectory);
        final InspectionModule inspectionModule = moduleFactory.createInspectionModule();
        final PolicyModule policyModule = moduleFactory.createPolicyModule();
        final AnalyticsModule analyticsModule = moduleFactory.createAnalyticsModule(analyticsService, moduleRegistry);

        moduleRegistry.registerModules(scanModule, inspectionModule, policyModule, analyticsModule);

        statusCheckService = new StatusCheckService(moduleRegistry, pluginConfig, directoryConfig.getVersionFile());
        logStatusCheckMessage(TriggerType.STARTUP);

        final FeatureAnalyticsCollector featureAnalyticsCollector = new FeatureAnalyticsCollector(ModuleManager.class);
        final ModuleManager moduleManager = ModuleManager.createFromModules(moduleRegistry, featureAnalyticsCollector, scanModule, inspectionModule, policyModule, analyticsModule);
        analyticsService.registerAnalyzable(moduleManager);

        logger.info("...blackDuckPlugin initialized.");
        return moduleManager;
    }

    public void reloadBlackDuckDirectory(final TriggerType triggerType) throws IOException, IntegrationException {
        LogUtil.start(logger, "blackDuckReloadDirectory", triggerType);

        FileUtils.deleteDirectory(determineBlackDuckDirectory());
        this.blackDuckDirectory = setUpBlackDuckDirectory();

        LogUtil.finish(logger, "blackDuckReloadDirectory", triggerType);
    }

    public String logStatusCheckMessage(final TriggerType triggerType) {
        LogUtil.start(logger, "logStatusCheckMessage", triggerType);
        final String statusCheckMessage = statusCheckService.logStatusCheckMessage();
        LogUtil.finish(logger, "logStatusCheckMessage", triggerType);

        return statusCheckMessage;
    }

    private File setUpBlackDuckDirectory() throws IOException, IntegrationException {
        try {
            final File blackDuckDirectory = determineBlackDuckDirectory();

            if (!blackDuckDirectory.exists() && !blackDuckDirectory.mkdir()) {
                throw new IntegrationException(String.format("Failed to create the BlackDuck directory: %s", blackDuckDirectory.getCanonicalPath()));
            }

            return blackDuckDirectory;
        } catch (final IOException | IntegrationException e) {
            logger.error(String.format("Exception while setting up the Black Duck directory %s", blackDuckDirectory), e);
            throw e;
        }
    }

    private File determineBlackDuckDirectory() {
        final File blackDuckDirectory;
        final String scanBinariesDirectory = configurationPropertyManager.getProperty(ScanModuleProperty.BINARIES_DIRECTORY_PATH);
        if (StringUtils.isNotEmpty(scanBinariesDirectory)) {
            blackDuckDirectory = new File(directoryConfig.getHomeDirectory(), scanBinariesDirectory);
        } else {
            blackDuckDirectory = new File(directoryConfig.getEtcDirectory(), "blackducksoftware");
        }

        return blackDuckDirectory;
    }

    private File getPropertiesFile() {
        final String propertiesFilePathOverride = directoryConfig.getPropertiesFilePathOverride();
        final File propertiesFile;

        if (StringUtils.isNotBlank(propertiesFilePathOverride)) {
            propertiesFile = new File(propertiesFilePathOverride);
        } else {
            propertiesFile = new File(directoryConfig.getPluginsLibDirectory(), "blackDuckPlugin.properties");
        }

        return propertiesFile;
    }

    private Properties loadPropertiesFromFile(final File propertiesFile) throws IOException {
        final Properties properties = new Properties();
        try (final FileInputStream fileInputStream = new FileInputStream(propertiesFile)) {
            properties.load(fileInputStream);
        }

        return properties;
    }
}
