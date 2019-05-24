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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.service;

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
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.exception.FailedInspectionException;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.Artifact;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;
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

    private final InspectionPropertyService inspectionPropertyService;
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final InspectionModuleConfig inspectionModuleConfig;
    private final BdioUploadService bdioUploadService;
    private final ArtifactInspectionService artifactInspectionService;

    public RepositoryInitializationService(final InspectionPropertyService inspectionPropertyService, final ArtifactoryPAPIService artifactoryPAPIService, final InspectionModuleConfig inspectionModuleConfig,
        final BdioUploadService bdioUploadService, final ArtifactInspectionService artifactInspectionService) {
        this.inspectionPropertyService = inspectionPropertyService;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.inspectionModuleConfig = inspectionModuleConfig;
        this.bdioUploadService = bdioUploadService;
        this.artifactInspectionService = artifactInspectionService;
    }

    public void initializeRepository(final String repoKey) {
        final RepoPath repoKeyPath = RepoPathFactory.create(repoKey);
        try {
            initializeRepository(repoKeyPath);
        } catch (final FailedInspectionException e) {
            inspectionPropertyService.failInspection(e);
        }
    }

    private void initializeRepository(final RepoPath repoKeyPath) throws FailedInspectionException {
        final String repoKey = repoKeyPath.getRepoKey();
        final Optional<InspectionStatus> repoInspectionStatus = inspectionPropertyService.getInspectionStatus(repoKeyPath);
        if (repoInspectionStatus.isPresent()) {
            // If an inspection status is present, we don't need to do a BOM upload. A failure will be cleared automatically or by a user.
            logger.debug(String.format("Not performing repo initialization on '%s' because it has already been initialized.", repoKey));
            return;
        }

        final Optional<String> possiblePackageType = artifactoryPAPIService.getPackageType(repoKey);
        if (!possiblePackageType.isPresent()) {
            logger.warn("Skipping initialization of configured repo '%s' because its package type was not found. Please remove this repo from your configuration or ensure a package type is specified");
            throw new FailedInspectionException(repoKeyPath, "Repository package type not found.");
        }
        final String packageType = possiblePackageType.get();

        if (!SupportedPackageType.getAsSupportedPackageType(packageType).isPresent()) {
            logger.warn("Skipping initialization of configured repo '%s' because its package type is not supported. Please remove this repo from your configuration or specify a supported package type");
            throw new FailedInspectionException(repoKeyPath, "Repository package type not supported.");
        }

        final List<String> fileNamePatterns = inspectionModuleConfig.getPatternsForPackageType(packageType);
        if (fileNamePatterns.isEmpty()) {
            final String message = String.format("No file name patterns configured for discovered package type '%s'.", packageType);
            logger.warn(message);
            throw new FailedInspectionException(repoKeyPath, message);
        }

        final String projectName = inspectionPropertyService.getRepoProjectName(repoKey);
        final String projectVersionName = inspectionPropertyService.getRepoProjectVersionName(repoKey);
        inspectionPropertyService.setRepoProjectNameProperties(repoKey, projectName, projectVersionName);

        final List<RepoPath> identifiableRepoPaths = artifactoryPAPIService.searchForArtifactsByPatterns(Collections.singletonList(repoKey), fileNamePatterns);
        final List<Dependency> dependencies = identifiableRepoPaths.stream()
                                                  .map(artifactInspectionService::identifyAndMarkArtifact)
                                                  .map(Artifact::getExternalId)
                                                  .filter(Optional::isPresent)
                                                  .map(Optional::get)
                                                  .map(externalId -> new Dependency(externalId.name, externalId.version, externalId))
                                                  .collect(Collectors.toList());

        final SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
        final MutableDependencyGraph mutableDependencyGraph = simpleBdioFactory.createMutableDependencyGraph();
        mutableDependencyGraph.addChildrenToRoot(dependencies);
        final Forge artifactoryForge = new Forge("/", "/", "artifactory");
        final ExternalId projectExternalId = simpleBdioFactory.createNameVersionExternalId(artifactoryForge, projectName, projectVersionName);
        final String codeLocationName = StringUtils.join(Arrays.asList(projectName, projectVersionName, packageType), "/");
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
            inspectionPropertyService.setInspectionStatus(repoKeyPath, InspectionStatus.PENDING, "Waiting for policy and vulnerability information");
        } catch (final IOException | IntegrationException e) {
            logger.error("An error occurred when attempting to upload bdio file", e);
            throw new FailedInspectionException(repoKeyPath, String.format("Failed to upload BOM: %s", e.getMessage()));
        }
    }
}
