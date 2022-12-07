/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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
