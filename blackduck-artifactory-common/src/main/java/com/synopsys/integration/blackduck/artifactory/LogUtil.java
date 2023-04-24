/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory;

import com.synopsys.integration.log.IntLogger;

public class LogUtil {
    private LogUtil() {
        throw new IllegalStateException("Utility class.");
    }

    public static void start(IntLogger logger, String functionName, TriggerType triggerType) {
        if (triggerType.equals(TriggerType.STARTUP)) {
            logger.info(String.format("Starting %s for %s...", functionName, triggerType.getLogName()));
        } else {
            logger.info(String.format("Starting %s from %s...", functionName, triggerType.getLogName()));
        }
    }

    public static void finish(IntLogger logger, String functionName, TriggerType triggerType) {
        logger.info(String.format("...completed %s %s.", functionName, triggerType.getLogName()));
    }
}
