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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
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
import com.synopsys.integration.blackduck.api.generated.view.ProjectView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.codelocation.bdioupload.BdioUploadService;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadTarget;
import com.synopsys.integration.blackduck.exception.BlackDuckIntegrationException;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ComponentService;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.ComponentVersionVulnerabilities;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.rest.exception.IntegrationRestException;
import com.synopsys.integration.util.IntegrationEscapeUtil;

public class ArtifactIdentificationService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(ArtifactIdentificationService.class));

    private final PackageTypePatternManager packageTypePatternManager;
    private final ArtifactoryExternalIdFactory artifactoryExternalIdFactory;

    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final CacheInspectorService cacheInspectorService;
    private final BlackDuckServicesFactory blackDuckServicesFactory;
    private final MetaDataPopulationService metaDataPopulationService;

    public ArtifactIdentificationService(final ArtifactoryPAPIService artifactoryPAPIService, final PackageTypePatternManager packageTypePatternManager, final ArtifactoryExternalIdFactory artifactoryExternalIdFactory,
        final ArtifactoryPropertyService artifactoryPropertyService, final CacheInspectorService cacheInspectorService, final BlackDuckServicesFactory blackDuckServicesFactory, final MetaDataPopulationService metaDataPopulationService) {
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.cacheInspectorService = cacheInspectorService;
        this.packageTypePatternManager = packageTypePatternManager;
        this.artifactoryExternalIdFactory = artifactoryExternalIdFactory;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.blackDuckServicesFactory = blackDuckServicesFactory;
        this.metaDataPopulationService = metaDataPopulationService;
    }

    public void identifyArtifacts(final String repoKey) {
        final RepoPath repoKeyPath = RepoPathFactory.create(repoKey);
        final Optional<InspectionStatus> repositoryStatus = cacheInspectorService.getInspectionStatus(repoKeyPath);

        try {
            final Set<RepoPath> identifiableArtifacts = getIdentifiableArtifacts(repoKey);
            final Optional<String> packageType = artifactoryPAPIService.getPackageType(repoKey);

            if (!identifiableArtifacts.isEmpty() && packageType.isPresent()) {
                final String projectName = cacheInspectorService.getRepoProjectName(repoKey);
                final String projectVersionName = cacheInspectorService.getRepoProjectVersionName(repoKey);

                if (repositoryStatus.isPresent() && repositoryStatus.get().equals(InspectionStatus.SUCCESS)) {
                    final Optional<ProjectVersionWrapper> projectVersionWrapper = blackDuckServicesFactory.createProjectService().getProjectVersion(projectName, projectVersionName);

                    if (projectVersionWrapper.isPresent()) {
                        final ProjectView projectView = projectVersionWrapper.get().getProjectView();
                        final ProjectVersionView projectVersionView = projectVersionWrapper.get().getProjectVersionView();
                        addDeltaToBlackDuckProject(projectView, projectVersionView, packageType.get(), identifiableArtifacts);
                    }
                } else if (!repositoryStatus.isPresent()) {
                    createHubProjectFromRepo(projectName, projectVersionName, packageType.get(), identifiableArtifacts);
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
        final FileLayoutInfo fileLayoutInfo = artifactoryPAPIService.getLayoutInfo(repoPath);
        final org.artifactory.md.Properties properties = artifactoryPAPIService.getProperties(repoPath);
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
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID, blackDuckOriginId, logger);
        final String blackduckForge = externalId.forge.getName();
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_FORGE, blackduckForge, logger);

        cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.PENDING);
    }

    public boolean addIdentifiedArtifactToProjectVersion(final IdentifiedArtifact artifact, final ProjectVersionView projectVersionView) {
        final RepoPath repoPath = artifact.getRepoPath();

        boolean success = false;
        try {
            if (artifact.getExternalId().isPresent()) {
                final ProjectService projectService = blackDuckServicesFactory.createProjectService();
                success = projectService.addComponentToProjectVersion(artifact.getExternalId().get(), projectVersionView).isPresent();

                if (success) {
                    cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.PENDING);
                } else {
                    cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.FAILURE, "Failed to find component match");
                }
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
        } catch (final BlackDuckIntegrationException e) {
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
        final Optional<String> packageType = artifactoryPAPIService.getPackageType(repoKey);

        if (packageType.isPresent()) {
            final Optional<List<String>> patterns = packageTypePatternManager.getPatterns(packageType.get());
            if (patterns.isPresent()) {
                final List<RepoPath> repoPaths = artifactoryPAPIService.searchForArtifactsByPatterns(Collections.singletonList(repoKey), patterns.get());
                identifiableArtifacts.addAll(repoPaths);
            }
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

        final BdioUploadService bdioUploadService = blackDuckServicesFactory.createBdioUploadService();
        final UploadTarget uploadTarget = UploadTarget.createDefault(codeLocationName, bdioFile);
        bdioUploadService.uploadBdio(uploadTarget);
    }

    private void addDeltaToBlackDuckProject(final ProjectView projectView, final ProjectVersionView projectVersionView, final String repoPackageType, final Set<RepoPath> repoPaths) {
        final ComponentService componentService = blackDuckServicesFactory.createComponentService();
        final BlackDuckService blackDuckService = blackDuckServicesFactory.createBlackDuckService();

        for (final RepoPath repoPath : repoPaths) {
            final boolean isArtifactPending = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS, logger)
                                                  .map(InspectionStatus::valueOf)
                                                  .filter(InspectionStatus.PENDING::equals)
                                                  .isPresent();

            if (isArtifactPending) {
                final ArtifactIdentificationService.IdentifiedArtifact identifiedArtifact = identifyArtifact(repoPath, repoPackageType);
                final boolean successfullyAdded = addIdentifiedArtifactToProjectVersion(identifiedArtifact, projectVersionView);
                final Optional<ExternalId> externalIdOptional = identifiedArtifact.getExternalId();

                if (successfullyAdded && externalIdOptional.isPresent()) {
                    final ExternalId externalId = externalIdOptional.get();
                    try {
                        // Get componentVersionView
                        final Optional<ComponentVersionView> componentVersionViewOptional = componentService.getComponentVersion(externalId);

                        if (componentVersionViewOptional.isPresent()) {
                            final ComponentVersionView componentVersionView = componentVersionViewOptional.get();
                            // Get vulnerabilities
                            final ComponentVersionVulnerabilities componentVersionVulnerabilities = componentService.getComponentVersionVulnerabilities(componentVersionView);
                            final VulnerabilityAggregate vulnerabilityAggregate = VulnerabilityAggregate.fromVulnerabilityV2Views(componentVersionVulnerabilities.getVulnerabilities());

                            // Get policy status
                            final Optional<String> projectVersionViewHref = projectVersionView.getHref();
                            final Optional<String> componentVersionViewHref = componentVersionView.getHref();

                            // This is bad practice but...
                            // The link to a VersionBomComponentView cannot be obtained without searching the BOM or manually constructing the link. So for performance in Black Duck, we manually construct the link
                            if (projectVersionViewHref.isPresent() && componentVersionViewHref.isPresent()) {
                                final String apiComponentsLinkPrefix = "/api/components/";
                                final int apiComponentsStart = componentVersionViewHref.get().indexOf(apiComponentsLinkPrefix) + apiComponentsLinkPrefix.length();
                                final String versionBomComponentUri = projectVersionViewHref.get() + "/components/" + componentVersionViewHref.get().substring(apiComponentsStart);
                                final UriSingleResponse<VersionBomComponentView> versionBomComponentViewUriResponse = new UriSingleResponse<>(versionBomComponentUri, VersionBomComponentView.class);
                                final VersionBomComponentView versionBomComponentView = blackDuckService.getResponse(versionBomComponentViewUriResponse);
                                final PolicySummaryStatusType policyStatus = versionBomComponentView.getPolicyStatus();

                                // Populate metadata
                                metaDataPopulationService.populateBlackDuckMetadata(repoPath, vulnerabilityAggregate, policyStatus, componentVersionViewHref.get());
                            }
                        }
                    } catch (final IntegrationException e) {
                        cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.FAILURE, "Failed to retrieve vulnerability information");
                        logger.info(String.format("Failed to retrieve vulnerability information for artifact: %s", repoPath.toPath()));
                        logger.debug(e.getMessage(), e);
                    }
                } else {
                    logger.debug(String.format("Artifact was not successfully added to BlackDuck project [%s] version [%s]: %s = %s", projectView.getName(), projectVersionView.getVersionName(), repoPath.getName(), repoPath.toPath()));
                }
            }
        }
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
