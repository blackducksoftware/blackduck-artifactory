/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import org.apache.commons.lang.StringUtils;
import org.artifactory.exception.CancelException;
import org.artifactory.repo.RepoPath;

public interface CancelDecider {
    CancelDecision getCancelDecision(RepoPath repoPath);

    default void handleBeforeDownloadEvent(RepoPath repoPath) {
        CancelDecision cancelDecision = getCancelDecision(repoPath);
        if (cancelDecision.shouldCancelDownload()) {
            String cancelMessageSuffix = StringUtils.trimToEmpty(cancelDecision.getCancelReason());
            if (StringUtils.isNotBlank(cancelMessageSuffix)) {
                cancelMessageSuffix = ". " + cancelMessageSuffix;
            } else {
                cancelMessageSuffix = ".";
            }

            throw new CancelException(String.format("The Black Duck plugin has prevented the download of %s%s", repoPath.toPath(), cancelMessageSuffix), 403);
        }
    }
}
