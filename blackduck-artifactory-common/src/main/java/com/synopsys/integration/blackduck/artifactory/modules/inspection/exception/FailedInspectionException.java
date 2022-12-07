/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.exception;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.exception.IntegrationException;

public class FailedInspectionException extends IntegrationException {
    private final RepoPath repoPath;

    public FailedInspectionException(RepoPath repoPath, String message) {
        super(message);
        this.repoPath = repoPath;
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }
}
