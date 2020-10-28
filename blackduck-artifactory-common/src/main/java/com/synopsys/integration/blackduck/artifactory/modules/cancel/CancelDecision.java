package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import org.jetbrains.annotations.Nullable;

public class CancelDecision {
    private final boolean shouldCancelDownload;
    @Nullable
    private final String cancelReason;

    public static CancelDecision CANCEL_DOWNLOAD(String cancelReason) {
        return new CancelDecision(true, cancelReason);
    }

    public static CancelDecision NO_CANCELLATION() {
        return new CancelDecision(true, null);
    }

    private CancelDecision(boolean shouldCancelDownload, @Nullable String cancelReason) {
        this.shouldCancelDownload = shouldCancelDownload;
        this.cancelReason = cancelReason;
    }

    public boolean shouldCancelDownload() {
        return shouldCancelDownload;
    }

    @Nullable // null when getCancelDecision() is false
    public String getCancelReason() {
        return cancelReason;
    }
}
