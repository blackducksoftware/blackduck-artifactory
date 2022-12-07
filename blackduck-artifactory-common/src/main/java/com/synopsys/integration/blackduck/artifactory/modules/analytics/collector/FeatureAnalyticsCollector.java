/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.analytics.collector;

import java.util.HashMap;
import java.util.Map;

/**
 * An {@link AnalyticsCollector} that records the number of times a particular function or event is triggered
 */
public class FeatureAnalyticsCollector extends AnalyticsCollector {
    private final Class analyzableClass;
    private final Map<String, Integer> statisticCounter = new HashMap<>();

    public FeatureAnalyticsCollector(Class analyzableClass) {
        this.analyzableClass = analyzableClass;
    }

    public void logFeatureHit(String className, String featureName, Object value) {
        logFeatureHit(className, featureName, value.toString());
    }

    public void logFeatureHit(String featureName, String value) {
        logFeatureHit(analyzableClass.getSimpleName(), featureName, value);
    }

    public void logFeatureHit(String className, String featureName, String value) {
        String statisticName = String.format("feature:%s.%s:%s", className, featureName, value);
        incrementStatistic(statisticName);
    }

    @Override
    public Map<String, String> getMetadataMap() {
        return convertMapValueToString(statisticCounter);
    }

    @Override
    public void clear() {
        statisticCounter.clear();
    }

    private void incrementStatistic(String statisticName) {
        int count = 1;
        if (statisticCounter.containsKey(statisticName)) {
            int currentCount = statisticCounter.get(statisticName);
            if (currentCount < Integer.MAX_VALUE - 2) {
                count = currentCount + 1;
            }
        }

        statisticCounter.put(statisticName, count);
    }
}
