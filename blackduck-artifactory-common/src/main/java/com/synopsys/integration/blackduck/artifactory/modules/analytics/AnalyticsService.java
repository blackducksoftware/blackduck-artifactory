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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.synopsys.integration.blackduck.artifactory.BlackDuckConnectionService;

public class AnalyticsService {
    private final BlackDuckConnectionService blackDuckConnectionService;
    private final List<AnalyticsCollector> analyticsCollectors = new ArrayList<>();

    public AnalyticsService(final BlackDuckConnectionService blackDuckConnectionService) {
        this.blackDuckConnectionService = blackDuckConnectionService;
    }

    public void registerAnalyzable(final Analyzable analyzable) {
        Optional.ofNullable(analyzable.getAnalyticsCollectors())
            .map(analyticsCollectors::addAll);
    }

    /**
     * Submits a payload to phone home with data from all the collectors ({@link AnalyticsCollector})
     * This should be used infrequently such as once a day due to quota
     */
    public boolean submitAnalytics() {
        // Flatten the metadata maps from all of the collectors
        final Map<String, String> metadataMap = analyticsCollectors.stream()
                                                    .map(AnalyticsCollector::getMetadataMap)
                                                    .map(Map::entrySet)
                                                    .flatMap(Collection::stream)
                                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final boolean phoneHomeSuccess = blackDuckConnectionService.phoneHome(metadataMap);
        if (phoneHomeSuccess) {
            analyticsCollectors.forEach(AnalyticsCollector::clear);
        }

        return phoneHomeSuccess;
    }
}
