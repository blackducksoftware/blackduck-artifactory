package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import org.apache.commons.lang.StringUtils;
import org.artifactory.exception.CancelException;
import org.artifactory.repo.RepoPath;

public abstract class CancelDecider {
    abstract CancelDecision getCancelDecision(RepoPath repoPath);

    public void handleBeforeDownloadEvent(RepoPath repoPath) {
        CancelDecision cancelDecision = this.getCancelDecision(repoPath);
        if (cancelDecision.shouldCancelDownload()) {
            String cancelMessageSuffix = ". " + StringUtils.trimToEmpty(cancelDecision.getCancelReason());
            throw new CancelException(String.format("The Black Duck plugin has prevented the download of %s%s", repoPath.toPath(), cancelMessageSuffix), 403);
        }
    }
}
