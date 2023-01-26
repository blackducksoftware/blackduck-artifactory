/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory;

public enum TriggerType {
    BEFORE_DOWNLOAD("download beforeDownload"),
    CRON_JOB("cron job"),
    REST_REQUEST("REST request"),
    STARTUP("startup"),
    STORAGE_AFTER_CREATE("storage afterCreate"),
    STORAGE_AFTER_COPY("storage afterCopy"),
    STORAGE_AFTER_MOVE("storage afterMove");

    final String logName;

    TriggerType(String logName) {
        this.logName = logName;
    }

    public String getLogName() {
        return logName;
    }
}
