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
package com.synopsys.integration.blackduck.artifactory.modules.inspection;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.repo.Repositories;
import org.artifactory.repo.RepositoryConfiguration;
import org.artifactory.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.BlackDuckConnectionService;
import com.synopsys.integration.blackduck.exception.HubIntegrationException;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.SimpleBdioFactory;
import com.synopsys.integration.hub.bdio.graph.MutableDependencyGraph;
import com.synopsys.integration.hub.bdio.model.Forge;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.hub.bdio.model.dependency.Dependency;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;
import com.synopsys.integration.rest.exception.IntegrationRestException;
import com.synopsys.integration.util.IntegrationEscapeUtil;

public class ArtifactIdentificationService {
    private final Logger logger = LoggerFactory.getLogger(CacheInspectorService.class);

    private final Repositories repositories;
    private final Searches searches;
    private final PackageTypePatternManager packageTypePatternManager;
    private final ArtifactoryExternalIdFactory artifactoryExternalIdFactory;

    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final CacheInspectorService cacheInspectorService;
    private final BlackDuckConnectionService blackDuckConnectionService;

    public ArtifactIdentificationService(final Repositories repositories, final Searches searches, final PackageTypePatternManager packageTypePatternManager, final ArtifactoryExternalIdFactory artifactoryExternalIdFactory,
        final ArtifactoryPropertyService artifactoryPropertyService, final CacheInspectorService cacheInspectorService, final BlackDuckConnectionService blackDuckConnectionService) {
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.cacheInspectorService = cacheInspectorService;
        this.blackDuckConnectionService = blackDuckConnectionService;
        this.packageTypePatternManager = packageTypePatternManager;
        this.artifactoryExternalIdFactory = artifactoryExternalIdFactory;
        this.repositories = repositories;
        this.searches = searches;
    }

    public void identifyArtifacts(final String repoKey) {
        final RepoPath repoKeyPath = RepoPathFactory.create(repoKey);
        final Optional<InspectionStatus> repositoryStatus = cacheInspectorService.getInspectionStatus(repoKeyPath);

        try {
            final Optional<Set<RepoPath>> matchedArtifacts = getIdentifiableArtifacts(repoKey);

            if (matchedArtifacts.isPresent()) {
                final RepositoryConfiguration repositoryConfiguration = repositories.getRepositoryConfiguration(repoKey);
                final String packageType = repositoryConfiguration.getPackageType();
                final String projectName = cacheInspectorService.getRepoProjectName(repoKey);
                final String projectVersionName = cacheInspectorService.getRepoProjectVersionName(repoKey);

                if (repositoryStatus.isPresent() && repositoryStatus.get().equals(InspectionStatus.SUCCESS)) {
                    addDeltaToHubProject(projectName, projectVersionName, packageType, matchedArtifacts.get());
                } else if (!repositoryStatus.isPresent()) {
                    createHubProjectFromRepo(projectName, projectVersionName, packageType, matchedArtifacts.get());
                    cacheInspectorService.setInspectionStatus(repoKeyPath, InspectionStatus.PENDING);
                }
            } else {
                logger.warn(String.format(
                    "The %s could not identify artifacts in repository %s because no supported patterns were found. The repository either uses an unsupported package manager or no patterns were configured for it.",
                    InspectionModule.class.getSimpleName(), repoKey));
            }

        } catch (final Exception e) {
            logger.error(String.format("The blackDuckCacheInspector encountered an exception while identifying artifacts in repository %s", repoKey), e);
            cacheInspectorService.setInspectionStatus(repoKeyPath, InspectionStatus.FAILURE);
        }
    }

    public Optional<IdentifiedArtifact> identifyArtifact(final RepoPath repoPath, final String packageType) {
        IdentifiedArtifact identifiedArtifact = null;

        final FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath);
        final org.artifactory.md.Properties properties = repositories.getProperties(repoPath);
        final Optional<ExternalId> possibleExternalId = artifactoryExternalIdFactory.createExternalId(packageType, fileLayoutInfo, properties);
        if (possibleExternalId.isPresent()) {
            identifiedArtifact = new IdentifiedArtifact(repoPath, possibleExternalId.get());
        }

