/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.analytics.service;

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
import com.synopsys.integration.blackduck.artifactory.modules.analytics.Analyzable;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.AnalyticsCollector;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.phonehome.BlackDuckPhoneHomeHelper;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.phonehome.PhoneHomeResponse;
import com.synopsys.integration.util.NoThreadExecutorService;

public class AnalyticsService {

    private final DirectoryConfig directoryConfig;
    private final BlackDuckServicesFactory blackDuckServicesFactory;

    private final List<AnalyticsCollector> analyticsCollectors = new ArrayList<>();

    public AnalyticsService(DirectoryConfig directoryConfig, BlackDuckServicesFactory blackDuckServicesFactory) {
        this.directoryConfig = directoryConfig;
        this.blackDuckServicesFactory = blackDuckServicesFactory;
    }

    public static AnalyticsService createFromBlackDuckServerConfig(DirectoryConfig directoryConfig, BlackDuckServerConfig blackDuckServerConfig) {
        IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(AnalyticsService.class));
        BlackDuckServicesFactory blackDuckServicesFactory = blackDuckServerConfig.createBlackDuckServicesFactory(logger);

        return new AnalyticsService(directoryConfig, blackDuckServicesFactory);
    }

    public void registerAnalyzable(Analyzable... analyzables) {
        for (Analyzable analyzable : analyzables) {
            List<AnalyticsCollector> analyticsCollector = analyzable.getAnalyticsCollectors();

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
        // Flatten the notifications maps from all of the collectors
        Map<String, String> metadataMap = analyticsCollectors.stream()
                                                    .map(AnalyticsCollector::getMetadataMap)
                                                    .map(Map::entrySet)
                                                    .flatMap(Collection::stream)
                                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Boolean phoneHomeSuccess = phoneHome(metadataMap);
        if (phoneHomeSuccess) {
            analyticsCollectors.forEach(AnalyticsCollector::clear);
        }

        return phoneHomeSuccess;
    }

    private Boolean phoneHome(Map<String, String> metadataMap) {
        try {
            String pluginVersion = "UNKNOWN_VERSION";
            File versionFile = directoryConfig.getVersionFile();
            if (versionFile != null) {
                pluginVersion = FileUtils.readFileToString(versionFile, StandardCharsets.UTF_8);
            }

            String thirdPartyVersion = directoryConfig.getThirdPartyVersion();
            if (thirdPartyVersion == null) {
                thirdPartyVersion = "UNKNOWN_VERSION";
            }

            Map<String, String> metadata = new HashMap<>(metadataMap);
            metadata.put("third.party.version", thirdPartyVersion);
            BlackDuckPhoneHomeHelper phoneHomeHelper = BlackDuckPhoneHomeHelper.createAsynchronousPhoneHomeHelper(blackDuckServicesFactory, new NoThreadExecutorService());
            PhoneHomeResponse phoneHomeResponse = phoneHomeHelper.handlePhoneHome("blackduck-artifactory", pluginVersion, metadata);

            return phoneHomeResponse.getImmediateResult();
        } catch (Exception ignored) {
            // Phone home is not a critical operation
            return Boolean.FALSE;
        }
    }
}
