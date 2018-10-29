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
package com.synopsys.integration.blackduck.artifactory.modules.inspection;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.artifactory.repo.RepositoryConfiguration;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.Analyzable;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.SimpleAnalyticsCollector;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class InspectionModule implements Analyzable, Module {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final InspectionModuleConfig inspectionModuleConfig;
    private final ArtifactIdentificationService artifactIdentificationService;
    private final MetaDataPopulationService metaDataPopulationService;
    private final MetaDataUpdateService metaDataUpdateService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final Repositories repositories;
    private final SimpleAnalyticsCollector simpleAnalyticsCollector;

    public InspectionModule(final InspectionModuleConfig inspectionModuleConfig, final ArtifactIdentificationService artifactIdentificationService, final MetaDataPopulationService metaDataPopulationService,
        final MetaDataUpdateService metaDataUpdateService, final ArtifactoryPropertyService artifactoryPropertyService, final Repositories repositories,
        final SimpleAnalyticsCollector simpleAnalyticsCollector) {
        this.inspectionModuleConfig = inspectionModuleConfig;
        this.artifactIdentificationService = artifactIdentificationService;
        this.metaDataPopulationService = metaDataPopulationService;
        this.metaDataUpdateService = metaDataUpdateService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.repositories = repositories;
        this.simpleAnalyticsCollector = simpleAnalyticsCollector;
    }

    public InspectionModuleConfig getModuleConfig() {
        return inspectionModuleConfig;
    }

    public void identifyArtifacts() {
        inspectionModuleConfig.getRepoKeys()
            .forEach(artifactIdentificationService::identifyArtifacts);
        updateAnalytics();
    }

    public void populateMetadata() {
        inspectionModuleConfig.getRepoKeys()
            .forEach(metaDataPopulationService::populateMetadata);
        updateAnalytics();
    }

    public void updateMetadata() {
        inspectionModuleConfig.getRepoKeys()
            .forEach(metaDataUpdateService::updateMetadata);
        updateAnalytics();
    }

    public void deleteInspectionProperties(final Map<String, List<String>> params) {
        inspectionModuleConfig.getRepoKeys()
            .forEach(repoKey -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepo(repoKey, params));
        updateAnalytics();
    }

    public void updateDeprecatedProperties() {
        inspectionModuleConfig.getRepoKeys()
            .forEach(artifactoryPropertyService::updateAllBlackDuckPropertiesFromRepoKey);
        updateAnalytics();
    }

    public boolean handleAfterCreateEvent(final ItemInfo itemInfo) {
        final String repoKey = itemInfo.getRepoKey();
        final RepoPath repoPath = itemInfo.getRepoPath();

        boolean successfulInspection;
        try {
            final String packageType = repositories.getRepositoryConfiguration(repoKey).getPackageType();

            if (inspectionModuleConfig.getRepoKeys().contains(repoKey)) {
                final Optional<Set<RepoPath>> identifiableArtifacts = artifactIdentificationService.getIdentifiableArtifacts(repoKey);

                if (identifiableArtifacts.isPresent() && identifiableArtifacts.get().contains(repoPath)) {
                    final Optional<ArtifactIdentificationService.IdentifiedArtifact> optionalIdentifiedArtifact = artifactIdentificationService.identifyArtifact(repoPath, packageType);
                    optionalIdentifiedArtifact.ifPresent(artifactIdentificationService::populateIdMetadataOnIdentifiedArtifact);
                }
            }
            successfulInspection = true;
        } catch (final Exception e) {
            logger.error(String.format("Failed to inspect item added to storage: %s", repoPath.toPath()));
            logger.debug(e.getMessage(), e);
            successfulInspection = false;
        }

        updateAnalytics();

        return successfulInspection;
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return Arrays.asList(simpleAnalyticsCollector);
    }

    private void updateAnalytics() {
        final List<String> cacheRepositoryKeys = inspectionModuleConfig.getRepoKeys();
        simpleAnalyticsCollector.putMetadata("cache.repo.count", cacheRepositoryKeys.size());
        simpleAnalyticsCollector.putMetadata("cache.artifact.count", artifactIdentificationService.getArtifactCount(cacheRepositoryKeys));
        simpleAnalyticsCollector.putMetadata("cache.package.managers", StringUtils.join(getPackageManagers(cacheRepositoryKeys), "/"));
    }

    private List<String> getPackageManagers(final List<String> repoKeys) {
        return repoKeys.stream()
                   .map(repositories::getRepositoryConfiguration)
                   .map(RepositoryConfiguration::getPackageType)
                   .collect(Collectors.toList());
    }
}
