package com.synopsys.integration.blackduck.artifactory.modules.inspection.model;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.bdio.model.externalid.ExternalId;

public class IdentifiedArtifact extends Artifact {
    private final ExternalId externalId;

    public IdentifiedArtifact(final RepoPath repoPath, final ExternalId externalId) {
        super(repoPath);
        this.externalId = externalId;
    }

    public ExternalId getExternalId() {
        return externalId;
    }
}
