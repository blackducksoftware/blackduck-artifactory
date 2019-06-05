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

import java.util.List;
import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.component.ProjectRequest;
import com.synopsys.integration.blackduck.api.generated.component.ProjectVersionRequest;
import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionPhaseType;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.exception.FailedInspectionException;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class RepositoryInitializationService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final InspectionPropertyService inspectionPropertyService;
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final InspectionModuleConfig inspectionModuleConfig;
    private final ProjectService projectService;

    public RepositoryInitializationService(final InspectionPropertyService inspectionPropertyService, final ArtifactoryPAPIService artifactoryPAPIService, final InspectionModuleConfig inspectionModuleConfig,
        final ProjectService projectService) {
        this.inspectionPropertyService = inspectionPropertyService;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.inspectionModuleConfig = inspectionModuleConfig;
        this.projectService = projectService;
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

        if (inspectionPropertyService.shouldRetryInspection(repoKeyPath)) {
            // If an inspection status is present, we don't need to do a BOM upload. A failure will be cleared automatically or by a user.
            logger.debug(String.format("Not performing repo initialization on '%s' because it has already been initialized.", repoKey));
            return;
        }

        final Optional<String> possiblePackageType = artifactoryPAPIService.getPackageType(repoKey);
        if (!possiblePackageType.isPresent()) {
            logger.warn(String.format("Skipping initialization of configured repo '%s' because its package type was not found. Please remove this repo from your configuration or ensure a package type is specified", repoKey));
            throw new FailedInspectionException(repoKeyPath, "Repository package type not found.");
        }
        final String packageType = possiblePackageType.get();

        if (!SupportedPackageType.getAsSupportedPackageType(packageType).isPresent()) {
            logger.warn(String.format("Skipping initialization of configured repo '%s' because its package type is not supported. Please remove this repo from your configuration or specify a supported package type", repoKey));
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

        try {
            if (!projectService.getProjectVersion(projectName, projectVersionName).isPresent()) {
                final ProjectRequest projectRequest = new ProjectRequest();
                projectRequest.setName(projectName);
                final ProjectVersionRequest projectVersionRequest = new ProjectVersionRequest();
                projectVersionRequest.setVersionName(projectVersionName);
                projectVersionRequest.setPhase(ProjectVersionPhaseType.RELEASED);
                projectRequest.setVersionRequest(projectVersionRequest);
                projectService.createProject(projectRequest);
                inspectionPropertyService.setInspectionStatus(repoKeyPath, InspectionStatus.SUCCESS);
            }
        } catch (final IntegrationException e) {
            final String message = String.format("Failed to create project and version in Black Duck for repository '%s'", repoKey);
            logger.debug(message, e);
            throw new FailedInspectionException(repoKeyPath, message);
        }
    }
}
