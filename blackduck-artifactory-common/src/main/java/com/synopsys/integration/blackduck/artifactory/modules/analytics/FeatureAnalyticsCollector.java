/**
 * hub-artifactory-common
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.artifactory.modules.analytics;

import java.util.HashMap;
import java.util.Map;

/**
 * An {@link AnalyticsCollector} that records the number of times a particular function or event is triggered
 */
public class FeatureAnalyticsCollector extends AnalyticsCollector {
    private final Class analyzableClass;
    private final Map<String, Integer> statisticCounter = new HashMap<>();

    public FeatureAnalyticsCollector(final Class analyzableClass) {
        this.analyzableClass = analyzableClass;
    }

    public void logFeatureHit(final String className, final String featureName, final Object value) {
        logFeatureHit(className, featureName, value.toString());
    }

    public void logFeatureHit(final String featureName, final String value) {
        logFeatureHit(analyzableClass.getSimpleName(), featureName, value);
    }

    public void logFeatureHit(final String className, final String featureName, final String value) {
        final String statisticName = String.format("feature:%s.%s:%s", className, featureName, value);
        incrementStatistic(statisticName);
    }

    public Map<String, String> getMetadataMap() {
        return convertMapValueToString(statisticCounter);
    }

    public void clear() {
        statisticCounter.clear();
    }

    private void incrementStatistic(final String statisticName) {
        int count = 1;
        if (statisticCounter.containsKey(statisticName)) {
            final int currentCount = statisticCounter.get(statisticName);
            if (currentCount < Integer.MAX_VALUE - 2) {
                count = currentCount + 1;
            }
        }

        statisticCounter.put(statisticName, count);
    }
}