        return Optional.ofNullable(identifiedArtifact);
    }

    public void populateIdMetadataOnIdentifiedArtifact(final IdentifiedArtifact identifiedArtifact) {
        final ExternalId externalId = identifiedArtifact.getExternalId();
        final RepoPath repoPath = identifiedArtifact.getRepoPath();

        final String hubOriginId = externalId.createHubOriginId();
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID, hubOriginId);
        final String hubForge = externalId.forge.getName();
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_FORGE, hubForge);

        cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.PENDING);
    }

    public void addIdentifiedArtifactToProjectVersion(final IdentifiedArtifact artifact, final String projectName, final String projectVersionName) {
        final RepoPath repoPath = artifact.getRepoPath();

        try {
            blackDuckConnectionService.addComponentToProjectVersion(artifact.getExternalId(), projectName, projectVersionName);
            cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.SUCCESS);
        } catch (final IntegrationRestException e) {
            final int statusCode = e.getHttpStatusCode();
            if (statusCode == 412) {
                logger.info(String.format("Unable to add manual BOM component because it already exists: %s", repoPath));
                cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.SUCCESS);
            } else if (statusCode == 401) {
                logger.warn(String.format("The Black Duck %s could not successfully inspect %s because plugin is unauthorized (%d). Ensure the plugin is configured with the correct credentials", InspectionModule.class.getSimpleName(),
                    repoPath, statusCode));
                cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.FAILURE);
            } else {
                logger.warn(String.format("The Black Duck %s could not successfully inspect %s because of a %d status code", InspectionModule.class.getSimpleName(), repoPath, statusCode));
                logger.debug(String.format(e.getMessage(), repoPath), e);
                cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.FAILURE);
            }
        } catch (final HubIntegrationException e) {
            // TODO: Can't find exact match in hub error.
        } catch (final Exception e) {
            logger.warn(String.format("The Black Duck %s could not successfully inspect %s:", InspectionModule.class.getSimpleName(), repoPath));
            logger.debug(e.getMessage(), e);
            cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.FAILURE);
        }
    }

    public Optional<Set<RepoPath>> getIdentifiableArtifacts(final String repoKey) {
        Set<RepoPath> identifiableArtifacts = null;

        final RepositoryConfiguration repositoryConfiguration = repositories.getRepositoryConfiguration(repoKey);
        final String packageType = repositoryConfiguration.getPackageType();
        final Optional<String> patterns = packageTypePatternManager.getPattern(packageType);
        if (patterns.isPresent()) {
            final String[] patternsToFind = patterns.get().split(",");
            identifiableArtifacts = Arrays.stream(patternsToFind)
                                        .map(pattern -> searches.artifactsByName(pattern, repoKey))
                                        .flatMap(List::stream)
                                        .collect(Collectors.toSet());
        }

        return Optional.ofNullable(identifiableArtifacts);
    }

    private void createHubProjectFromRepo(final String projectName, final String projectVersionName, final String repoPackageType, final Set<RepoPath> repoPaths) throws IOException, IntegrationException {
        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();

        final List<Pair<RepoPath, Optional<IdentifiedArtifact>>> repoPathIndentifiedArtifactPair = repoPaths.stream()
                                                                                                       .map(repoPath -> Pair.of(repoPath, identifyArtifact(repoPath, repoPackageType)))
                                                                                                       .collect(Collectors.toList());

        final MutableDependencyGraph mutableDependencyGraph = repoPathIndentifiedArtifactPair.stream()
                                                                  .map(Pair::getRight)
                                                                  .filter(Optional::isPresent)
                                                                  .map(Optional::get)
                                                                  .peek(this::populateIdMetadataOnIdentifiedArtifact)
                                                                  .map(IdentifiedArtifact::getExternalId)
                                                                  .map(externalId -> new Dependency(externalId.name, externalId.version, externalId))
                                                                  .collect(simpleBdioFactory::createMutableDependencyGraph, MutableDependencyGraph::addChildToRoot, MutableDependencyGraph::addGraphAsChildrenToRoot);

        final Forge artifactoryForge = new Forge("/", "/", "artifactory");
        final ExternalId projectExternalId = simpleBdioFactory.createNameVersionExternalId(artifactoryForge, projectName, projectVersionName);
        final String codeLocationName = StringUtils.join(Arrays.asList(projectName, projectVersionName, repoPackageType), "/");
        final SimpleBdioDocument simpleBdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, projectName, projectVersionName, projectExternalId, mutableDependencyGraph);

        final IntegrationEscapeUtil integrationEscapeUtil = new IntegrationEscapeUtil();
        final File bdioFile = new File(String.format("/tmp/%s", integrationEscapeUtil.escapeForUri(codeLocationName)));
        bdioFile.delete();
        simpleBdioFactory.writeSimpleBdioDocumentToFile(bdioFile, simpleBdioDocument);

        blackDuckConnectionService.importBomFile(bdioFile);

        for (final Pair<RepoPath, Optional<IdentifiedArtifact>> pair : repoPathIndentifiedArtifactPair) {
            InspectionStatus inspectionStatus = InspectionStatus.NO_EXTERNAL_ID_FOUND;
            if (pair.getValue().isPresent()) {
                inspectionStatus = InspectionStatus.SUCCESS;
            }
            cacheInspectorService.setInspectionStatus(pair.getKey(), inspectionStatus);
        }
    }

    private void addDeltaToHubProject(final String projectName, final String projectVersionName, final String repoPackageType, final Set<RepoPath> repoPaths) {
        final List<IdentifiedArtifact> identifiedArtifacts = repoPaths.stream()
                                                                 .filter(repoPath -> !assertInspectionStatusIs(repoPath, InspectionStatus.SUCCESS))
                                                                 .map(repoPath -> identifyArtifact(repoPath, repoPackageType))
                                                                 .filter(Optional::isPresent)
                                                                 .map(Optional::get)
                                                                 .collect(Collectors.toList());

        identifiedArtifacts.forEach(identifiedArtifact -> {
            populateIdMetadataOnIdentifiedArtifact(identifiedArtifact);
            addIdentifiedArtifactToProjectVersion(identifiedArtifact, projectName, projectVersionName);
        });
    }

    private boolean assertInspectionStatusIs(final RepoPath repoPath, final InspectionStatus status) {
        final Optional<InspectionStatus> inspectionStatus = cacheInspectorService.getInspectionStatus(repoPath);
        return inspectionStatus.isPresent() && inspectionStatus.get().equals(status);
    }

    public Long getArtifactCount(final List<String> repoKeys) {
        return repoKeys.stream()
                   .map(RepoPathFactory::create)
                   .map(repositories::getArtifactsCount)
                   .mapToLong(Long::longValue)
                   .sum();
    }

    public class IdentifiedArtifact {
        private final RepoPath repoPath;
        private final ExternalId externalId;

        public IdentifiedArtifact(final RepoPath repoPath, final ExternalId externalId) {
            this.repoPath = repoPath;
            this.externalId = externalId;
        }

        public RepoPath getRepoPath() {
            return repoPath;
        }

        public ExternalId getExternalId() {
            return externalId;
        }
    }
}
