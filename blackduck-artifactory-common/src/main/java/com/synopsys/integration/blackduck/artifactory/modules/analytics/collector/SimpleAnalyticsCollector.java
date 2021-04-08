/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.analytics.collector;

import java.util.HashMap;
import java.util.Map;

public class SimpleAnalyticsCollector extends AnalyticsCollector {
    private final Map<String, String> metadataMap = new HashMap<>();

    public Object putMetadata(String key, Object value) {
        return metadataMap.put(key, value.toString());
    }

    @Override
    public Map<String, String> getMetadataMap() {
        return metadataMap;
    }

    @Override
    public void clear() {
        metadataMap.clear();
    }
}
