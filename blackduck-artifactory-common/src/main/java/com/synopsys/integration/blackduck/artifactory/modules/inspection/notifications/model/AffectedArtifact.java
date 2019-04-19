package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model;

import org.artifactory.repo.RepoPath;

public class AffectedArtifact<T extends BlackDuckNotification> {
    private final RepoPath repoPath;
    private final T blackDuckNotification;

    public AffectedArtifact(final RepoPath repoPath, final T blackDuckNotification) {
        this.repoPath = repoPath;
        this.blackDuckNotification = blackDuckNotification;
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public T getBlackDuckNotification() {
        return blackDuckNotification;
    }
}
