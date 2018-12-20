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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.repo.Repositories;
import org.artifactory.repo.RepositoryConfiguration;
import org.artifactory.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.SimpleBdioFactory;
import com.synopsys.integration.bdio.graph.MutableDependencyGraph;
import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.bdio.model.dependency.Dependency;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.api.UriSingleResponse;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.BlackDuckConnectionService;
import com.synopsys.integration.blackduck.exception.HubIntegrationException;
import com.synopsys.integration.blackduck.service.ComponentService;
import com.synopsys.integration.blackduck.service.HubService;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.ComponentVersionVulnerabilities;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
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
    private final MetaDataPopulationService metaDataPopulationService;

    public ArtifactIdentificationService(final Repositories repositories, final Searches searches, final PackageTypePatternManager packageTypePatternManager, final ArtifactoryExternalIdFactory artifactoryExternalIdFactory,
        final ArtifactoryPropertyService artifactoryPropertyService, final CacheInspectorService cacheInspectorService, final BlackDuckConnectionService blackDuckConnectionService,
        final MetaDataPopulationService metaDataPopulationService) {
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.cacheInspectorService = cacheInspectorService;
        this.blackDuckConnectionService = blackDuckConnectionService;
        this.packageTypePatternManager = packageTypePatternManager;
        this.artifactoryExternalIdFactory = artifactoryExternalIdFactory;
        this.repositories = repositories;
        this.searches = searches;
        this.metaDataPopulationService = metaDataPopulationService;
    }

    public void identifyArtifacts(final String repoKey) {
        final RepoPath repoKeyPath = RepoPathFactory.create(repoKey);
        final Optional<InspectionStatus> repositoryStatus = cacheInspectorService.getInspectionStatus(repoKeyPath);

        try {
            final Set<RepoPath> identifiableArtifacts = getIdentifiableArtifacts(repoKey);

            if (!identifiableArtifacts.isEmpty()) {
                final RepositoryConfiguration repositoryConfiguration = repositories.getRepositoryConfiguration(repoKey);
                final String packageType = repositoryConfiguration.getPackageType();
                final String projectName = cacheInspectorService.getRepoProjectName(repoKey);
                final String projectVersionName = cacheInspectorService.getRepoProjectVersionName(repoKey);

                if (repositoryStatus.isPresent() && repositoryStatus.get().equals(InspectionStatus.SUCCESS)) {
                    addDeltaToBlackDuckProject(projectName, projectVersionName, packageType, identifiableArtifacts);
                } else if (!repositoryStatus.isPresent()) {
                    createHubProjectFromRepo(projectName, projectVersionName, packageType, identifiableArtifacts);
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

    public IdentifiedArtifact identifyArtifact(final RepoPath repoPath, final String packageType) {
        final FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath);
        final org.artifactory.md.Properties properties = repositories.getProperties(repoPath);
        final Optional<ExternalId> possibleExternalId = artifactoryExternalIdFactory.createExternalId(packageType, fileLayoutInfo, properties);
        final ExternalId externalId = possibleExternalId.orElse(null);

        return new IdentifiedArtifact(repoPath, externalId);
    }

    public void populateIdMetadataOnIdentifiedArtifact(final IdentifiedArtifact identifiedArtifact) {
        if (!identifiedArtifact.getExternalId().isPresent()) {
            logger.debug(String.format("Could not populate artifact with metadata. Missing externalId: %s", identifiedArtifact.getRepoPath()));
            cacheInspectorService.setInspectionStatus(identifiedArtifact.getRepoPath(), InspectionStatus.FAILURE, "No external identifier found");
            return;
        }

        final ExternalId externalId = identifiedArtifact.getExternalId().get();
        final RepoPath repoPath = identifiedArtifact.getRepoPath();

        final String blackDuckOriginId = externalId.createBlackDuckOriginId();
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID, blackDuckOriginId);
        final String blackduckForge = externalId.forge.getName();
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_FORGE, blackduckForge);

        cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.PENDING);
    }

    public boolean addIdentifiedArtifactToProjectVersion(final IdentifiedArtifact artifact, final String projectName, final String projectVersionName) {
        final RepoPath repoPath = artifact.getRepoPath();

        boolean success = false;
        try {
            if (artifact.getExternalId().isPresent()) {
                // TODO: Might not need to return componentVersionUrl
                blackDuckConnectionService.addComponentToProjectVersion(artifact.getExternalId().get(), projectName, projectVersionName);
                cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.PENDING);
                success = true;
            } else {
                cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.FAILURE, "No external identifier found");
            }
        } catch (final IntegrationRestException e) {
            final int statusCode = e.getHttpStatusCode();
            if (statusCode == 412) {
                logger.info(String.format("Unable to add manual BOM component because it already exists: %s", repoPath.toPath()));
                cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.PENDING);
                success = true;
            } else if (statusCode == 401) {
                logger.warn(String.format("The Black Duck %s could not successfully inspect %s because plugin is unauthorized (%d). Ensure the plugin is configured with the correct credentials", InspectionModule.class.getSimpleName(),
                    repoPath.toPath(), statusCode));
                cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.FAILURE, String.format("Unauthorized (%s)", statusCode));
            } else {
                logger.warn(String.format("The Black Duck %s could not successfully inspect %s because of a %d status code", InspectionModule.class.getSimpleName(), repoPath.toPath(), statusCode));
                logger.debug(String.format(e.getMessage(), repoPath), e);
                cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.FAILURE, String.format("Status code: %s", statusCode));
            }
        } catch (final HubIntegrationException e) {
            logger.warn(String.format("Cannot find component match for artifact at %s", repoPath.toPath()));
            cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.FAILURE, "Failed to find component match");
        } catch (final Exception e) {
            logger.warn(String.format("The Black Duck %s could not successfully inspect %s:", InspectionModule.class.getSimpleName(), repoPath.toPath()));
            logger.debug(e.getMessage(), e);
            cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.FAILURE);
        }

        return success;
    }

    public Set<RepoPath> getIdentifiableArtifacts(final String repoKey) {
        final Set<RepoPath> identifiableArtifacts = new HashSet<>();

        final RepositoryConfiguration repositoryConfiguration = repositories.getRepositoryConfiguration(repoKey);
        final String packageType = repositoryConfiguration.getPackageType();
        final Optional<List<String>> patterns = packageTypePatternManager.getPatterns(packageType);
        if (patterns.isPresent()) {
            final Set<RepoPath> repoPaths = patterns.get().stream()
                                                .map(pattern -> searches.artifactsByName(pattern, repoKey))
                                                .flatMap(List::stream)
                                                .collect(Collectors.toSet());
            identifiableArtifacts.addAll(repoPaths);
        }

        return identifiableArtifacts;
    }

    private void createHubProjectFromRepo(final String projectName, final String projectVersionName, final String repoPackageType, final Set<RepoPath> repoPaths) throws IOException, IntegrationException {
        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();

        final List<IdentifiedArtifact> identifiedArtifacts = repoPaths.stream()
                                                                 .map(repoPath -> identifyArtifact(repoPath, repoPackageType))
                                                                 .collect(Collectors.toList());

        identifiedArtifacts.forEach(this::populateIdMetadataOnIdentifiedArtifact);

        final MutableDependencyGraph mutableDependencyGraph = identifiedArtifacts.stream()
                                                                  .map(IdentifiedArtifact::getExternalId)
                                                                  .filter(Optional::isPresent)
                                                                  .map(Optional::get)
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

        blackDuckConnectionService.importBomFile(codeLocationName, bdioFile);
    }

    private void addDeltaToBlackDuckProject(final String projectName, final String projectVersionName, final String repoPackageType, final Set<RepoPath> repoPaths) {
        final ComponentService componentService = blackDuckConnectionService.getHubServicesFactory().createComponentService();
        final HubService hubService = blackDuckConnectionService.getHubServicesFactory().createHubService();

        for (final RepoPath repoPath : repoPaths) {
            final boolean isArtifactPending = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS)
                                                  .map(InspectionStatus::valueOf)
                                                  .filter(InspectionStatus.PENDING::equals)
                                                  .isPresent();

            if (isArtifactPending) {
                final ArtifactIdentificationService.IdentifiedArtifact identifiedArtifact = identifyArtifact(repoPath, repoPackageType);
                final boolean successfullyAdded = addIdentifiedArtifactToProjectVersion(identifiedArtifact, projectName, projectVersionName);
                final Optional<ExternalId> externalIdOptional = identifiedArtifact.getExternalId();

                if (successfullyAdded && externalIdOptional.isPresent()) {
                    final ExternalId externalId = externalIdOptional.get();
                    try {
                        final ProjectService projectService = blackDuckConnectionService.getHubServicesFactory().createProjectService();
                        final Optional<ProjectVersionWrapper> projectVersionWrapper = projectService.getProjectVersion(projectName, projectVersionName);

                        if (projectVersionWrapper.isPresent()) {
                            // Get componentVersionView
                            final ComponentVersionView componentVersionView = componentService.getComponentVersion(externalId);

                            // Get vulnerabilities
                            final ComponentVersionVulnerabilities componentVersionVulnerabilities = componentService.getComponentVersionVulnerabilities(componentVersionView);
                            final VulnerabilityAggregate vulnerabilityAggregate = VulnerabilityAggregate.fromVulnerabilityV2Views(componentVersionVulnerabilities.getVulnerabilities());

                            // Get policy status
                            final ProjectVersionView projectVersionView = projectVersionWrapper.get().getProjectVersionView();
                            final UriSingleResponse<ProjectVersionView> projectVersionUriResponse = new UriSingleResponse<>(hubService.getHref(projectVersionView), ProjectVersionView.class);
                            final UriSingleResponse<ComponentVersionView> componentVersionUriResponse = new UriSingleResponse<>(hubService.getHref(componentVersionView), ComponentVersionView.class);
                            final UriSingleResponse<VersionBomComponentView> versionBomComponentUriResponse = blackDuckConnectionService.getVersionBomComponentUriResponse(projectVersionUriResponse, componentVersionUriResponse);
                            final VersionBomComponentView versionBomComponentView = hubService.getResponse(versionBomComponentUriResponse);
                            final PolicySummaryStatusType policyStatus = versionBomComponentView.policyStatus;

                            // Populate metadata
                            metaDataPopulationService.populateBlackDuckMetadata(repoPath, vulnerabilityAggregate, policyStatus, hubService.getHref(componentVersionView));
                        }
                    } catch (final IntegrationException e) {
                        cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.FAILURE, "Failed to retrieve vulnerability information");
                        logger.info(String.format("Failed to retrieve vulnerability information for artifact: %s", repoPath.toPath()));
                        logger.debug(e.getMessage(), e);
                    }
                } else {
                    logger.debug(String.format("Artifact was not successfully added to BlackDuck project [%s] version [%s]: %s = %s", projectName, projectVersionName, repoPath.getName(), repoPath.toPath()));
                }
            }
        }
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

        public Optional<ExternalId> getExternalId() {
            return Optional.ofNullable(externalId);
        }
    }
}
