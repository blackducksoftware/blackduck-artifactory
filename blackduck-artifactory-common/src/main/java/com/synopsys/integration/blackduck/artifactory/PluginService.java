/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

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
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModule;
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
        ConfigurationPropertyManager configurationPropertyManager = new ConfigurationPropertyManager(unprocessedProperties);

        PluginConfig pluginConfig = PluginConfig.createFromProperties(configurationPropertyManager);
        BlackDuckServerConfig blackDuckServerConfig = pluginConfig.getBlackDuckServerConfigBuilder().build();

        DateTimeManager dateTimeManager = new DateTimeManager(pluginConfig.getDateTimePattern(), pluginConfig.getDateTimeZone());
        PluginRepoPathFactory pluginRepoPathFactory = new PluginRepoPathFactory();
        ArtifactoryPAPIService artifactoryPAPIService = new ArtifactoryPAPIService(pluginRepoPathFactory, repositories, searches);
        ArtifactoryPropertyService artifactoryPropertyService = new ArtifactoryPropertyService(artifactoryPAPIService, dateTimeManager);
        ArtifactSearchService artifactSearchService = new ArtifactSearchService(artifactoryPAPIService, artifactoryPropertyService);
        AnalyticsService analyticsService = AnalyticsService.createFromBlackDuckServerConfig(directoryConfig, blackDuckServerConfig);
        BlackDuckServicesFactory blackDuckServicesFactory = blackDuckServerConfig.createBlackDuckServicesFactory(logger);
        ModuleManager moduleManager = new ModuleManager(analyticsService);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ModuleFactory moduleFactory = new ModuleFactory(
            configurationPropertyManager,
            blackDuckServerConfig,
            artifactoryPAPIService,
            artifactoryPropertyService,
            artifactSearchService,
            dateTimeManager,
            blackDuckServicesFactory,
            gson,
            directoryConfig
        );

        ScanModule scanModule = moduleFactory.createScanModule();
        InspectionModule inspectionModule = moduleFactory.createInspectionModule();
        AnalyticsModule analyticsModule = moduleFactory.createAnalyticsModule(analyticsService, moduleManager);

        moduleManager.registerModules(scanModule, inspectionModule, analyticsModule);

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
        PluginAPI pluginAPI = new PluginAPI(featureAnalyticsCollector, moduleManager, scanModule, inspectionModule, analyticsModule);
        analyticsService.registerAnalyzable(pluginAPI);

        logger.info("...blackDuckPlugin initialized.");
        return pluginAPI;
    }

    public String logStatusCheckMessage(TriggerType triggerType) {
        LogUtil.start(logger, "generateStatusCheckMessage", triggerType);
        ConfigValidationReport configValidationReport = configValidationService.validateConfig();
        String statusCheckMessage = configValidationService.generateStatusCheckMessage(configValidationReport, true);
        logger.info(statusCheckMessage);
        LogUtil.finish(logger, "generateStatusCheckMessage", triggerType);

        return statusCheckMessage;
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
