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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.repo.Repositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.BlackDuckPropertyManager;
import com.synopsys.integration.util.HostNameHelper;

public class CacheInspectorService {
    private final Logger logger = LoggerFactory.getLogger(CacheInspectorService.class);

    private final BlackDuckPropertyManager blackDuckPropertyManager;
    private final Repositories repositories;
    private final ArtifactoryPropertyService artifactoryPropertyService;

    public CacheInspectorService(final BlackDuckPropertyManager blackDuckPropertyManager, final Repositories repositories, final ArtifactoryPropertyService artifactoryPropertyService) {
        this.repositories = repositories;
        this.blackDuckPropertyManager = blackDuckPropertyManager;
        this.artifactoryPropertyService = artifactoryPropertyService;
    }

    public void setInspectionStatus(final RepoPath repoPath, final InspectionStatus status) {
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS, status.name());
        artifactoryPropertyService.setPropertyToDate(repoPath, BlackDuckArtifactoryProperty.LAST_INSPECTION, new Date());
    }

    public Optional<InspectionStatus> getInspectionStatus(final RepoPath repoPath) {
        final Optional<String> inspectionStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS);

        return inspectionStatus.map(InspectionStatus::valueOf);
    }

    public String getRepoProjectName(final String repoKey) {
        final RepoPath repoPath = RepoPathFactory.create(repoKey);
        final Optional<String> projectNameProperty = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME);

        return projectNameProperty.orElse(repoKey);
    }

    public String getRepoProjectVersionName(final String repoKey) {
        final RepoPath repoPath = RepoPathFactory.create(repoKey);
        final Optional<String> projectVersionNameProperty = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME);

        return projectVersionNameProperty.orElse(HostNameHelper.getMyHostName("UNKNOWN_HOST"));
    }

    public List<String> getRepositoriesToInspect() throws IOException {
        final List<String> repoKeys = blackDuckPropertyManager.getRepositoryKeysFromProperties(InspectionModuleProperty.REPOS, InspectionModuleProperty.REPOS_CSV_PATH);
        return repoKeys.stream().filter(this::isValidRepository).collect(Collectors.toList());
    }

    // TODO: Move to ArtifactoryPropertyService
    private boolean isValidRepository(final String repoKey) {
        final boolean isValid;

        final RepoPath repoKeyPath = RepoPathFactory.create(repoKey);
        if (repositories.exists(repoKeyPath) && repositories.getRepositoryConfiguration(repoKey) != null) {
            isValid = true;
        } else {
            logger.warn(String.format("The Black Duck %s will ignore configured repository \'%s\': Repository was not found or is not a valid repository.", InspectionModule.class.getSimpleName(), repoKey));
            isValid = false;
        }

        return isValid;
    }

}
