/**
 * blackduck-artifactory-common
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.configuration.DirectoryConfig;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.phonehome.BlackDuckPhoneHomeHelper;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.phonehome.PhoneHomeResponse;

public class AnalyticsService {

    private final DirectoryConfig directoryConfig;
    private final BlackDuckServicesFactory blackDuckServicesFactory;

    private final List<AnalyticsCollector> analyticsCollectors = new ArrayList<>();

    public static AnalyticsService createFromBlackDuckServerConfig(final DirectoryConfig directoryConfig, final BlackDuckServerConfig blackDuckServerConfig) {
        final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(AnalyticsService.class));
        final BlackDuckServicesFactory blackDuckServicesFactory = blackDuckServerConfig.createBlackDuckServicesFactory(logger);

        return new AnalyticsService(directoryConfig, blackDuckServicesFactory);
    }

    public AnalyticsService(final DirectoryConfig directoryConfig, final BlackDuckServicesFactory blackDuckServicesFactory) {
        this.directoryConfig = directoryConfig;
        this.blackDuckServicesFactory = blackDuckServicesFactory;
    }

    public void registerAnalyzable(final Analyzable... analyzables) {
        for (final Analyzable analyzable : analyzables) {
            final List<AnalyticsCollector> analyticsCollector = analyzable.getAnalyticsCollectors();

            if (analyticsCollector != null) {
                this.analyticsCollectors.addAll(analyticsCollector);
            }
        }
    }

    /**
     * Submits a payload to phone home with data from all the collectors ({@link AnalyticsCollector})
     * This should be used infrequently such as once a day due to quota
     */
    public Boolean submitAnalytics() {
        // Flatten the metadata maps from all of the collectors
        final Map<String, String> metadataMap = analyticsCollectors.stream()
                                                    .map(AnalyticsCollector::getMetadataMap)
                                                    .map(Map::entrySet)
                                                    .flatMap(Collection::stream)
                                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final Boolean phoneHomeSuccess = phoneHome(metadataMap);
        if (phoneHomeSuccess) {
            analyticsCollectors.forEach(AnalyticsCollector::clear);
        }

        return phoneHomeSuccess;
    }

    private Boolean phoneHome(final Map<String, String> metadataMap) {
        try {
            String pluginVersion = "UNKNOWN_VERSION";
            final File versionFile = directoryConfig.getVersionFile();
            if (versionFile != null) {
                pluginVersion = FileUtils.readFileToString(versionFile, StandardCharsets.UTF_8);
            }

            String thirdPartyVersion = directoryConfig.getThirdPartyVersion();
            if (thirdPartyVersion == null) {
                thirdPartyVersion = "UNKNOWN_VERSION";
            }

            final Map<String, String> metadata = new HashMap<>(metadataMap);
            metadata.put("third.party.version", thirdPartyVersion);
            final BlackDuckPhoneHomeHelper phoneHomeHelper = BlackDuckPhoneHomeHelper.createPhoneHomeHelper(blackDuckServicesFactory);
            final PhoneHomeResponse phoneHomeResponse = phoneHomeHelper.handlePhoneHome("blackduck-artifactory", pluginVersion, metadata);

            return phoneHomeResponse.getImmediateResult();
        } catch (final Exception ignored) {
            // Phone home is not a critical operation
            return Boolean.FALSE;
        }
    }
}
