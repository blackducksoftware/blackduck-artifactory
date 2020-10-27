/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.util.NameVersion;

// TODO: Create a common processor with an API for handling the RepositoryProjectNameLookup. All the processors have the same logic around it.
public class RepositoryProjectNameLookup {
    private final Map<NameVersion, RepoPath> artifactoryProjects;

    public static RepositoryProjectNameLookup fromProperties(InspectionPropertyService inspectionPropertyService, List<RepoPath> repoKeyPaths) {
        Map<NameVersion, RepoPath> artifactoryProjects = new HashMap<>();
        for (RepoPath repoKeyPath : repoKeyPaths) {
            inspectionPropertyService.getProjectNameVersion(repoKeyPath)
                .ifPresent(nameVersion -> artifactoryProjects.put(nameVersion, repoKeyPath));
        }

        return new RepositoryProjectNameLookup(artifactoryProjects);
    }

    public RepositoryProjectNameLookup(Map<NameVersion, RepoPath> artifactoryProjects) {
        this.artifactoryProjects = artifactoryProjects;
    }

    public Optional<RepoPath> getRepoKeyPath(String projectName, String projectVersionName) {
        NameVersion projectNameVersion = new NameVersion(projectName, projectVersionName);
        return getRepoKeyPath(projectNameVersion);
    }

    public Optional<RepoPath> getRepoKeyPath(NameVersion nameVersion) {
        return Optional.ofNullable(artifactoryProjects.get(nameVersion));
    }
}
