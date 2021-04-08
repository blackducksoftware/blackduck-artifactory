/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.model;

import java.util.Optional;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.bdio.model.externalid.ExternalId;

public class Artifact {
    private final RepoPath repoPath;
    private final ExternalId externalId;

    public Artifact(RepoPath repoPath, ExternalId externalId) {
        this.repoPath = repoPath;
        this.externalId = externalId;
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public Optional<ExternalId> getExternalId() {
        return Optional.ofNullable(externalId);
    }
}
