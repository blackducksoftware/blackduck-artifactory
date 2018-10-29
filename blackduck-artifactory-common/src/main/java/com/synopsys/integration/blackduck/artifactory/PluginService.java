/**
 * hub-artifactory-common
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.Repositories;
import org.artifactory.search.Searches;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.modules.LogUtil;
import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleManager;
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
import com.synopsys.integration.blackduck.configuration.HubServerConfigBuilder;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class PluginService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));
    private final PluginConfig pluginConfig;
    private final Repositories repositories;
    private final Searches searches;

    private BlackDuckPropertyManager blackDuckPropertyManager;
    private HubServerConfig hubServerConfig;
    private File blackDuckDirectory;
    private DateTimeManager dateTimeManager;
    private ArtifactoryPropertyService artifactoryPropertyService;
    private BlackDuckConnectionService blackDuckConnectionService;
    private AnalyticsService analyticsService;
    private List<Module> registeredModules;

    public PluginService(final PluginConfig pluginConfig, final Repositories repositories, final Searches searches) {
        this.pluginConfig = pluginConfig;
        this.repositories = repositories;
        this.searches = searches;
    }

    public ModuleManager initializePlugin() throws IOException, IntegrationException {
        logger.info("initializing blackDuckPlugin...");

        final File propertiesFile = getPropertiesFile();
        final Properties unprocessedProperties = loadPropertiesFromFile(propertiesFile);
        blackDuckPropertyManager = new BlackDuckPropertyManager(unprocessedProperties);

        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        hubServerConfigBuilder.setFromProperties(blackDuckPropertyManager.properties);
        hubServerConfig = hubServerConfigBuilder.build();

        this.blackDuckDirectory = setUpBlackDuckDirectory();

        dateTimeManager = new DateTimeManager(blackDuckPropertyManager.getProperty(BlackDuckProperty.DATE_TIME_PATTERN));
        artifactoryPropertyService = new ArtifactoryPropertyService(repositories, searches, dateTimeManager);
        blackDuckConnectionService = new BlackDuckConnectionService(pluginConfig, hubServerConfig);
        analyticsService = new AnalyticsService(blackDuckConnectionService);

        registeredModules = new ArrayList<>();
        final ScanModule scanModule = createAndRegisterScanModule();
        final InspectionModule inspectionModule = createAndRegisterInspectionModule();
        final PolicyModule policyModule = createAndRegisterPolicyModule();
        final AnalyticsModule analyticsModule = createAndRegisterAnalyticsModule();
        analyticsModule.setModuleConfigs(registeredModules.stream().map(Module::getModuleConfig).collect(Collectors.toList()));

        final FeatureAnalyticsCollector featureAnalyticsCollector = new FeatureAnalyticsCollector(ModuleManager.class);
        final ModuleManager moduleManager = ModuleManager.createFromModules(featureAnalyticsCollector, scanModule, inspectionModule, policyModule, analyticsModule);

        registeredModules.stream()
            .map(Module::getModuleConfig)
            .forEach(moduleConfig -> logger.info(String.format("Module [%s] created with enabled state [%b]", moduleConfig.getModuleName(), moduleConfig.isEnabled())));

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
                final String moduleState = entry.getValue().get(0);
                final boolean moduleEnabled = BooleanUtils.toBoolean(moduleState);
                final String moduleName = entry.getKey();

                final List<ModuleConfig> matchingModuleConfigs = registeredModules.stream()
                                                                     .map(Module::getModuleConfig)
                                                                     .filter(moduleConfig -> moduleConfig.getModuleName().equalsIgnoreCase(moduleName))
                                                                     .collect(Collectors.toList());

                matchingModuleConfigs.forEach(moduleConfig -> {
                    logger.warn(String.format("Setting %s's enabled state to %b", moduleConfig.getModuleName(), moduleEnabled));
                    moduleConfig.setEnabled(moduleEnabled);
                });

            }
        }

        LogUtil.finish(logger, "setModuleState", triggerType);
    }

    private ScanModule createAndRegisterScanModule() throws IOException, IntegrationException {
        final File cliDirectory = ScanModule.setUpCliDuckDirectory(blackDuckDirectory);
        final ScanModuleConfig scanModuleConfig = ScanModuleConfig.createFromProperties(blackDuckPropertyManager, cliDirectory);
        final RepositoryIdentificationService repositoryIdentificationService = new RepositoryIdentificationService(blackDuckPropertyManager, dateTimeManager, repositories, searches);
        final ArtifactScanService artifactScanService = new ArtifactScanService(scanModuleConfig, hubServerConfig, blackDuckDirectory, blackDuckPropertyManager, repositoryIdentificationService,
            artifactoryPropertyService, repositories, dateTimeManager);
        final StatusCheckService statusCheckService = new StatusCheckService(scanModuleConfig, blackDuckConnectionService, repositoryIdentificationService, dateTimeManager);
        final SimpleAnalyticsCollector simpleAnalyticsCollector = new SimpleAnalyticsCollector();
        final ScanPolicyService scanPolicyService = ScanPolicyService.createDefault(blackDuckConnectionService, artifactoryPropertyService, dateTimeManager);
        final ScanModule scanModule = new ScanModule(scanModuleConfig, repositoryIdentificationService, artifactScanService, artifactoryPropertyService, statusCheckService, simpleAnalyticsCollector, scanPolicyService);

        registeredModules.add(scanModule);
        analyticsService.registerAnalyzable(scanModule);

        return scanModule;
    }

    private InspectionModule createAndRegisterInspectionModule() throws IOException {
        final CacheInspectorService cacheInspectorService = new CacheInspectorService(blackDuckPropertyManager, repositories, artifactoryPropertyService);
        final List<String> repoKeys = cacheInspectorService.getRepositoriesToInspect();
        final InspectionModuleConfig inspectionModuleConfig = InspectionModuleConfig.createFromProperties(blackDuckPropertyManager, repoKeys);
        final PackageTypePatternManager packageTypePatternManager = new PackageTypePatternManager();
        packageTypePatternManager.loadPatterns(blackDuckPropertyManager);
        final ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        final ArtifactoryExternalIdFactory artifactoryExternalIdFactory = new ArtifactoryExternalIdFactory(externalIdFactory);
        final ArtifactIdentificationService artifactIdentificationService = new ArtifactIdentificationService(repositories, searches, packageTypePatternManager,
            artifactoryExternalIdFactory, artifactoryPropertyService, cacheInspectorService, blackDuckConnectionService);
        final ArtifactMetaDataService artifactMetaDataService = new ArtifactMetaDataService(blackDuckConnectionService);
        final MetaDataPopulationService metaDataPopulationService = new MetaDataPopulationService(artifactoryPropertyService, cacheInspectorService, artifactMetaDataService);
        final MetaDataUpdateService metaDataUpdateService = new MetaDataUpdateService(artifactoryPropertyService, cacheInspectorService, artifactMetaDataService, metaDataPopulationService);
        final SimpleAnalyticsCollector simpleAnalyticsCollector = new SimpleAnalyticsCollector();
        final InspectionModule inspectionModule = new InspectionModule(inspectionModuleConfig, artifactIdentificationService, metaDataPopulationService, metaDataUpdateService, artifactoryPropertyService, repositories,
            simpleAnalyticsCollector);

        registeredModules.add(inspectionModule);
        analyticsService.registerAnalyzable(inspectionModule);

        return inspectionModule;
    }

    private PolicyModule createAndRegisterPolicyModule() {
        final PolicyModuleConfig policyModuleConfig = PolicyModuleConfig.createFromProperties(blackDuckPropertyManager);
        final FeatureAnalyticsCollector featureAnalyticsCollector = new FeatureAnalyticsCollector(PolicyModule.class);
        final PolicyModule policyModule = new PolicyModule(policyModuleConfig, artifactoryPropertyService, featureAnalyticsCollector);

        registeredModules.add(policyModule);
        analyticsService.registerAnalyzable(policyModule);

        return policyModule;
    }

    private AnalyticsModule createAndRegisterAnalyticsModule() {
        final AnalyticsModuleConfig analyticsModuleConfig = AnalyticsModuleConfig.createFromProperties(blackDuckPropertyManager);
        final SimpleAnalyticsCollector simpleAnalyticsCollector = new SimpleAnalyticsCollector();
        final AnalyticsModule analyticsModule = new AnalyticsModule(analyticsModuleConfig, analyticsService, simpleAnalyticsCollector);

        registeredModules.add(analyticsModule);
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
            blackDuckDirectory = new File(pluginConfig.getHomeDirectory(), scanBinariesDirectory);
        } else {
            blackDuckDirectory = new File(pluginConfig.getEtcDirectory(), "blackducksoftware");
        }

        return blackDuckDirectory;
    }

    private File getPropertiesFile() {
        final String propertiesFilePathOverride = pluginConfig.getPropertiesFilePathOverride();
        final File propertiesFile;

        if (StringUtils.isNotBlank(propertiesFilePathOverride)) {
            propertiesFile = new File(propertiesFilePathOverride);
        } else {
            propertiesFile = new File(pluginConfig.getPluginsLibDirectory(), "blackDuckPlugin.properties");
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
