/**
 * blackduck-artifactory-common
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
package com.synopsys.integration.blackduck.artifactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.Repositories;
import org.artifactory.search.Searches;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.modules.LogUtil;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleManager;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleRegistry;
import com.synopsys.integration.blackduck.artifactory.modules.TriggerType;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsModule;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsService;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.FeatureAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.SimpleAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.ArtifactIdentificationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.ArtifactoryExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.CacheInspectorService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModule;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.MetaDataPopulationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.MetaDataUpdateService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.PackageTypePatternManager;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.ArtifactMetaDataService;
import com.synopsys.integration.blackduck.artifactory.modules.policy.PolicyModule;
import com.synopsys.integration.blackduck.artifactory.modules.policy.PolicyModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ArtifactScanService;
import com.synopsys.integration.blackduck.artifactory.modules.scan.RepositoryIdentificationService;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModule;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModuleProperty;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanPolicyService;
import com.synopsys.integration.blackduck.artifactory.modules.scan.StatusCheckService;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.phonehome.google.analytics.GoogleAnalyticsConstants;
import com.synopsys.integration.util.BuilderStatus;

public class PluginService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));
    private final DirectoryConfig directoryConfig;
    private final Repositories repositories;
    private final Searches searches;

    private BlackDuckPropertyManager blackDuckPropertyManager;
    private HubServerConfig hubServerConfig;
    private File blackDuckDirectory;
    private DateTimeManager dateTimeManager;
    private ArtifactoryPropertyService artifactoryPropertyService;
    private ArtifactoryPAPIService artifactoryPAPIService;
    private BlackDuckConnectionService blackDuckConnectionService;
    private AnalyticsService analyticsService;
    private ModuleRegistry moduleRegistry;
    private PluginConfig pluginConfig;

    public PluginService(final DirectoryConfig directoryConfig, final Repositories repositories, final Searches searches) {
        this.directoryConfig = directoryConfig;
        this.repositories = repositories;
        this.searches = searches;
    }

    public ModuleManager initializePlugin() throws IOException, IntegrationException {
        logger.info("initializing blackDuckPlugin...");

        final File propertiesFile = getPropertiesFile();
        final Properties unprocessedProperties = loadPropertiesFromFile(propertiesFile);
        blackDuckPropertyManager = new BlackDuckPropertyManager(unprocessedProperties);

        pluginConfig = PluginConfig.createFromProperties(blackDuckPropertyManager);
        hubServerConfig = pluginConfig.getHubServerConfigBuilder().build();

        this.blackDuckDirectory = setUpBlackDuckDirectory();

        dateTimeManager = new DateTimeManager(pluginConfig.getDateTimePattern());
        artifactoryPAPIService = new ArtifactoryPAPIService(repositories, searches);
        artifactoryPropertyService = new ArtifactoryPropertyService(repositories, searches, dateTimeManager);
        blackDuckConnectionService = new BlackDuckConnectionService(hubServerConfig);
        analyticsService = new AnalyticsService(directoryConfig, blackDuckConnectionService, GoogleAnalyticsConstants.PRODUCTION_INTEGRATIONS_TRACKING_ID);

        moduleRegistry = new ModuleRegistry();
        final ScanModule scanModule = createAndRegisterScanModule();
        final InspectionModule inspectionModule = createAndRegisterInspectionModule();
        final PolicyModule policyModule = createAndRegisterPolicyModule();
        final AnalyticsModule analyticsModule = createAndRegisterAnalyticsModule();
        analyticsModule.setModuleConfigs(moduleRegistry.getModuleConfigs());

        logStatusCheckMessage(TriggerType.STARTUP);

        final FeatureAnalyticsCollector featureAnalyticsCollector = new FeatureAnalyticsCollector(ModuleManager.class);
        final ModuleManager moduleManager = ModuleManager.createFromModules(featureAnalyticsCollector, scanModule, inspectionModule, policyModule, analyticsModule);

        logger.info("...blackDuckPlugin initialized.");
        return moduleManager;
    }

    public void reloadBlackDuckDirectory(final TriggerType triggerType) throws IOException, IntegrationException {
        LogUtil.start(logger, "blackDuckReloadDirectory", triggerType);

        FileUtils.deleteDirectory(determineBlackDuckDirectory());
        this.blackDuckDirectory = setUpBlackDuckDirectory();

        LogUtil.finish(logger, "blackDuckReloadDirectory", triggerType);
    }

    public void setModuleState(final TriggerType triggerType, final Map<String, List<String>> params) {
        LogUtil.start(logger, "setModuleState", triggerType);

        for (final Map.Entry<String, List<String>> entry : params.entrySet()) {
            if (entry.getValue().size() > 0) {
                final String moduleStateRaw = entry.getValue().get(0);
                final boolean moduleState = BooleanUtils.toBoolean(moduleStateRaw);
                final String moduleName = entry.getKey();
                final List<ModuleConfig> moduleConfigs = moduleRegistry.getModuleConfigsByName(moduleName);

                if (moduleConfigs.isEmpty()) {
                    logger.warn(String.format("No registered modules with the name '%s' found. Hit the checkStatusMessage endpoint to see why.", moduleName));
                } else {
                    moduleConfigs.forEach(moduleConfig -> {
                        logger.warn(String.format("Setting %s's enabled state to %b", moduleConfig.getModuleName(), moduleState));
                        moduleConfig.setEnabled(moduleState);
                    });
                }
            }
        }

        LogUtil.finish(logger, "setModuleState", triggerType);
    }

    public String logStatusCheckMessage(final TriggerType triggerType) {
        LogUtil.start(logger, "logStatusCheckMessage", triggerType);

        final String lineSeparator = System.lineSeparator();
        final String blockSeparator = lineSeparator + StringUtils.repeat("-", 100) + lineSeparator;

        final StringBuilder statusCheckMessage = new StringBuilder(blockSeparator + "Status Check" + blockSeparator);

        statusCheckMessage.append("General Settings:").append(lineSeparator);
        final BuilderStatus generalBuilderStatus = new BuilderStatus();
        pluginConfig.validate(generalBuilderStatus);
        if (generalBuilderStatus.isValid()) {
            statusCheckMessage.append("General properties validated");
        } else {
            statusCheckMessage.append(generalBuilderStatus.getFullErrorMessage(lineSeparator));
        }
        statusCheckMessage.append(blockSeparator);

        for (final ModuleConfig moduleConfig : moduleRegistry.getAllModuleConfigs()) {
            statusCheckMessage.append(String.format("Module Name: %s", moduleConfig.getModuleName())).append(lineSeparator);
            statusCheckMessage.append(String.format("Enabled: %b", moduleConfig.isEnabled())).append(lineSeparator);
            final BuilderStatus builderStatus = new BuilderStatus();
            moduleConfig.validate(builderStatus);

            if (builderStatus.isValid()) {
                statusCheckMessage.append(String.format("%s has passed validation", moduleConfig.getModuleName()));
            } else {
                statusCheckMessage.append(builderStatus.getFullErrorMessage(lineSeparator));
            }

            statusCheckMessage.append(blockSeparator);
        }

        final String finalMessage = statusCheckMessage.toString();
        logger.info(finalMessage);
        LogUtil.finish(logger, "logStatusCheckMessage", triggerType);

        return finalMessage;
    }

    private ScanModule createAndRegisterScanModule() throws IOException, IntegrationException {
        final File cliDirectory = ScanModule.setUpCliDuckDirectory(blackDuckDirectory);
        final ScanModuleConfig scanModuleConfig = ScanModuleConfig.createFromProperties(blackDuckPropertyManager, artifactoryPAPIService, cliDirectory, dateTimeManager);
        final RepositoryIdentificationService repositoryIdentificationService = new RepositoryIdentificationService(scanModuleConfig, dateTimeManager, artifactoryPropertyService, artifactoryPAPIService);
        final ArtifactScanService artifactScanService = new ArtifactScanService(scanModuleConfig, hubServerConfig, blackDuckDirectory, repositoryIdentificationService, artifactoryPropertyService, repositories, dateTimeManager);
        final StatusCheckService statusCheckService = new StatusCheckService(scanModuleConfig, blackDuckConnectionService, repositoryIdentificationService, dateTimeManager);
        final SimpleAnalyticsCollector simpleAnalyticsCollector = new SimpleAnalyticsCollector();
        final ScanPolicyService scanPolicyService = ScanPolicyService.createDefault(blackDuckConnectionService, artifactoryPropertyService, dateTimeManager);
        final ScanModule scanModule = new ScanModule(scanModuleConfig, repositoryIdentificationService, artifactScanService, artifactoryPropertyService, artifactoryPAPIService, statusCheckService, simpleAnalyticsCollector,
            scanPolicyService);

        moduleRegistry.registerModule(scanModule);
        analyticsService.registerAnalyzable(scanModule);

        return scanModule;
    }

    private InspectionModule createAndRegisterInspectionModule() throws IOException {
        final InspectionModuleConfig inspectionModuleConfig = InspectionModuleConfig.createFromProperties(blackDuckPropertyManager, artifactoryPAPIService);
        final CacheInspectorService cacheInspectorService = new CacheInspectorService(artifactoryPropertyService);
        final PackageTypePatternManager packageTypePatternManager = PackageTypePatternManager.fromInspectionModuleConfig(inspectionModuleConfig);
        final ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        final ArtifactoryExternalIdFactory artifactoryExternalIdFactory = new ArtifactoryExternalIdFactory(externalIdFactory);
        final ArtifactMetaDataService artifactMetaDataService = new ArtifactMetaDataService(blackDuckConnectionService);
        final MetaDataPopulationService metaDataPopulationService = new MetaDataPopulationService(artifactoryPropertyService, cacheInspectorService, artifactMetaDataService);
        final ArtifactIdentificationService artifactIdentificationService = new ArtifactIdentificationService(repositories, searches, packageTypePatternManager,
            artifactoryExternalIdFactory, artifactoryPropertyService, cacheInspectorService, blackDuckConnectionService, metaDataPopulationService);
        final MetaDataUpdateService metaDataUpdateService = new MetaDataUpdateService(artifactoryPropertyService, cacheInspectorService, artifactMetaDataService, metaDataPopulationService);
        final SimpleAnalyticsCollector simpleAnalyticsCollector = new SimpleAnalyticsCollector();
        final InspectionModule inspectionModule = new InspectionModule(inspectionModuleConfig, artifactIdentificationService, metaDataPopulationService, metaDataUpdateService, artifactoryPropertyService, repositories,
            simpleAnalyticsCollector);

        moduleRegistry.registerModule(inspectionModule);
        analyticsService.registerAnalyzable(inspectionModule);

        return inspectionModule;
    }

    private PolicyModule createAndRegisterPolicyModule() {
        final PolicyModuleConfig policyModuleConfig = PolicyModuleConfig.createFromProperties(blackDuckPropertyManager);
        final FeatureAnalyticsCollector featureAnalyticsCollector = new FeatureAnalyticsCollector(PolicyModule.class);
        final PolicyModule policyModule = new PolicyModule(policyModuleConfig, artifactoryPropertyService, featureAnalyticsCollector);

        moduleRegistry.registerModule(policyModule);
        analyticsService.registerAnalyzable(policyModule);

        return policyModule;
    }

    private AnalyticsModule createAndRegisterAnalyticsModule() {
        final AnalyticsModuleConfig analyticsModuleConfig = AnalyticsModuleConfig.createFromProperties(blackDuckPropertyManager);
        final SimpleAnalyticsCollector simpleAnalyticsCollector = new SimpleAnalyticsCollector();
        final AnalyticsModule analyticsModule = new AnalyticsModule(analyticsModuleConfig, analyticsService, simpleAnalyticsCollector);

        moduleRegistry.registerModule(analyticsModule);
        analyticsService.registerAnalyzable(analyticsModule);

        return analyticsModule;
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
        final String scanBinariesDirectory = blackDuckPropertyManager.getProperty(ScanModuleProperty.BINARIES_DIRECTORY_PATH);
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
