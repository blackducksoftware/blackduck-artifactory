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
package com.synopsys.integration.blackduck.artifactory.modules.inspection;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.SimpleAnalyticsCollector;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class InspectionModule implements Module {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final InspectionModuleConfig inspectionModuleConfig;
    private final ArtifactIdentificationService artifactIdentificationService;
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final MetaDataPopulationService metaDataPopulationService;
    private final MetaDataUpdateService metaDataUpdateService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final CacheInspectorService cacheInspectorService;
    private final SimpleAnalyticsCollector simpleAnalyticsCollector;

    public InspectionModule(final InspectionModuleConfig inspectionModuleConfig, final ArtifactIdentificationService artifactIdentificationService, final ArtifactoryPAPIService artifactoryPAPIService,
        final MetaDataPopulationService metaDataPopulationService, final MetaDataUpdateService metaDataUpdateService, final ArtifactoryPropertyService artifactoryPropertyService,
        final CacheInspectorService cacheInspectorService, final SimpleAnalyticsCollector simpleAnalyticsCollector) {
        this.inspectionModuleConfig = inspectionModuleConfig;
        this.artifactIdentificationService = artifactIdentificationService;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.metaDataPopulationService = metaDataPopulationService;
        this.metaDataUpdateService = metaDataUpdateService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.cacheInspectorService = cacheInspectorService;
        this.simpleAnalyticsCollector = simpleAnalyticsCollector;
    }

    @Override
    public InspectionModuleConfig getModuleConfig() {
        return inspectionModuleConfig;
    }

    public void identifyArtifacts() {
        inspectionModuleConfig.getRepos()
            .forEach(artifactIdentificationService::identifyArtifacts);
        updateAnalytics();
    }

    public void populateMetadata() {
        inspectionModuleConfig.getRepos()
            .forEach(metaDataPopulationService::populateMetadata);
        updateAnalytics();
    }

    public void updateMetadata() {
        inspectionModuleConfig.getRepos()
            .forEach(metaDataUpdateService::updateMetadata);
        updateAnalytics();
    }

    public void deleteInspectionProperties(final Map<String, List<String>> params) {
        inspectionModuleConfig.getRepos()
            .forEach(repoKey -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepo(repoKey, params, logger));
        updateAnalytics();
    }

    public void reinspectFromFailures(final Map<String, List<String>> params) {
        final List<RepoPath> repoPaths = inspectionModuleConfig.getRepos().stream()
                                             .map(repoKey -> cacheInspectorService.getAllArtifactsInRepoWithInspectionStatus(repoKey, InspectionStatus.FAILURE))
                                             .flatMap(Collection::stream)
                                             .collect(Collectors.toList());

        repoPaths.forEach(repoPath -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepoPath(repoPath, params, logger));
        repoPaths.forEach(this::inspectArtifact);

        inspectionModuleConfig.getRepos().stream()
            .map(RepoPathFactory::create)
            .filter(repoPath -> cacheInspectorService.assertInspectionStatus(repoPath, InspectionStatus.FAILURE))
            .forEach(repoPath -> cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.SUCCESS));

        updateAnalytics();
    }

    public void updateDeprecatedProperties() {
        inspectionModuleConfig.getRepos()
            .forEach(repoKey -> artifactoryPropertyService.updateAllBlackDuckPropertiesFromRepoKey(repoKey, logger));
        updateAnalytics();
    }

    public void handleAfterCreateEvent(final ItemInfo itemInfo) {
        final RepoPath repoPath = itemInfo.getRepoPath();
        handleStorageEvent(repoPath);
    }

    public void handleAfterCopyEvent(final RepoPath targetRepoPath) {
        handleStorageEvent(targetRepoPath);
    }

    private void handleStorageEvent(final RepoPath repoPath) {
        try {
            if (artifactIdentificationService.shouldInspectArtifact(inspectionModuleConfig.getRepos(), repoPath)) {
                inspectArtifact(repoPath);
            } else {
                logger.debug(String.format("Artifact at '%s' is not existent, the repo is not configured to be inspected, or the artifact doesn't have a matching pattern", repoPath.toPath()));
            }
        } catch (final Exception e) {
            logger.error(String.format("Failed to inspect artifact added to storage: %s", repoPath.toPath()));
            cacheInspectorService.failInspection(repoPath, "See logs for details");
            logger.debug(e.getMessage(), e);
        }

        updateAnalytics();
    }

    private void inspectArtifact(final RepoPath repoPath) {
        final String repoKey = repoPath.getRepoKey();
        final Optional<String> packageType = artifactoryPAPIService.getPackageType(repoKey);
        if (packageType.isPresent()) {
            final ArtifactIdentificationService.IdentifiedArtifact identifiedArtifact = artifactIdentificationService.identifyArtifact(repoPath, packageType.get());
            artifactIdentificationService.populateIdMetadataOnIdentifiedArtifact(identifiedArtifact);
        } else {
            logger.debug(String.format("Package type for repo '%s' is not existent", repoKey));
        }
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
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
