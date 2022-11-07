/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

public class CancelDecision {
    private final boolean shouldCancelDownload;
    @Nullable
    private final String cancelReason;

    public static CancelDecision CANCEL_DOWNLOAD(String cancelReason) {
        return new CancelDecision(true, cancelReason);
    }

    public static CancelDecision NO_CANCELLATION() {
        return new CancelDecision(false, null);
    }

    private CancelDecision(boolean shouldCancelDownload, @Nullable String cancelReason) {
        this.shouldCancelDownload = shouldCancelDownload;
        this.cancelReason = cancelReason;
    }

    public boolean shouldCancelDownload() {
        return shouldCancelDownload;
    }

    @Nullable // null when CancelDecision::shouldCancelDownload is false
    public String getCancelReason() {
        return cancelReason;
    }

    @Override
    public int hashCode() {
        return Objects.hash(shouldCancelDownload, cancelReason);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof CancelDecision)) {
            return false;
        }

        CancelDecision c = (CancelDecision) o;
        return shouldCancelDownload == c.shouldCancelDownload
                && ((cancelReason == null && c.cancelReason == null)
                    || (cancelReason != null && cancelReason.equals(c.cancelReason)));
    }
}
