/**
 * blackduck-artifactory-common
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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.BlackDuckConnectionService;
import com.synopsys.integration.blackduck.artifactory.configuration.DirectoryConfig;
import com.synopsys.integration.blackduck.phonehome.BlackDuckPhoneHomeCallable;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.phonehome.PhoneHomeClient;
import com.synopsys.integration.phonehome.PhoneHomeRequestBody;
import com.synopsys.integration.phonehome.PhoneHomeService;

public class AnalyticsService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final DirectoryConfig directoryConfig;
    private final BlackDuckConnectionService blackDuckConnectionService;

    private final PhoneHomeClient phoneHomeClient;
    private final List<AnalyticsCollector> analyticsCollectors = new ArrayList<>();

    public AnalyticsService(final DirectoryConfig directoryConfig, final BlackDuckConnectionService blackDuckConnectionService, final String googleAnalyticsTrackingId) {
        this.directoryConfig = directoryConfig;
        this.blackDuckConnectionService = blackDuckConnectionService;

        final HttpClientBuilder httpClientBuilder = blackDuckConnectionService.createRestConnection().getClientBuilder();
        phoneHomeClient = new PhoneHomeClient(googleAnalyticsTrackingId, logger, httpClientBuilder, HubServicesFactory.createDefaultGson());
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

        final boolean phoneHomeSuccess = phoneHome(metadataMap);
        if (phoneHomeSuccess) {
            analyticsCollectors.forEach(AnalyticsCollector::clear);
        }

        return phoneHomeSuccess;
    }

    public Boolean phoneHome(final Map<String, String> metadataMap) {
        Boolean result = Boolean.FALSE;

        try {
            String pluginVersion = null;
            final File versionFile = directoryConfig.getVersionFile();
            if (versionFile != null) {
                pluginVersion = FileUtils.readFileToString(versionFile, StandardCharsets.UTF_8);
            }

            result = phoneHome(pluginVersion, directoryConfig.getThirdPartyVersion(), metadataMap);
        } catch (final Exception ignored) {
            // Phone home is not a critical operation
        }

        return result;
    }

    private Boolean phoneHome(final String reportedPluginVersion, final String reportedThirdPartyVersion, final Map<String, String> metadataMap) {
        final HubServicesFactory hubServicesFactory = blackDuckConnectionService.getHubServicesFactory();
        String pluginVersion = reportedPluginVersion;
        String thirdPartyVersion = reportedThirdPartyVersion;

        if (pluginVersion == null) {
            pluginVersion = "UNKNOWN_VERSION";
        }

        if (thirdPartyVersion == null) {
            thirdPartyVersion = "UNKNOWN_VERSION";
        }

        final PhoneHomeService phoneHomeService = hubServicesFactory.createPhoneHomeService(Executors.newSingleThreadExecutor());
        final PhoneHomeRequestBody.Builder phoneHomeRequestBodyBuilder = new PhoneHomeRequestBody.Builder();
        phoneHomeRequestBodyBuilder.addToMetaData("third.party.version", thirdPartyVersion);
        phoneHomeRequestBodyBuilder.addAllToMetaData(metadataMap);
        final BlackDuckPhoneHomeCallable blackDuckPhoneHomeCallable = new BlackDuckPhoneHomeCallable(
            logger,
            phoneHomeClient,
            blackDuckConnectionService.getBlackDuckUrl(),
            "blackduck-artifactory",
            pluginVersion,
            hubServicesFactory.getEnvironmentVariables(),
            hubServicesFactory.createHubService(),
            hubServicesFactory.createHubRegistrationService(),
            phoneHomeRequestBodyBuilder
        );

        return phoneHomeService.phoneHome(blackDuckPhoneHomeCallable);
    }
}
