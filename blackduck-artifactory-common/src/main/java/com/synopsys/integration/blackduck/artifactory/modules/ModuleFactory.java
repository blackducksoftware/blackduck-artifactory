/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules;

import java.io.IOException;
import java.util.Arrays;

import com.google.gson.Gson;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.ArtifactSearchService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationPropertyManager;
import com.synopsys.integration.blackduck.artifactory.configuration.DirectoryConfig;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsModule;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.SimpleAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.service.AnalyticsService;
import com.synopsys.integration.blackduck.artifactory.modules.cancel.CancelDecider;
import com.synopsys.integration.blackduck.artifactory.modules.cancel.InspectionCancelDecider;
import com.synopsys.integration.blackduck.artifactory.modules.cancel.PolicyCancelDecider;
import com.synopsys.integration.blackduck.artifactory.modules.cancel.ScanAsAServiceCancelDecider;
import com.synopsys.integration.blackduck.artifactory.modules.cancel.ScanCancelDecider;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModule;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.ArtifactoryInfoExternalIdExtractor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.ExternalIdService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.ComposerExternalIdExtractor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.ComposerJsonService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.ComposerVersionSelector;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.conda.CondaExternalIdExtractor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.ArtifactNotificationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.PolicyNotificationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.VulnerabilityNotificationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.NotificationProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.PolicyOverrideProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.PolicyRuleClearedProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.PolicyViolationProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.VulnerabilityProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.ArtifactInspectionService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.BlackDuckBOMService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.MetaDataUpdateService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.PolicySeverityService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.RepositoryInitializationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.util.ArtifactoryComponentService;
import com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServiceModule;
import com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServiceModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServicePropertyService;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModule;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanPropertyService;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScannerDirectoryUtil;
import com.synopsys.integration.blackduck.artifactory.modules.scan.service.ArtifactScanService;
import com.synopsys.integration.blackduck.artifactory.modules.scan.service.PostScanActionsService;
import com.synopsys.integration.blackduck.artifactory.modules.scan.service.RepositoryIdentificationService;
import com.synopsys.integration.blackduck.artifactory.modules.scan.service.ScanPolicyService;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.dataservice.ComponentService;
import com.synopsys.integration.blackduck.service.dataservice.NotificationService;
import com.synopsys.integration.blackduck.service.dataservice.ProjectBomService;
import com.synopsys.integration.blackduck.service.dataservice.ProjectService;
import com.synopsys.integration.exception.IntegrationException;

public class ModuleFactory {
    private final ConfigurationPropertyManager configurationPropertyManager;
    private final BlackDuckServerConfig blackDuckServerConfig;
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final ArtifactSearchService artifactSearchService;
    private final DateTimeManager dateTimeManager;
    private final BlackDuckServicesFactory blackDuckServicesFactory;
    private final Gson gson;
    private final DirectoryConfig directoryConfig;

    public ModuleFactory(
        ConfigurationPropertyManager configurationPropertyManager,
        BlackDuckServerConfig blackDuckServerConfig,
        ArtifactoryPAPIService artifactoryPAPIService,
        ArtifactoryPropertyService artifactoryPropertyService,
        ArtifactSearchService artifactSearchService,
        DateTimeManager dateTimeManager,
        BlackDuckServicesFactory blackDuckServicesFactory,
        Gson gson,
        DirectoryConfig directoryConfig
    ) {
        this.configurationPropertyManager = configurationPropertyManager;
        this.blackDuckServerConfig = blackDuckServerConfig;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.artifactSearchService = artifactSearchService;
        this.dateTimeManager = dateTimeManager;
        this.blackDuckServicesFactory = blackDuckServicesFactory;
        this.gson = gson;
        this.directoryConfig = directoryConfig;
    }

