/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scaas;

import javax.annotation.Nullable;

public enum ScanAsAServiceScanStatus {
    PROCESSING("Artifact identified and Scanner notified."),
    SUCCESS("Scan successful"),
    FAILED("Scan failed"),
    ;

    private final String message;

    ScanAsAServiceScanStatus(@Nullable String message) {
        this.message = message;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public static ScanAsAServiceScanStatus getValue(String value) {
        for(ScanAsAServiceScanStatus val : ScanAsAServiceScanStatus.values()) {
            if (val.name().equals(value))
                return val;
        }
        throw new IllegalArgumentException(String.format("Unable to find suitable ScanAsAServiceScanStatus; Value: %s", value));
    }
}
