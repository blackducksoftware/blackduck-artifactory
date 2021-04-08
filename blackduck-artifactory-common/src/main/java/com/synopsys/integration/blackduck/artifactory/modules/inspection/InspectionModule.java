/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSetMultimap;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.UpdateStatus;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.SimpleAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.cancel.CancelDecider;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.ArtifactInspectionService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.MetaDataUpdateService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.PolicySeverityService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.RepositoryInitializationService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class InspectionModule implements Module {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final InspectionModuleConfig inspectionModuleConfig;
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final MetaDataUpdateService metaDataUpdateService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final InspectionPropertyService inspectionPropertyService;
    private final SimpleAnalyticsCollector simpleAnalyticsCollector;
    private final RepositoryInitializationService repositoryInitializationService;
    private final ArtifactInspectionService artifactInspectionService;
    private final PolicySeverityService policySeverityService;
    private final CancelDecider cancelDecider;

    public InspectionModule(InspectionModuleConfig inspectionModuleConfig, ArtifactoryPAPIService artifactoryPAPIService, MetaDataUpdateService metaDataUpdateService, ArtifactoryPropertyService artifactoryPropertyService,
        InspectionPropertyService inspectionPropertyService, SimpleAnalyticsCollector simpleAnalyticsCollector, RepositoryInitializationService repositoryInitializationService, ArtifactInspectionService artifactInspectionService,
        PolicySeverityService policySeverityService, CancelDecider cancelDecider) {
        this.inspectionModuleConfig = inspectionModuleConfig;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.metaDataUpdateService = metaDataUpdateService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.inspectionPropertyService = inspectionPropertyService;
        this.simpleAnalyticsCollector = simpleAnalyticsCollector;
        this.repositoryInitializationService = repositoryInitializationService;
        this.artifactInspectionService = artifactInspectionService;
        this.policySeverityService = policySeverityService;
        this.cancelDecider = cancelDecider;
    }

    @Override
    public InspectionModuleConfig getModuleConfig() {
        return inspectionModuleConfig;
    }

    //////////////////////// Upgrade Executions ////////////////////////

    // TODO: Remove upgrades in 9.0.0
    public void performUpgrades() {
        performComponentNameVersionUpgrade();
        performPolicySeverityUpdate();
    }

    // TODO: Remove in 9.0.0
    public void performComponentNameVersionUpgrade() {
        for (String repoKey : inspectionModuleConfig.getRepos()) {
            List<RepoPath> repoPaths = artifactoryPropertyService.getItemsContainingPropertiesAndValues(ImmutableSetMultimap.of(BlackDuckArtifactoryProperty.BLACKDUCK_FORGE.getPropertyName(), "*"), repoKey);

            try {
                ProjectVersionView projectVersionView = artifactInspectionService.fetchProjectVersionWrapper(repoKey).getProjectVersionView();
                for (RepoPath repoPath : repoPaths) {
                    if (inspectionPropertyService.assertInspectionStatus(repoPath, InspectionStatus.SUCCESS) && !inspectionPropertyService.hasExternalIdProperties(repoPath)) {
                        logger.debug(String.format("Performing componentNameVersion upgrade on artifact: %s", repoPath.toPath()));
                        artifactInspectionService.inspectSingleArtifact(repoPath, projectVersionView);
                    }
                }
            } catch (IntegrationException e) {
                logger.debug(String.format("Failed to perform componentNameVersion upgrade for repo '%s'.", repoKey), e);
            }
        }
    }

    public void performPolicySeverityUpdate() {
        inspectionModuleConfig.getRepos().forEach(policySeverityService::performPolicySeverityUpgrade);
    }

    //////////////////////// New cron jobs ////////////////////////
    public void initializeRepositories() {
        inspectionModuleConfig.getRepos().forEach(repositoryInitializationService::initializeRepository);
    }

    public void reinspectFromFailures() {
        Map<String, List<String>> params = new HashMap<>();
        params.put("properties", Arrays.asList(BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME.getPropertyName(), BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME.getPropertyName()));
        reinspectFromFailures(params);
    }

    //////////////////////// Old cron jobs ////////////////////////

    public void inspectAllUnknownArtifacts() {
        inspectionModuleConfig.getRepos().forEach(artifactInspectionService::inspectAllUnknownArtifacts);
    }

    public void updateMetadata() {
        List<RepoPath> repoKeyPaths = inspectionModuleConfig.getRepos().stream()
                                          .map(RepoPathFactory::create)
                                          .collect(Collectors.toList());
        metaDataUpdateService.updateMetadata(repoKeyPaths);
    }

    //////////////////////// Endpoints ////////////////////////

    public void deleteInspectionProperties(Map<String, List<String>> params) {
        inspectionModuleConfig.getRepos()
            .forEach(repoKey -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepo(repoKey, params, logger));
    }

    public void deleteInspectionPropertiesFromOutOfDate(Map<String, List<String>> params) {
        inspectionModuleConfig.getRepos().stream()
            .filter(repoKey -> inspectionPropertyService.assertUpdateStatus(RepoPathFactory.create(repoKey), UpdateStatus.OUT_OF_DATE))
            .forEach(repoKey -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepo(repoKey, params, logger));
    }

    public void reinspectFromFailures(Map<String, List<String>> params) {
        List<RepoPath> repoPaths = inspectionModuleConfig.getRepos().stream()
                                       .map(repoKey -> inspectionPropertyService.getAllArtifactsInRepoWithInspectionStatus(repoKey, InspectionStatus.FAILURE))
                                       .flatMap(Collection::stream).collect(Collectors.toList());

        repoPaths.forEach(repoPath -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepoPath(repoPath, params, logger));
        repoPaths.stream()
            .filter(artifactInspectionService::shouldInspectArtifact)
            .forEach(artifactInspectionService::inspectSingleArtifact);
    }

    //////////////////////// Event Listeners ////////////////////////

    public void handleAfterCreateEvent(ItemInfo itemInfo) {
        RepoPath repoPath = itemInfo.getRepoPath();
        handleStorageEvent(repoPath);
    }

    public void handleAfterCopyEvent(RepoPath targetRepoPath) {
        handleStorageEvent(targetRepoPath);
    }

    public void handleAfterMoveEvent(RepoPath targetRepoPath) {
        handleStorageEvent(targetRepoPath);
    }

    private void handleStorageEvent(RepoPath repoPath) {
        if (artifactInspectionService.shouldInspectArtifact(repoPath)) {
            artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepoPath(repoPath, new HashMap<>(), logger);
            artifactInspectionService.inspectSingleArtifact(repoPath);
        } else {
            logger.debug(String.format("Artifact at '%s' is not existent, the repo is not configured to be inspected, or the artifact doesn't have a matching pattern", repoPath.toPath()));
        }
    }

    public void handleBeforeDownloadEvent(RepoPath repoPath) {
        cancelDecider.handleBeforeDownloadEvent(repoPath);
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        updateAnalytics();
        return Collections.singletonList(simpleAnalyticsCollector);
    }

    private void updateAnalytics() {
        List<String> cacheRepositoryKeys = inspectionModuleConfig.getRepos();
        simpleAnalyticsCollector.putMetadata("cache.repo.count", cacheRepositoryKeys.size());
        simpleAnalyticsCollector.putMetadata("cache.artifact.count", artifactoryPAPIService.getArtifactCount(cacheRepositoryKeys));

        String packageManagers = cacheRepositoryKeys.stream()
                                     .map(artifactoryPAPIService::getPackageType)
                                     .filter(Optional::isPresent)
                                     .map(Optional::get)
                                     .collect(Collectors.joining("/"));
        simpleAnalyticsCollector.putMetadata("cache.package.managers", packageManagers);
    }
}
