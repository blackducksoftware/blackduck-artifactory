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
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.codelocation.bdioupload.BdioUploadService;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadTarget;
import com.synopsys.integration.blackduck.exception.BlackDuckApiException;
import com.synopsys.integration.blackduck.exception.BlackDuckIntegrationException;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ComponentService;
import com.synopsys.integration.blackduck.service.ProjectBomService;
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
    private final InspectionModuleConfig inspectionModuleConfig;

    public ArtifactIdentificationService(final ArtifactoryPAPIService artifactoryPAPIService, final PackageTypePatternManager packageTypePatternManager, final ArtifactoryExternalIdFactory artifactoryExternalIdFactory,
        final ArtifactoryPropertyService artifactoryPropertyService, final CacheInspectorService cacheInspectorService, final BlackDuckServicesFactory blackDuckServicesFactory, final MetaDataPopulationService metaDataPopulationService,
        final InspectionModuleConfig inspectionModuleConfig) {
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.cacheInspectorService = cacheInspectorService;
        this.packageTypePatternManager = packageTypePatternManager;
        this.artifactoryExternalIdFactory = artifactoryExternalIdFactory;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.blackDuckServicesFactory = blackDuckServicesFactory;
        this.metaDataPopulationService = metaDataPopulationService;
        this.inspectionModuleConfig = inspectionModuleConfig;
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
                        logger.debug(String.format("Adding delta to Black Duck project %s", repoKey));
                        addDeltaToBlackDuckProject(projectView, projectVersionView, packageType.get(), identifiableArtifacts);
                    } else {
                        throw new IntegrationException(String.format("Expected project '%s' and version '%s' are missing", projectName, projectVersionName));
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
            logger.error(String.format("The Black Duck %s encountered an exception while identifying artifacts in repository '%s'. Inspection may not have completed for other artifacts", InspectionModule.class.getSimpleName(), repoKey), e);
            cacheInspectorService.setInspectionStatus(repoKeyPath, InspectionStatus.FAILURE, e.getMessage());
        }
    }

    public IdentifiedArtifact identifyArtifact(final RepoPath repoPath, final String packageType) {
        final FileLayoutInfo fileLayoutInfo = artifactoryPAPIService.getLayoutInfo(repoPath);
        final org.artifactory.md.Properties properties = artifactoryPAPIService.getProperties(repoPath);
        final Optional<ExternalId> possibleExternalId = artifactoryExternalIdFactory.createExternalId(packageType, fileLayoutInfo, repoPath, properties);
        final ExternalId externalId = possibleExternalId.orElse(null);

        return new IdentifiedArtifact(repoPath, externalId);
    }

    public void populateIdMetadataOnIdentifiedArtifact(final IdentifiedArtifact identifiedArtifact) {
        if (!identifiedArtifact.getExternalId().isPresent()) {
            logger.debug(String.format("Could not populate artifact with metadata. Missing externalId: %s", identifiedArtifact.getRepoPath()));
            failInspection(identifiedArtifact.getRepoPath(), "Artifactory failed to provide sufficient information to identify the artifact");
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

    public boolean addIdentifiedArtifactToProjectVersion(final IdentifiedArtifact identifiedArtifact, final ProjectVersionView projectVersionView) {
        final RepoPath repoPath = identifiedArtifact.getRepoPath();

        boolean success = false;

        if (identifiedArtifact.getExternalId().isPresent()) {
            final ExternalId externalId = identifiedArtifact.getExternalId().get();
            try {
                final ProjectBomService projectBomService = blackDuckServicesFactory.createProjectBomService();
                final Optional<String> componentVersionUrl = projectBomService.addComponentToProjectVersion(externalId, projectVersionView);

                success = componentVersionUrl.isPresent();
                if (success) {
                    cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.PENDING);
                } else {
                    failInspection(repoPath, "Failed to find component match");
                }

            } catch (final IntegrationRestException e) {
                success = handleIntegrationRestException(repoPath, projectVersionView, externalId, e);
            } catch (final BlackDuckApiException e) {
                success = handleIntegrationRestException(repoPath, projectVersionView, externalId, e.getOriginalIntegrationRestException());
            } catch (final BlackDuckIntegrationException e) {
                logger.warn(String.format("Cannot find component match for artifact at %s", repoPath.toPath()));
                failInspection(repoPath, "Failed to find component match");
            } catch (final Exception e) {
                logger.warn(String.format("The Black Duck %s could not successfully inspect %s:", InspectionModule.class.getSimpleName(), repoPath.toPath()));
                logger.debug(e.getMessage(), e);
                failInspection(repoPath, "See logs for details");
            }
        } else {
            failInspection(repoPath, "Artifactory failed to provide sufficient information to identify the artifact");
        }

        return success;
    }

    private boolean handleIntegrationRestException(final RepoPath repoPath, final ProjectVersionView projectVersionView, final ExternalId artifactExternalId, final IntegrationRestException e) {
        final int statusCode = e.getHttpStatusCode();
        boolean success = false;

        if (statusCode == 412) {
            logger.info(String.format("Unable to add manual BOM component because it already exists: %s", repoPath.toPath()));
            try {
                logger.debug("Will attempt to grab policy status from Black Duck directly.");
                final ComponentViewWrapper componentViewWrapper = getComponentViewWrapper(projectVersionView, artifactExternalId);
                metaDataPopulationService.populateBlackDuckMetadata(repoPath, componentViewWrapper.getComponentVersionView(), componentViewWrapper.getVersionBomComponentView());
                success = true;
            } catch (final IntegrationException e1) {
                logger.debug("Failed to populate artifact with policy info even though it already exists in the BOM", e1);
                failInspection(repoPath, "Failed to retrieve policy information");
            }
        } else if (statusCode == 401) {
            logger.warn(String.format("The Black Duck %s could not successfully inspect %s because plugin is unauthorized (%d). Ensure the plugin is configured with the correct credentials", InspectionModule.class.getSimpleName(),
                repoPath.toPath(), statusCode));
            failInspection(repoPath, String.format("Unauthorized (%s)", statusCode));
        } else {
            logger.warn(String.format("The Black Duck %s could not successfully inspect %s because of a %d status code", InspectionModule.class.getSimpleName(), repoPath.toPath(), statusCode));
            logger.debug(String.format(e.getMessage(), repoPath), e);
            failInspection(repoPath, String.format("Status code: %s", statusCode));
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

    public boolean shouldInspectArtifact(final List<String> validRepoKeys, final RepoPath repoPath) {
        if (!validRepoKeys.contains(repoPath.getRepoKey())) {
            return false;
        }

        final Optional<String> packageType = artifactoryPAPIService.getPackageType(repoPath.getRepoKey());
        return packageType
                   .filter(s -> packageTypePatternManager.getPatterns(s).isPresent())
                   .isPresent();

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

    private ComponentViewWrapper getComponentViewWrapper(final ProjectVersionView projectVersionView, final ExternalId externalId) throws IntegrationException {
        final ComponentService componentService = blackDuckServicesFactory.createComponentService();
        final BlackDuckService blackDuckService = blackDuckServicesFactory.createBlackDuckService();
        final Optional<ComponentVersionView> componentVersionViewOptional = componentService.getComponentVersion(externalId);

        if (componentVersionViewOptional.isPresent()) {
            final ComponentVersionView componentVersionView = componentVersionViewOptional.get();
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

                return new ComponentViewWrapper(versionBomComponentView, componentVersionView);
            } else {
                throw new IntegrationException("projectVersionViewHref or componentVersionViewHref is not present");
            }
        } else {
            throw new IntegrationException(String.format("Cannot find component with given external id: %s:%s", externalId.forge, externalId.createBlackDuckOriginId()));
        }
    }

    private void addDeltaToBlackDuckProject(final ProjectView projectView, final ProjectVersionView projectVersionView, final String packageType, final Set<RepoPath> repoPaths) {
        for (final RepoPath repoPath : repoPaths) {
            final boolean isArtifactPending = cacheInspectorService.assertInspectionStatus(repoPath, InspectionStatus.PENDING);
            final boolean shouldRetry = cacheInspectorService.assertInspectionStatus(repoPath, InspectionStatus.FAILURE) && getRetryCount(repoPath) < inspectionModuleConfig.getRetryCount();

            if (isArtifactPending || shouldRetry) {
                final IdentifiedArtifact identifiedArtifact = identifyArtifact(repoPath, packageType);
                if (!identifiedArtifact.getExternalId().isPresent()) {
                    failInspection(repoPath, "Failed to generate external id from properties");
                    continue;
                }

                final boolean successfullyAdded = addIdentifiedArtifactToProjectVersion(identifiedArtifact, projectVersionView);
                final Optional<ExternalId> externalIdOptional = identifiedArtifact.getExternalId();

                if (successfullyAdded && externalIdOptional.isPresent()) {
                    final ExternalId externalId = externalIdOptional.get();
                    try {
                        final ComponentViewWrapper componentViewWrapper = getComponentViewWrapper(projectVersionView, externalId);
                        metaDataPopulationService.populateBlackDuckMetadata(repoPath, componentViewWrapper.getComponentVersionView(), componentViewWrapper.getVersionBomComponentView());
                    } catch (final IntegrationException e) {
                        failInspection(repoPath, "Failed to retrieve vulnerability information");
                        logger.warn(String.format("Failed to retrieve vulnerability information for artifact: %s", repoPath.toPath()));
                        logger.debug(e.getMessage(), e);
                    }
                } else {
                    failInspection(repoPath, "Artifact was not successfully added to Black Duck project");
                    logger.warn(String.format("Artifact was not successfully added to Black Duck project [%s] version [%s]: %s", projectView.getName(), projectVersionView.getVersionName(), repoPath.toPath()));
                }
            } else {
                logger.trace(String.format("Artifact is not pending and therefore will not be inspected: %s", repoPath.toPath()));
            }
        }

        if (repoPaths.isEmpty()) {
            logger.debug("Cannot add delta to Black Duck because supplied repoPaths is empty");
        }
    }

    private Integer getRetryCount(final RepoPath repoPath) {
        final Optional<Integer> retryCount = artifactoryPropertyService.getPropertyAsInteger(repoPath, BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT, logger);
        return retryCount.orElse(0);
    }

    public void failInspection(final RepoPath repoPath, final String inspectionStatusMessage) {
        final int retryCount = getRetryCount(repoPath) + 1;
        cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.FAILURE, inspectionStatusMessage, retryCount);
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
