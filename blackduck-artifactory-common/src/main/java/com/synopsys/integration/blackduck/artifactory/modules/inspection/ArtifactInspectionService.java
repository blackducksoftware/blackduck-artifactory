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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.Artifact;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.ComponentViewWrapper;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ArtifactInspectionService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final BlackDuckBOMService blackDuckBOMService;
    private final MetaDataPopulationService metaDataPopulationService;
    private final InspectionModuleConfig inspectionModuleConfig;
    private final PackageTypePatternManager packageTypePatternManager;
    private final CacheInspectorService cacheInspectorService;
    private final ProjectService projectService;
    private final ArtifactoryExternalIdFactory artifactoryExternalIdFactory;

    public ArtifactInspectionService(final ArtifactoryPAPIService artifactoryPAPIService, final BlackDuckBOMService blackDuckBOMService,
        final MetaDataPopulationService metaDataPopulationService, final InspectionModuleConfig inspectionModuleConfig,
        final PackageTypePatternManager packageTypePatternManager, final CacheInspectorService cacheInspectorService, final ProjectService projectService,
        final ArtifactoryExternalIdFactory artifactoryExternalIdFactory) {
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.blackDuckBOMService = blackDuckBOMService;
        this.metaDataPopulationService = metaDataPopulationService;
        this.inspectionModuleConfig = inspectionModuleConfig;
        this.packageTypePatternManager = packageTypePatternManager;
        this.cacheInspectorService = cacheInspectorService;
        this.projectService = projectService;
        this.artifactoryExternalIdFactory = artifactoryExternalIdFactory;
    }

    public boolean shouldInspectArtifact(final RepoPath repoPath) {
        if (!inspectionModuleConfig.getRepos().contains(repoPath.getRepoKey())) {
            return false;
        }

        final ItemInfo itemInfo = artifactoryPAPIService.getItemInfo(repoPath);
        final Optional<List<String>> patterns = artifactoryPAPIService.getPackageType(repoPath.getRepoKey())
                                                    .map(packageTypePatternManager::getPatterns)
                                                    .filter(Optional::isPresent)
                                                    .map(Optional::get);

        if (!patterns.isPresent() || patterns.get().isEmpty() || itemInfo.isFolder()) {
            return false;
        }

        final File artifact = new File(itemInfo.getName());
        final WildcardFileFilter wildcardFileFilter = new WildcardFileFilter(patterns.get());

        return wildcardFileFilter.accept(artifact);
    }

    public void identifyAndMarkArtifact(final RepoPath repoPath) {
        final String repoKey = repoPath.getRepoKey();
        final Optional<String> packageType = artifactoryPAPIService.getPackageType(repoKey);
        if (packageType.isPresent()) {
            identifyAndMarkArtifact(repoPath, packageType.get());
        } else {
            logger.warn(String.format("The repository '%s' has no package type. Inspection cannot be performed on artifact: %s", repoKey, repoPath.getPath()));
        }
    }

    public Artifact identifyAndMarkArtifact(final RepoPath repoPath, final String packageType) {
        final Artifact artifact = identifyArtifact(repoPath, packageType);
        metaDataPopulationService.populateExternalIdMetadata(artifact);
        return artifact;
    }

    private Artifact identifyArtifact(final RepoPath repoPath, final String packageType) {
        final FileLayoutInfo fileLayoutInfo = artifactoryPAPIService.getLayoutInfo(repoPath);
        final org.artifactory.md.Properties properties = artifactoryPAPIService.getProperties(repoPath);
        final Optional<ExternalId> possibleExternalId = artifactoryExternalIdFactory.createExternalId(packageType, fileLayoutInfo, repoPath, properties);
        final ExternalId externalId = possibleExternalId.orElse(null);

        return new Artifact(repoPath, externalId);
    }

    public void inspectDelta(final String repoKey) {
        final RepoPath repoKeyPath = RepoPathFactory.create(repoKey);
        if (!cacheInspectorService.assertInspectionStatus(repoKeyPath, InspectionStatus.SUCCESS)) {
            // Only inspect a delta if the repository has been successfully initialized
            return;
        }

        final Optional<String> packageType = artifactoryPAPIService.getPackageType(repoKey);
        if (!packageType.isPresent()) {
            final String message = String.format("The repository '%s' has no package type. Inspection cannot be performed.", repoKey);
            logger.error(message);
            cacheInspectorService.failInspection(repoKeyPath, message);
            return;
        }

        final List<String> patterns = packageTypePatternManager.getPatternsForPackageType(packageType.get());
        if (patterns.isEmpty()) {
            // If we don't verify that patterns is not empty, artifactory will grab every artifact in the repo.
            logger.warn(String.format("The repository '%s' has a package type of '%s' which either isn't supported or has no patterns configured for it.", repoKey, packageType.get()));
            cacheInspectorService.failInspection(repoKeyPath, "Repo has an unsupported package type or not patterns configured for it.");
            return;
        }

        final ProjectVersionView projectVersionView;
        try {
            final String projectName = cacheInspectorService.getRepoProjectName(repoKey);
            final String projectVersionName = cacheInspectorService.getRepoProjectVersionName(repoKey);
            final Optional<ProjectVersionWrapper> projectVersionWrapperOptional = projectService.getProjectVersion(projectName, projectVersionName);

            if (projectVersionWrapperOptional.isPresent()) {
                projectVersionView = projectVersionWrapperOptional.get().getProjectVersionView();
            } else {
                throw new IntegrationException(String.format("Project '%s' and version '%s' could not be found.", projectName, projectVersionName));
            }
        } catch (final IntegrationException e) {
            logger.error("An error occurred when attempting to get the project version from Black Duck.", e);
            cacheInspectorService.failInspection(repoKeyPath, e.getMessage());
            return;
        }

        final List<Artifact> artifacts = artifactoryPAPIService.searchForArtifactsByPatterns(Collections.singletonList(repoKey), patterns).stream()
                                             .filter(this::isArtifactPendingOrShouldRetry)
                                             .map(repoPath -> identifyArtifact(repoPath, packageType.get()))
                                             .collect(Collectors.toList());

        for (final Artifact artifact : artifacts) {
            final Optional<ExternalId> externalId = metaDataPopulationService.populateExternalIdMetadata(artifact.getRepoPath(), artifact.getExternalId().orElse(null));
            if (!externalId.isPresent()) {
                // Inspection already failed in metadataPopulationService::populateExternalIdMetadata
                return;
            }

            try {
                final ComponentViewWrapper componentViewWrapper = blackDuckBOMService.addIdentifiedArtifactToProjectVersion(artifact, projectVersionView);
                metaDataPopulationService.populateBlackDuckMetadata(artifact.getRepoPath(), componentViewWrapper.getComponentVersionView(), componentViewWrapper.getVersionBomComponentView());
            } catch (final IntegrationException e) {
                cacheInspectorService.failInspection(artifact.getRepoPath(), e.getMessage());
            }
        }

    }

    private boolean isArtifactPendingOrShouldRetry(final RepoPath repoPath) {
        return cacheInspectorService.assertInspectionStatus(repoPath, InspectionStatus.PENDING) || cacheInspectorService.shouldRetryInspection(repoPath);
    }
}
