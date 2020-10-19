package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.util.NameVersion;

public class NotificationRepositoryFilter {
    private final Map<NameVersion, RepoPath> artifactoryProjects;

    public static NotificationRepositoryFilter fromProperties(InspectionPropertyService inspectionPropertyService, List<RepoPath> repoKeyPaths) {
        Map<NameVersion, RepoPath> artifactoryProjects = new HashMap<>();
        for (RepoPath repoKeyPath : repoKeyPaths) {
            inspectionPropertyService.getProjectNameVersion(repoKeyPath)
                .ifPresent(nameVersion -> artifactoryProjects.put(nameVersion, repoKeyPath));
        }

        return new NotificationRepositoryFilter(artifactoryProjects);
    }

    public NotificationRepositoryFilter(Map<NameVersion, RepoPath> artifactoryProjects) {
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
