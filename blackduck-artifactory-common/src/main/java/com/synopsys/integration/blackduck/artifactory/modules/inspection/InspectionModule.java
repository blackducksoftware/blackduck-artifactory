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
package com.synopsys.integration.blackduck.artifactory.modules.inspection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.artifactory.exception.CancelException;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.UpdateStatus;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.SimpleAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.ArtifactInspectionService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.MetaDataUpdateService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.PolicySeverityService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.RepositoryInitializationService;
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

    public InspectionModule(final InspectionModuleConfig inspectionModuleConfig, final ArtifactoryPAPIService artifactoryPAPIService, final MetaDataUpdateService metaDataUpdateService,
        final ArtifactoryPropertyService artifactoryPropertyService, final InspectionPropertyService inspectionPropertyService, final SimpleAnalyticsCollector simpleAnalyticsCollector,
        final RepositoryInitializationService repositoryInitializationService, final ArtifactInspectionService artifactInspectionService,
        final PolicySeverityService policySeverityService) {
        this.inspectionModuleConfig = inspectionModuleConfig;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.metaDataUpdateService = metaDataUpdateService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.inspectionPropertyService = inspectionPropertyService;
        this.simpleAnalyticsCollector = simpleAnalyticsCollector;
        this.repositoryInitializationService = repositoryInitializationService;
        this.artifactInspectionService = artifactInspectionService;
        this.policySeverityService = policySeverityService;
    }

    @Override
    public InspectionModuleConfig getModuleConfig() {
        return inspectionModuleConfig;
    }

    // TODO: Remove in 9.0.0
    public void performNpmForgeUpgrade() {
        final List<RepoPath> repoPaths = artifactoryPAPIService.itemsByProperties(
            ImmutableSetMultimap.of(BlackDuckArtifactoryProperty.BLACKDUCK_FORGE.getName(), "npm"),
            inspectionModuleConfig.getRepos().toArray(new String[0])
        );
        for (final RepoPath repoPath : repoPaths) {
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_FORGE, SupportedPackageType.NPM.getForge().getName(), logger);
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS, InspectionStatus.PENDING.name(), logger);
        }
    }

    //////////////////////// New cron jobs ////////////////////////
    public void initializeRepositories() {
        inspectionModuleConfig.getRepos().forEach(repositoryInitializationService::initializeRepository);
    }

    public void reinspectFromFailures() {
        final Map<String, List<String>> params = new HashMap<>();
        params.put("properties", Arrays.asList(BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME.getName(), BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME.getName()));
        reinspectFromFailures(params);
    }

    public void performPolicySeverityUpdate() {
        inspectionModuleConfig.getRepos().forEach(policySeverityService::performPolicySeverityUpgrade);
    }

    //////////////////////// Old cron jobs ////////////////////////

    public void inspectAllUnknownArtifacts() {
        inspectionModuleConfig.getRepos().forEach(artifactInspectionService::inspectAllUnknownArtifacts);
    }

    public void updateMetadata() {
        final List<RepoPath> repoKeyPaths = inspectionModuleConfig.getRepos().stream()
                                                .map(RepoPathFactory::create)
                                                .collect(Collectors.toList());
        metaDataUpdateService.updateMetadata(repoKeyPaths);
    }

    //////////////////////// Endpoints ////////////////////////

    public void deleteInspectionProperties(final Map<String, List<String>> params) {
        inspectionModuleConfig.getRepos()
            .forEach(repoKey -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepo(repoKey, params, logger));
    }

    public void deleteInspectionPropertiesFromOutOfDate(final Map<String, List<String>> params) {
        inspectionModuleConfig.getRepos()
            .forEach(repoKey -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepo(repoKey, params, logger));

        final SetMultimap<String, String> propertyMap = ImmutableSetMultimap.of(BlackDuckArtifactoryProperty.UPDATE_STATUS.getName(), UpdateStatus.OUT_OF_DATE.name());
        final String[] repoKeys = inspectionModuleConfig.getRepos().toArray(new String[0]);
        final List<RepoPath> repoPaths = artifactoryPropertyService.getAllItemsInRepoWithPropertiesAndValues(propertyMap, repoKeys);
        repoPaths.forEach(repoPath -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepoPath(repoPath, params, logger));
    }

    public void reinspectFromFailures(final Map<String, List<String>> params) {
        final List<RepoPath> repoPaths = inspectionModuleConfig.getRepos().stream()
                                             .map(repoKey -> inspectionPropertyService.getAllArtifactsInRepoWithInspectionStatus(repoKey, InspectionStatus.FAILURE))
                                             .flatMap(Collection::stream).collect(Collectors.toList());

        repoPaths.forEach(repoPath -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepoPath(repoPath, params, logger));
        repoPaths.stream()
            .filter(artifactInspectionService::shouldInspectArtifact)
            .forEach(artifactInspectionService::inspectSingleArtifact);
    }

    //////////////////////// Event Listeners ////////////////////////

    public void handleAfterCreateEvent(final ItemInfo itemInfo) {
        final RepoPath repoPath = itemInfo.getRepoPath();
        handleStorageEvent(repoPath);
    }

    public void handleAfterCopyEvent(final RepoPath targetRepoPath) {
        handleStorageEvent(targetRepoPath);
    }

    public void handleAfterMoveEvent(final RepoPath targetRepoPath) {
        handleStorageEvent(targetRepoPath);
    }

    private void handleStorageEvent(final RepoPath repoPath) {
        if (artifactInspectionService.shouldInspectArtifact(repoPath)) {
            artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepoPath(repoPath, new HashMap<>(), logger);
            artifactInspectionService.inspectSingleArtifact(repoPath);
        } else {
            logger.debug(String.format("Artifact at '%s' is not existent, the repo is not configured to be inspected, or the artifact doesn't have a matching pattern", repoPath.toPath()));
        }
    }

    public void handleBeforeDownloadEvent(final RepoPath repoPath) {
        final Optional<InspectionStatus> inspectionStatus = inspectionPropertyService.getInspectionStatus(repoPath);
        final boolean shouldCancelDownload = inspectionModuleConfig.isMetadataBlockEnabled()
                                                 && (!inspectionStatus.isPresent() || inspectionStatus.get().equals(InspectionStatus.PENDING))
                                                 && artifactInspectionService.shouldInspectArtifact(repoPath);

        if (shouldCancelDownload) {
            throw new CancelException(String.format("The Black Duck %s has prevented the download of %s because it lacks blackduck notifications", InspectionModule.class.getSimpleName(), repoPath.toPath()), 403);
        }
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        updateAnalytics();
        return Collections.singletonList(simpleAnalyticsCollector);
    }

    private void updateAnalytics() {
        final List<String> cacheRepositoryKeys = inspectionModuleConfig.getRepos();
        simpleAnalyticsCollector.putMetadata("cache.repo.count", cacheRepositoryKeys.size());
        simpleAnalyticsCollector.putMetadata("cache.artifact.count", artifactoryPAPIService.getArtifactCount(cacheRepositoryKeys));

        final String packageManagers = cacheRepositoryKeys.stream()
                                           .map(artifactoryPAPIService::getPackageType)
                                           .filter(Optional::isPresent)
                                           .map(Optional::get)
                                           .collect(Collectors.joining("/"));
        simpleAnalyticsCollector.putMetadata("cache.package.managers", packageManagers);
    }
}
