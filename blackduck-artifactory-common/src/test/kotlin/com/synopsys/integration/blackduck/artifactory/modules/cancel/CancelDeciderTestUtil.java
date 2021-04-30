package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.artifactory.exception.CancelException;
import org.artifactory.repo.RepoPath;
import org.junit.platform.commons.util.StringUtils;

public class CancelDeciderTestUtil {
    public static void assertCancellationDecision(boolean shouldCancelDownload, RepoPath repoPath, CancelDecider cancelDecider) {
        String cancelMessage;
        if (shouldCancelDownload) {
            cancelMessage = "The download should have been cancelled, but was not.";
        } else {
            cancelMessage = "The download should not have been cancelled, but was.";
        }
        assertCancellationDecision(shouldCancelDownload, repoPath, cancelDecider, cancelMessage);
    }

    public static void assertCancellationDecision(boolean shouldCancelDownload, RepoPath repoPath, CancelDecider cancelDecider, String assertionMessage) {
        CancelDecision cancelDecision = cancelDecider.getCancelDecision(repoPath);
        if (shouldCancelDownload) {
            assertTrue(cancelDecision.shouldCancelDownload(), assertionMessage);
            assertFalse(StringUtils.isBlank(cancelDecision.getCancelReason()));
            assertThrows(CancelException.class, () -> cancelDecider.handleBeforeDownloadEvent(repoPath));
        } else {
            assertFalse(cancelDecision.shouldCancelDownload(), assertionMessage);
            assertTrue(StringUtils.isBlank(cancelDecision.getCancelReason()));
            assertDoesNotThrow(() -> cancelDecider.handleBeforeDownloadEvent(repoPath));
        }
    }
}
