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
package com.synopsys.integration.blackduck.artifactory.modules;

import java.io.File;
import java.io.IOException;

import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.ArtifactSearchService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationPropertyManager;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsModule;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.FeatureAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.SimpleAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.serivce.AnalyticsService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModule;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.ArtifactMetaDataService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.ArtifactNotificationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.NotificationRetrievalService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.ArtifactInspectionService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.BlackDuckBOMService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.MetaDataPopulationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.MetaDataUpdateService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.RepositoryInitializationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.util.ArtifactoryExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.modules.policy.PolicyModule;
import com.synopsys.integration.blackduck.artifactory.modules.policy.PolicyModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModule;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.scan.service.ArtifactScanService;
import com.synopsys.integration.blackduck.artifactory.modules.scan.service.RepositoryIdentificationService;
import com.synopsys.integration.blackduck.artifactory.modules.scan.service.ScanPolicyService;
import com.synopsys.integration.blackduck.codelocation.bdioupload.BdioUploadService;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ComponentService;
import com.synopsys.integration.blackduck.service.NotificationService;
import com.synopsys.integration.blackduck.service.ProjectBomService;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ModuleFactory {
    private final ConfigurationPropertyManager configurationPropertyManager;
    private final BlackDuckServerConfig blackDuckServerConfig;
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final DateTimeManager dateTimeManager;
    private final BlackDuckServicesFactory blackDuckServicesFactory;

    public ModuleFactory(final ConfigurationPropertyManager configurationPropertyManager, final BlackDuckServerConfig blackDuckServerConfig, final ArtifactoryPAPIService artifactoryPAPIService,
        final ArtifactoryPropertyService artifactoryPropertyService, final DateTimeManager dateTimeManager, final BlackDuckServicesFactory blackDuckServicesFactory) {
        this.configurationPropertyManager = configurationPropertyManager;
        this.blackDuckServerConfig = blackDuckServerConfig;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.dateTimeManager = dateTimeManager;
        this.blackDuckServicesFactory = blackDuckServicesFactory;
    }

    public ScanModule createScanModule(final File blackDuckDirectory) throws IOException, IntegrationException {
        final File cliDirectory = ScanModule.setUpCliDuckDirectory(blackDuckDirectory);
        final ScanModuleConfig scanModuleConfig = ScanModuleConfig.createFromProperties(configurationPropertyManager, artifactoryPAPIService, cliDirectory, dateTimeManager);
        final RepositoryIdentificationService repositoryIdentificationService = new RepositoryIdentificationService(scanModuleConfig, dateTimeManager, artifactoryPropertyService, artifactoryPAPIService);
        final ArtifactScanService artifactScanService = new ArtifactScanService(scanModuleConfig, blackDuckServerConfig, blackDuckDirectory, repositoryIdentificationService, artifactoryPropertyService, artifactoryPAPIService);
        final SimpleAnalyticsCollector simpleAnalyticsCollector = new SimpleAnalyticsCollector();
        final ScanPolicyService scanPolicyService = ScanPolicyService.createDefault(blackDuckServerConfig, artifactoryPropertyService);

        return new ScanModule(scanModuleConfig, repositoryIdentificationService, artifactScanService, artifactoryPropertyService, artifactoryPAPIService, simpleAnalyticsCollector, scanPolicyService);
    }

    public InspectionModule createInspectionModule() throws IOException {
        final InspectionModuleConfig inspectionModuleConfig = InspectionModuleConfig.createFromProperties(configurationPropertyManager, artifactoryPAPIService);
        final ProjectService projectService = blackDuckServerConfig.createBlackDuckServicesFactory(new Slf4jIntLogger(LoggerFactory.getLogger(InspectionPropertyService.class))).createProjectService();
        final InspectionPropertyService inspectionPropertyService = new InspectionPropertyService(artifactoryPropertyService, projectService, inspectionModuleConfig);
        final ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        final ArtifactoryExternalIdFactory artifactoryExternalIdFactory = new ArtifactoryExternalIdFactory(inspectionPropertyService, externalIdFactory);
        final ArtifactMetaDataService artifactMetaDataService = ArtifactMetaDataService.createDefault(blackDuckServerConfig);
        final MetaDataPopulationService metaDataPopulationService = new MetaDataPopulationService(inspectionPropertyService, artifactMetaDataService, blackDuckServicesFactory.createComponentService());
        final BlackDuckServicesFactory blackDuckServicesFactory = blackDuckServerConfig.createBlackDuckServicesFactory(new Slf4jIntLogger(LoggerFactory.getLogger(BlackDuckBOMService.class)));
        final ProjectBomService projectBomService = blackDuckServicesFactory.createProjectBomService();
        final ComponentService componentService = blackDuckServicesFactory.createComponentService();
        final BlackDuckService blackDuckService = blackDuckServicesFactory.createBlackDuckService();
        final BlackDuckBOMService blackDuckBOMService = new BlackDuckBOMService(projectBomService, componentService, blackDuckService, metaDataPopulationService);
        final NotificationService notificationService = blackDuckServicesFactory.createNotificationService();
        final ArtifactSearchService artifactSearchService = new ArtifactSearchService(artifactoryPropertyService);
        final NotificationRetrievalService notificationRetrievalService = new NotificationRetrievalService(blackDuckService);
        final ArtifactNotificationService artifactNotificationService = new ArtifactNotificationService(notificationRetrievalService, blackDuckService, notificationService, artifactSearchService, inspectionPropertyService);
        final MetaDataUpdateService metaDataUpdateService = new MetaDataUpdateService(inspectionPropertyService, artifactMetaDataService, metaDataPopulationService, artifactNotificationService);
        final SimpleAnalyticsCollector simpleAnalyticsCollector = new SimpleAnalyticsCollector();
        final BdioUploadService bdioUploadService = blackDuckServicesFactory.createBdioUploadService();

        final ArtifactInspectionService artifactInspectionService = new ArtifactInspectionService(artifactoryPAPIService, blackDuckBOMService, metaDataPopulationService, inspectionModuleConfig, inspectionPropertyService, projectService,
            artifactoryExternalIdFactory);
        final RepositoryInitializationService repositoryInitializationService = new RepositoryInitializationService(inspectionPropertyService, artifactoryPAPIService, inspectionModuleConfig, bdioUploadService, artifactInspectionService);

        return new InspectionModule(inspectionModuleConfig, artifactoryPAPIService, metaDataPopulationService, metaDataUpdateService, artifactoryPropertyService, inspectionPropertyService,
            simpleAnalyticsCollector, repositoryInitializationService, artifactInspectionService);
    }

    public PolicyModule createPolicyModule() {
        final PolicyModuleConfig policyModuleConfig = PolicyModuleConfig.createFromProperties(configurationPropertyManager);
        final FeatureAnalyticsCollector featureAnalyticsCollector = new FeatureAnalyticsCollector(PolicyModule.class);

        return new PolicyModule(policyModuleConfig, artifactoryPropertyService, featureAnalyticsCollector);
    }

    public AnalyticsModule createAnalyticsModule(final AnalyticsService analyticsService, final ModuleRegistry moduleRegistry) {
        final AnalyticsModuleConfig analyticsModuleConfig = AnalyticsModuleConfig.createFromProperties(configurationPropertyManager);
        final SimpleAnalyticsCollector simpleAnalyticsCollector = new SimpleAnalyticsCollector();

        return new AnalyticsModule(analyticsModuleConfig, analyticsService, simpleAnalyticsCollector, moduleRegistry);
    }
}
