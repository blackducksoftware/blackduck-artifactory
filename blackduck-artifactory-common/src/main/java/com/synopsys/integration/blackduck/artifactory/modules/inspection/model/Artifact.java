package com.synopsys.integration.blackduck.artifactory.modules.inspection.model;

import org.artifactory.repo.RepoPath;

public class Artifact {
    private final RepoPath repoPath;

    public Artifact(final RepoPath repoPath) {
        this.repoPath = repoPath;
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }
}