    public ScanModule createScanModule() throws IOException, IntegrationException {
        ScanModuleConfig scanModuleConfig = ScanModuleConfig.createFromProperties(configurationPropertyManager, artifactoryPAPIService, dateTimeManager);
        SimpleAnalyticsCollector simpleAnalyticsCollector = new SimpleAnalyticsCollector();
        ProjectService projectService = blackDuckServicesFactory.createProjectService();

        ScannerDirectoryUtil scannerDirectoryUtil = ScannerDirectoryUtil.createFromConfigs(directoryConfig, scanModuleConfig);
        scannerDirectoryUtil.createDirectories();

        RepositoryIdentificationService repositoryIdentificationService = new RepositoryIdentificationService(scanModuleConfig, dateTimeManager, artifactoryPropertyService, artifactoryPAPIService);
        ArtifactScanService artifactScanService = new ArtifactScanService(scanModuleConfig, blackDuckServerConfig, scannerDirectoryUtil, repositoryIdentificationService, artifactoryPropertyService, artifactoryPAPIService);
        ScanPolicyService scanPolicyService = ScanPolicyService.createDefault(blackDuckServerConfig, artifactoryPropertyService);
        PostScanActionsService postScanActionsService = new PostScanActionsService(artifactoryPropertyService, projectService);
        ScanPropertyService scanPropertyService = new ScanPropertyService(artifactoryPAPIService, dateTimeManager);
        ScanCancelDecider scanCancelDecider = new ScanCancelDecider(scanModuleConfig, scanPropertyService, artifactoryPAPIService);
        PolicyCancelDecider policyCancelDecider = new PolicyCancelDecider(artifactoryPropertyService, scanModuleConfig.getPolicyBlockedEnabled(), scanModuleConfig.getPolicyRepos(), scanModuleConfig.getPolicySeverityTypes());
        return new ScanModule(
            scanModuleConfig,
            repositoryIdentificationService,
            artifactScanService,
            artifactoryPAPIService,
            simpleAnalyticsCollector,
            scanPolicyService,
            postScanActionsService,
            scanPropertyService,
            scannerDirectoryUtil,
            Arrays.asList(scanCancelDecider, policyCancelDecider)
        );
    }

