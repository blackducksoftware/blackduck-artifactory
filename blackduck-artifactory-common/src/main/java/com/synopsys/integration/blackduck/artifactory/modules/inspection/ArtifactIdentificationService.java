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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.api.UriSingleResponse;
import com.synopsys.integration.blackduck.api.generated.view.ComponentSearchResultView;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.exception.FailedInspectionException;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.Artifact;
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

public class ArtifactIdentificationService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(ArtifactIdentificationService.class));

    private final PackageTypePatternManager packageTypePatternManager;
    private final ArtifactoryExternalIdFactory artifactoryExternalIdFactory;

    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final CacheInspectorService cacheInspectorService;
    private final BlackDuckServicesFactory blackDuckServicesFactory;
    private final MetaDataPopulationService metaDataPopulationService;

    public ArtifactIdentificationService(final ArtifactoryPAPIService artifactoryPAPIService, final PackageTypePatternManager packageTypePatternManager, final ArtifactoryExternalIdFactory artifactoryExternalIdFactory,
        final CacheInspectorService cacheInspectorService, final BlackDuckServicesFactory blackDuckServicesFactory, final MetaDataPopulationService metaDataPopulationService) {
        this.cacheInspectorService = cacheInspectorService;
        this.packageTypePatternManager = packageTypePatternManager;
        this.artifactoryExternalIdFactory = artifactoryExternalIdFactory;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.blackDuckServicesFactory = blackDuckServicesFactory;
        this.metaDataPopulationService = metaDataPopulationService;
    }

    public void identifyArtifacts(final String repoKey) {
        final RepoPath repoKeyPath = RepoPathFactory.create(repoKey);

        try {
            final Set<RepoPath> identifiableArtifacts = getIdentifiableArtifacts(repoKey);
            final Optional<String> packageType = artifactoryPAPIService.getPackageType(repoKey);

            if (!identifiableArtifacts.isEmpty() && packageType.isPresent()) {
                final String projectName = cacheInspectorService.getRepoProjectName(repoKey);
                final String projectVersionName = cacheInspectorService.getRepoProjectVersionName(repoKey);

                if (cacheInspectorService.assertInspectionStatus(repoKeyPath, InspectionStatus.SUCCESS)) {
                    final Optional<ProjectVersionWrapper> projectVersionWrapper = blackDuckServicesFactory.createProjectService().getProjectVersion(projectName, projectVersionName);

                    if (projectVersionWrapper.isPresent()) {
                        final ProjectVersionView projectVersionView = projectVersionWrapper.get().getProjectVersionView();
                        logger.debug(String.format("Adding delta to Black Duck project %s", repoKey));
                        addDeltaToBlackDuckProject(projectVersionView, packageType.get(), identifiableArtifacts);
                    } else {
                        throw new IntegrationException(String.format("Expected project '%s' and version '%s' are missing", projectName, projectVersionName));
                    }
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

    public Artifact identifyArtifact(final RepoPath repoPath, final String packageType) {
        final FileLayoutInfo fileLayoutInfo = artifactoryPAPIService.getLayoutInfo(repoPath);
        final org.artifactory.md.Properties properties = artifactoryPAPIService.getProperties(repoPath);
        final Optional<ExternalId> possibleExternalId = artifactoryExternalIdFactory.createExternalId(packageType, fileLayoutInfo, repoPath, properties);
        final ExternalId externalId = possibleExternalId.orElse(null);

        return new Artifact(repoPath, externalId);
    }

    public ComponentViewWrapper addIdentifiedArtifactToProjectVersion(final Artifact artifact, final ProjectVersionView projectVersionView) throws FailedInspectionException {
        final RepoPath repoPath = artifact.getRepoPath();
        ComponentViewWrapper componentViewWrapper;

        if (artifact.getExternalId().isPresent()) {
            final ExternalId externalId = artifact.getExternalId().get();
            try {
                final ProjectBomService projectBomService = blackDuckServicesFactory.createProjectBomService();
                final Optional<String> componentVersionUrl = projectBomService.addComponentToProjectVersion(externalId, projectVersionView);

                if (componentVersionUrl.isPresent()) {
                    componentViewWrapper = getComponentViewWrapper(projectVersionView, externalId);
                    cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.PENDING); // TODO: Add an AWAITING_POLICY status.
                } else {
                    throw new FailedInspectionException(repoPath, "Failed to find component match");
                }

            } catch (final IntegrationRestException e) {
                componentViewWrapper = handleIntegrationRestException(repoPath, projectVersionView, externalId, e);
            } catch (final BlackDuckApiException e) {
                componentViewWrapper = handleIntegrationRestException(repoPath, projectVersionView, externalId, e.getOriginalIntegrationRestException());
            } catch (final BlackDuckIntegrationException e) {
                logger.warn(String.format("Cannot find component match for artifact at %s", repoPath.toPath()));
                throw new FailedInspectionException(repoPath, "Failed to find component match");
            } catch (final Exception e) {
                logger.warn(String.format("The Black Duck %s could not successfully inspect %s:", InspectionModule.class.getSimpleName(), repoPath.toPath()));
                logger.debug(e.getMessage(), e);
                throw new FailedInspectionException(repoPath, "See logs for details");
            }
        } else {
            throw new FailedInspectionException(repoPath, "Artifactory failed to provide sufficient information to identify the artifact");
        }

        return componentViewWrapper;
    }

    private ComponentViewWrapper handleIntegrationRestException(final RepoPath repoPath, final ProjectVersionView projectVersionView, final ExternalId artifactExternalId, final IntegrationRestException e) throws FailedInspectionException {
        final int statusCode = e.getHttpStatusCode();
        final ComponentViewWrapper componentViewWrapper;

        if (statusCode == 412) {
            logger.info(String.format("Unable to add manual BOM component because it already exists: %s", repoPath.toPath()));
            try {
                logger.debug("Will attempt to grab policy status from Black Duck directly.");
                componentViewWrapper = getComponentViewWrapper(projectVersionView, artifactExternalId);
                metaDataPopulationService.populateBlackDuckMetadata(repoPath, componentViewWrapper.getComponentVersionView(), componentViewWrapper.getVersionBomComponentView());
            } catch (final IntegrationException e1) {
                logger.debug("Failed to populate artifact with policy info even though it already exists in the BOM", e1);
                throw new FailedInspectionException(repoPath, "Failed to retrieve policy information");
            }
        } else if (statusCode == 401) {
            logger.warn(String.format("The Black Duck %s could not successfully inspect %s because plugin is unauthorized (%d). Ensure the plugin is configured with the correct credentials", InspectionModule.class.getSimpleName(),
                repoPath.toPath(), statusCode));
            throw new FailedInspectionException(repoPath, String.format("Unauthorized (%s)", statusCode));
        } else {
            logger.warn(String.format("The Black Duck %s could not successfully inspect %s because of a %d status code", InspectionModule.class.getSimpleName(), repoPath.toPath(), statusCode));
            logger.debug(String.format(e.getMessage(), repoPath), e);
            throw new FailedInspectionException(repoPath, String.format("Status code: %s", statusCode));
        }

        return componentViewWrapper;
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

    public ComponentViewWrapper getComponentViewWrapper(final ProjectVersionView projectVersionView, final ExternalId externalId) throws IntegrationException {
        final ComponentService componentService = blackDuckServicesFactory.createComponentService();
        final BlackDuckService blackDuckService = blackDuckServicesFactory.createBlackDuckService();
        final Optional<ComponentSearchResultView> componentSearchResultView = componentService.getExactComponentMatch(externalId);

        if (!componentSearchResultView.isPresent()) {
            throw new IntegrationException(String.format("No search results for component found: %s", externalId.createBlackDuckOriginId()));
        }

        final String componentVersionUrl = componentSearchResultView.get().getVersion();
        final ComponentVersionView componentVersionView = blackDuckService.getResponse(new UriSingleResponse<>(componentVersionUrl, ComponentVersionView.class));
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
    }

    private void addDeltaToBlackDuckProject(final ProjectVersionView projectVersionView, final String packageType, final Set<RepoPath> repoPaths) {
        for (final RepoPath repoPath : repoPaths) {
            final boolean isArtifactPending = cacheInspectorService.assertInspectionStatus(repoPath, InspectionStatus.PENDING);
            final boolean shouldRetry = cacheInspectorService.shouldRetryInspection(repoPath);

            if (isArtifactPending || shouldRetry) {
                final Artifact artifact = identifyArtifact(repoPath, packageType);
                final boolean successfullyIdentified = metaDataPopulationService.populateExternalIdMetadata(artifact).isPresent();
                if (!successfullyIdentified) {
                    continue;
                }

                try {
                    final ComponentViewWrapper componentViewWrapper = addIdentifiedArtifactToProjectVersion(artifact, projectVersionView);
                    metaDataPopulationService.populateBlackDuckMetadata(repoPath, componentViewWrapper.getComponentVersionView(), componentViewWrapper.getVersionBomComponentView());
                } catch (final IntegrationException e) {
                    cacheInspectorService.failInspection(repoPath, "Failed to retrieve vulnerability information");
                    logger.warn(String.format("Failed to retrieve vulnerability information for artifact: %s", repoPath.toPath()));
                    logger.debug(e.getMessage(), e);
                }
            } else {
                logger.trace(String.format("Artifact is not pending and therefore will not be inspected: %s", repoPath.toPath()));
            }
        }

        if (repoPaths.isEmpty()) {
            logger.debug("Cannot add delta to Black Duck because supplied repoPaths is empty");
        }
    }
}
