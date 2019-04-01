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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.SimpleBdioFactory;
import com.synopsys.integration.bdio.graph.MutableDependencyGraph;
import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.bdio.model.dependency.Dependency;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.IdentifiedArtifact;
import com.synopsys.integration.blackduck.codelocation.bdioupload.BdioUploadService;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadTarget;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.IntegrationEscapeUtil;

/**
 * Handles the initial BOM upload for a repository
 */
public class RepositoryInitializationService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final CacheInspectorService cacheInspectorService;
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final PackageTypePatternManager packageTypePatternManager;
    private final ArtifactIdentificationService2 artifactIdentificationService;
    private final MetaDataPopulationService metaDataPopulationService;
    private final BdioUploadService bdioUploadService;

    public RepositoryInitializationService(final CacheInspectorService cacheInspectorService, final ArtifactoryPAPIService artifactoryPAPIService,
        final PackageTypePatternManager packageTypePatternManager, final ArtifactIdentificationService2 artifactIdentificationService,
        final MetaDataPopulationService metaDataPopulationService, final BdioUploadService bdioUploadService) {
        this.cacheInspectorService = cacheInspectorService;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.packageTypePatternManager = packageTypePatternManager;
        this.artifactIdentificationService = artifactIdentificationService;
        this.metaDataPopulationService = metaDataPopulationService;
        this.bdioUploadService = bdioUploadService;
    }

    public void initializeRepository(final String repoKey) {
        final RepoPath repoKeyPath = RepoPathFactory.create(repoKey);
        if (cacheInspectorService.getInspectionStatus(repoKeyPath).isPresent() && !cacheInspectorService.assertInspectionStatus(repoKeyPath, InspectionStatus.FAILURE)) {
            // If an inspection status is present, we don't need to do a BOM upload unless it is a failure. In which case we will see if we should retry
            logger.debug(String.format("Not performing repo initialization on '%s' because it has already been initialized.", repoKey));
            return;
        }

        if (!cacheInspectorService.shouldRetryInspection(repoKeyPath)) {
            // Number of retry attempts exceeded
            return;
        }

        final Optional<String> packageType = artifactoryPAPIService.getPackageType(repoKey);
        if (!packageType.isPresent()) {
            logger.warn("Skipping initialization of configured repo '%s' because it is a package type was not found. Please remove this repo from your configuration or ensure a package type is specified");
            cacheInspectorService.failInspection(repoKeyPath, "Repository package type not found.");
            return;
        }

        final List<String> fileNamePatterns = packageTypePatternManager.getPatternsForPackageType(packageType.get());
        if (fileNamePatterns.isEmpty()) {
            final String message = String.format("No file name patterns configured for discovered package type '%s'.", packageType.get());
            logger.warn(message);
            cacheInspectorService.failInspection(repoKeyPath, message);
        }

        final String projectName = cacheInspectorService.getRepoProjectName(repoKey);
        final String projectVersionName = cacheInspectorService.getRepoProjectVersionName(repoKey);
        final List<RepoPath> identifiableRepoPaths = artifactoryPAPIService.searchForArtifactsByPatterns(Collections.singletonList(repoKey), fileNamePatterns);
        final List<IdentifiedArtifact> identifiedArtifacts = identifiableRepoPaths.stream()
                                                                 .filter(cacheInspectorService::shouldRetryInspection)
                                                                 .map(repoPath -> artifactIdentificationService.identifyArtifact(repoPath, packageType.get()))
                                                                 .filter(Optional::isPresent)
                                                                 .map(Optional::get)
                                                                 .collect(Collectors.toList());

        final List<Dependency> dependencies = identifiedArtifacts.stream()
                                                  .peek(metaDataPopulationService::populateExternalIdMetadata)
                                                  .map(identifiedArtifact -> new Dependency(identifiedArtifact.getExternalId().name, identifiedArtifact.getExternalId().version, identifiedArtifact.getExternalId()))
                                                  .collect(Collectors.toList());

        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        final MutableDependencyGraph mutableDependencyGraph = simpleBdioFactory.createMutableDependencyGraph();
        mutableDependencyGraph.addChildrenToRoot(dependencies);
        final Forge artifactoryForge = new Forge("/", "/", "artifactory");
        final ExternalId projectExternalId = simpleBdioFactory.createNameVersionExternalId(artifactoryForge, projectName, projectVersionName);
        final String codeLocationName = StringUtils.join(Arrays.asList(projectName, projectVersionName, packageType.get()), "/");
        final SimpleBdioDocument simpleBdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, projectName, projectVersionName, projectExternalId, mutableDependencyGraph);

        try {
            final IntegrationEscapeUtil integrationEscapeUtil = new IntegrationEscapeUtil();
            final File bdioFile = new File(String.format("/tmp/%s", integrationEscapeUtil.escapeForUri(codeLocationName)));
            if (bdioFile.exists() && !bdioFile.delete()) {
                logger.info(String.format("Failed to delete bdio. This may cause unexpected results. %s", bdioFile.getAbsolutePath()));
            }
            simpleBdioFactory.writeSimpleBdioDocumentToFile(bdioFile, simpleBdioDocument);

            final UploadTarget uploadTarget = UploadTarget.createDefault(codeLocationName, bdioFile);
            bdioUploadService.uploadBdio(uploadTarget);

            // The PENDING state is resolved by the MetaDataPopulationService
            cacheInspectorService.setInspectionStatus(repoKeyPath, InspectionStatus.PENDING, "Waiting for policy and vulnerability information");
        } catch (final IOException | IntegrationException e) {
            logger.error("An error occurred when attempting to upload bdio file", e);
            cacheInspectorService.failInspection(repoKeyPath, "Failed to upload BOM");
        }
    }
}
