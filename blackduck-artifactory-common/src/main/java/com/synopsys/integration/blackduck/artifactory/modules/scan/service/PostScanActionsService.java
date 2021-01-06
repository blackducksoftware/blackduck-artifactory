/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.artifactory.modules.scan.service;

import java.util.List;
import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.manual.throwaway.generated.enumeration.ProjectVersionPhaseType;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.codelocation.Result;
import com.synopsys.integration.blackduck.service.dataservice.ProjectService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.NameVersion;

// TODO: Move ScanPolicyService functionality here or call it from here.
public class PostScanActionsService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final ProjectService projectService;

    public PostScanActionsService(ArtifactoryPropertyService artifactoryPropertyService, ProjectService projectService) {
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.projectService = projectService;
    }

    public void performPostScanActions(List<String> repoKeys) {
        SetMultimap<String, String> setMultimap = HashMultimap.create();
        setMultimap.put(BlackDuckArtifactoryProperty.SCAN_RESULT.getPropertyName(), Result.SUCCESS.name());
        setMultimap.put(BlackDuckArtifactoryProperty.POST_SCAN_ACTION_STATUS.getPropertyName(), PostScanActionStatus.PENDING.name());

        for (String repoKey : repoKeys) {
            RepoPath repoKeyPath = RepoPathFactory.create(repoKey);
            Optional<ProjectVersionPhaseType> postScanPhase = artifactoryPropertyService.getProperty(repoKeyPath, BlackDuckArtifactoryProperty.POST_SCAN_PHASE)
                                                                  .map(ProjectVersionPhaseType::valueOf);
            List<RepoPath> repoPaths = artifactoryPropertyService.getItemsContainingPropertiesAndValues(setMultimap, repoKey);
            if (postScanPhase.isPresent()) {
                setProjectPhase(repoPaths, postScanPhase.get());
            } else {
                repoPaths.forEach(repoPath -> artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.POST_SCAN_ACTION_STATUS, PostScanActionStatus.SUCCESS.name(), logger));
            }
        }
    }

    private void setProjectPhase(List<RepoPath> repoPaths, ProjectVersionPhaseType projectVersionPhaseType) {
        for (RepoPath repoPath : repoPaths) {
            try {
                ProjectVersionWrapper projectVersionWrapper = resolveProjectVersionWrapper(repoPath);
                ProjectVersionView projectVersionView = projectVersionWrapper.getProjectVersionView();
                projectVersionView.setPhase(projectVersionPhaseType);
                projectService.updateProjectVersion(projectVersionView);
                artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.POST_SCAN_ACTION_STATUS, PostScanActionStatus.SUCCESS.name(), logger);
            } catch (IntegrationException e) {
                logger.warn(String.format("Failed to perform post scan actions on '%s'. Black Duck may not have finished processing the scan. Will try again.", repoPath.getPath()));
            }
        }
    }

    // TODO: Create a ScanPropertyService for this class and ScanPolicyService to use.
    private ProjectVersionWrapper resolveProjectVersionWrapper(RepoPath repoPath) throws IntegrationException {
        Optional<NameVersion> nameVersion = artifactoryPropertyService.getProjectNameVersion(repoPath);

        if (nameVersion.isPresent()) {
            String projectName = nameVersion.get().getName();
            String projectVersionName = nameVersion.get().getVersion();
            return projectService.getProjectVersion(projectName, projectVersionName)
                       .orElseThrow(() -> new IntegrationException(String.format("Failed to find Black Duck project version with name '%s' and version '%s'.", projectName, projectVersionName)));
        } else {
            throw new IntegrationException("Failed to extract project name and project version from properties.");
        }
    }
}
