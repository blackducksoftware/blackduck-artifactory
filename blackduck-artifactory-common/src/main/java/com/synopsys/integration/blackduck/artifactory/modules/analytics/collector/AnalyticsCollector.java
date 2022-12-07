/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.analytics.collector;

import java.util.Map;
import java.util.stream.Collectors;

public abstract class AnalyticsCollector {
    protected static Map<String, String> convertMapValueToString(Map<String, ?> map) {
        return map.entrySet().stream()
                   .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString()));
    }

    public abstract Map<String, String> getMetadataMap();

    public abstract void clear();
}