    public InspectionModule createInspectionModule() throws IOException {
        InspectionModuleConfig inspectionModuleConfig = InspectionModuleConfig.createFromProperties(configurationPropertyManager, artifactoryPAPIService);
        SimpleAnalyticsCollector simpleAnalyticsCollector = new SimpleAnalyticsCollector();

        BlackDuckApiClient blackDuckApiClient = blackDuckServicesFactory.getBlackDuckApiClient();
        ComponentService componentService = blackDuckServicesFactory.createComponentService();
        ProjectService projectService = blackDuckServicesFactory.createProjectService();
        ProjectBomService projectBomService = blackDuckServicesFactory.createProjectBomService();
        NotificationService notificationService = blackDuckServicesFactory.createNotificationService();
        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        PluginRepoPathFactory pluginRepoPathFactory = new PluginRepoPathFactory();

        InspectionPropertyService inspectionPropertyService = new InspectionPropertyService(artifactoryPAPIService, dateTimeManager, pluginRepoPathFactory, inspectionModuleConfig.getRetryCount());

        PolicyNotificationService policyNotificationService = new PolicyNotificationService(blackDuckApiClient, notificationService);
        PolicyOverrideProcessor policyOverrideProcessor = new PolicyOverrideProcessor(policyNotificationService);
        PolicyRuleClearedProcessor policyRuleClearedProcessor = new PolicyRuleClearedProcessor(policyNotificationService);
        PolicyViolationProcessor policyViolationProcessor = new PolicyViolationProcessor(policyNotificationService);
        VulnerabilityNotificationService vulnerabilityNotificationService = new VulnerabilityNotificationService(blackDuckApiClient, notificationService);
        VulnerabilityProcessor vulnerabilityProcessor = new VulnerabilityProcessor(vulnerabilityNotificationService);
        NotificationProcessor notificationProcessor = new NotificationProcessor(policyOverrideProcessor, policyRuleClearedProcessor, policyViolationProcessor, vulnerabilityProcessor);

        BlackDuckBOMService blackDuckBOMService = new BlackDuckBOMService(projectBomService, componentService, blackDuckApiClient);

        ArtifactoryInfoExternalIdExtractor artifactoryInfoExternalIdExtractor = new ArtifactoryInfoExternalIdExtractor(artifactoryPAPIService, externalIdFactory);
        ComposerVersionSelector composerVersionSelector = new ComposerVersionSelector(externalIdFactory);
        ComposerJsonService composerJsonService = new ComposerJsonService(artifactoryPAPIService, gson);
        ComposerExternalIdExtractor composerExternalIdExtractor = new ComposerExternalIdExtractor(composerVersionSelector, composerJsonService);
        CondaExternalIdExtractor condaExternalIdExtractor = new CondaExternalIdExtractor(externalIdFactory);
        ExternalIdService externalIdService = new ExternalIdService(artifactoryPAPIService, artifactoryInfoExternalIdExtractor, composerExternalIdExtractor, condaExternalIdExtractor);
        ArtifactoryComponentService artifactoryComponentService = new ArtifactoryComponentService(blackDuckServicesFactory.getRequestFactory(), blackDuckApiClient);
        ArtifactInspectionService artifactInspectionService = new ArtifactInspectionService(
            artifactoryPAPIService,
            blackDuckBOMService,
            inspectionModuleConfig,
            inspectionPropertyService,
            projectService,
            componentService,
            externalIdService,
            blackDuckApiClient,
            artifactoryComponentService
        );
        ArtifactNotificationService artifactNotificationService = new ArtifactNotificationService(artifactSearchService, inspectionPropertyService, artifactInspectionService, policyNotificationService, vulnerabilityNotificationService,
            notificationProcessor);
        MetaDataUpdateService metaDataUpdateService = new MetaDataUpdateService(inspectionPropertyService, artifactNotificationService);
        RepositoryInitializationService repositoryInitializationService = new RepositoryInitializationService(inspectionPropertyService, artifactoryPAPIService, inspectionModuleConfig, projectService);
        PolicySeverityService policySeverityService = new PolicySeverityService(artifactoryPropertyService, inspectionPropertyService, blackDuckApiClient, blackDuckBOMService, projectService);
        CancelDecider inspectionCancelDecider = new InspectionCancelDecider(inspectionModuleConfig.isMetadataBlockEnabled(), inspectionModuleConfig.getMetadataBlockRepos(), inspectionPropertyService, artifactInspectionService);
        CancelDecider policyCancelDecider = new PolicyCancelDecider(artifactoryPropertyService, inspectionModuleConfig.getPolicyBlockedEnabled(), inspectionModuleConfig.getPolicyRepos(), inspectionModuleConfig.getPolicySeverityTypes());

        return new InspectionModule(inspectionModuleConfig,
            artifactoryPAPIService,
            metaDataUpdateService,
            artifactoryPropertyService,
            inspectionPropertyService,
            simpleAnalyticsCollector,
            repositoryInitializationService,
            artifactInspectionService,
            policySeverityService,
            Arrays.asList(inspectionCancelDecider, policyCancelDecider)
        );
    }

    public AnalyticsModule createAnalyticsModule(AnalyticsService analyticsService, ModuleManager moduleManager) {
        AnalyticsModuleConfig analyticsModuleConfig = AnalyticsModuleConfig.createFromProperties(configurationPropertyManager);
        SimpleAnalyticsCollector simpleAnalyticsCollector = new SimpleAnalyticsCollector();

        return new AnalyticsModule(analyticsModuleConfig, analyticsService, simpleAnalyticsCollector, moduleManager);
    }

    public ScanAsAServiceModule createScanAsAServiceModule() throws IOException {
        ScanAsAServiceModuleConfig scanAsAServiceModuleConfig = ScanAsAServiceModuleConfig.createFromProperties(configurationPropertyManager, artifactoryPAPIService, dateTimeManager);
        ScanAsAServicePropertyService scanAsAServicePropertyService = new ScanAsAServicePropertyService(artifactoryPAPIService, dateTimeManager);
        PluginRepoPathFactory pluginRepoPathFactory = new PluginRepoPathFactory();
        ScanAsAServiceCancelDecider scanAsAServiceCancelDecider = new ScanAsAServiceCancelDecider(scanAsAServiceModuleConfig, scanAsAServicePropertyService, pluginRepoPathFactory, artifactoryPAPIService);
        return new ScanAsAServiceModule(scanAsAServiceModuleConfig,
                artifactoryPropertyService,
                artifactoryPAPIService,
                Arrays.asList(scanAsAServiceCancelDecider));
    }
}
