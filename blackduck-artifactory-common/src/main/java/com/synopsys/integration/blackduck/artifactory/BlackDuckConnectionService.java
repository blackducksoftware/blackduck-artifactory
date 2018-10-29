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
package com.synopsys.integration.blackduck.artifactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.rest.BlackduckRestConnection;
import com.synopsys.integration.blackduck.service.CodeLocationService;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.BlackDuckPhoneHomeCallable;
import com.synopsys.integration.exception.EncryptionException;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.phonehome.PhoneHomeClient;
import com.synopsys.integration.phonehome.PhoneHomeService;
import com.synopsys.integration.phonehome.google.analytics.GoogleAnalyticsConstants;

public class BlackDuckConnectionService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final PluginConfig pluginConfig;
    private final PhoneHomeClient phoneHomeClient;

    private final HubServicesFactory hubServicesFactory;
    private final HubServerConfig hubServerConfig;

    public BlackDuckConnectionService(final PluginConfig pluginConfig, final HubServerConfig hubServerConfig) throws EncryptionException {
        this.pluginConfig = pluginConfig;
        this.hubServerConfig = hubServerConfig;

        final BlackduckRestConnection restConnection = this.hubServerConfig.createRestConnection(logger);
        this.hubServicesFactory = new HubServicesFactory(HubServicesFactory.createDefaultGson(), HubServicesFactory.createDefaultJsonParser(), restConnection, logger);

        final String googleAnalyticsTrackingId = GoogleAnalyticsConstants.TEST_INTEGRATIONS_TRACKING_ID; // TODO: Replace with real tracking id
        final HttpClientBuilder httpClientBuilder = hubServerConfig.createRestConnection(logger).getClientBuilder();
        phoneHomeClient = new PhoneHomeClient(googleAnalyticsTrackingId, logger, httpClientBuilder, HubServicesFactory.createDefaultGson());
    }

    public Boolean phoneHome(final Map<String, String> metadataMap) {
        Boolean result = Boolean.FALSE;

        try {
            String pluginVersion = null;
            final File versionFile = pluginConfig.getVersionFile();
            if (versionFile != null) {
                pluginVersion = FileUtils.readFileToString(versionFile, StandardCharsets.UTF_8);
            }

            result = phoneHome(pluginVersion, pluginConfig.getThirdPartyVersion(), metadataMap);
        } catch (final Exception ignored) {
            // Phone home is not a critical operation
        }

        return result;
    }

    private Boolean phoneHome(final String reportedPluginVersion, final String reportedThirdPartyVersion, final Map<String, String> metadataMap) {
        String pluginVersion = reportedPluginVersion;
        String thirdPartyVersion = reportedThirdPartyVersion;

        if (pluginVersion == null) {
            pluginVersion = "UNKNOWN_VERSION";
        }

        if (thirdPartyVersion == null) {
            thirdPartyVersion = "UNKNOWN_VERSION";
        }

        final BlackDuckPhoneHomeCallable blackDuckPhoneHomeCallable = new BlackDuckPhoneHomeCallable(
            logger,
            phoneHomeClient,
            hubServerConfig.getHubUrl(),
            "blackduck-artifactory",
            pluginVersion,
            hubServicesFactory.getEnvironmentVariables(),
            hubServicesFactory.createHubService(),
            hubServicesFactory.createHubRegistrationService()
        );
        blackDuckPhoneHomeCallable.addMetaData("third.party.version", thirdPartyVersion);
        blackDuckPhoneHomeCallable.addAllMetadata(metadataMap);
        final PhoneHomeService phoneHomeService = hubServicesFactory.createPhoneHomeService(Executors.newSingleThreadExecutor());

        return phoneHomeService.phoneHome(blackDuckPhoneHomeCallable);
    }

    public void importBomFile(final File bdioFile) throws IntegrationException {
        final CodeLocationService codeLocationService = hubServicesFactory.createCodeLocationService();
        codeLocationService.importBomFile(bdioFile);
    }

    public void addComponentToProjectVersion(final ExternalId componentExternalId, final String projectName, final String projectVersionName) throws IntegrationException {
        final ProjectService projectService = hubServicesFactory.createProjectService();
        projectService.addComponentToProjectVersion(componentExternalId, projectName, projectVersionName);
    }

    public HubServicesFactory getHubServicesFactory() {
        return hubServicesFactory;
    }
}
