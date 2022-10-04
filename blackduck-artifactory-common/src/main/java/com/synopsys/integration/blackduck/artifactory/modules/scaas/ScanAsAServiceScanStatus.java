/*
 * Copyright (C) 2022 Synopsys Inc.
 * http://www.synopsys.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Synopsys ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Synopsys.
 */

package com.synopsys.integration.blackduck.artifactory.modules.scaas;

import javax.annotation.Nullable;

public enum ScanAsAServiceScanStatus {
    SCAN_IN_PROGRESS("Scanning currently in progress."),
    SUCCESS_NO_POLICY_VIOLATION(null),
    SUCCESS_POLICY_VIOLATION("Scan successfully complete, but policy violations were detected."),
    FAILED("Scan failed"),
    UNKNOWN(""),
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
        return ScanAsAServiceScanStatus.UNKNOWN;
    }
}
