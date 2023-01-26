/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import org.artifactory.exception.CancelException;
import org.artifactory.repo.RepoPath;

public interface CancelDecider {
    CancelDecision getCancelDecision(RepoPath repoPath);

    default void handleBeforeDownloadEvent(RepoPath repoPath) {
        CancelDecision cancelDecision = getCancelDecision(repoPath);
        if (cancelDecision.shouldCancelDownload()) {
            throw new CancelException(String.format("The Black Duck plugin has prevented the download of %s. %s", repoPath.toPath(), cancelDecision.getCancelReason()), 403);
        }
    }
}
