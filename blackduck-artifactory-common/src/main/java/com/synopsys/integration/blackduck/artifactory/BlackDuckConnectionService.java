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
package com.synopsys.integration.blackduck.artifactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.api.UriSingleResponse;
import com.synopsys.integration.blackduck.api.generated.view.ComponentSearchResultView;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadBatch;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadRunner;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadTarget;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.phonehome.BlackDuckPhoneHomeCallable;
import com.synopsys.integration.blackduck.rest.BlackDuckRestConnection;
import com.synopsys.integration.blackduck.service.ComponentService;
import com.synopsys.integration.blackduck.service.HubService;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jsonfield.JsonFieldResolver;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.phonehome.PhoneHomeClient;
import com.synopsys.integration.phonehome.PhoneHomeRequestBody;
import com.synopsys.integration.phonehome.PhoneHomeService;

public class BlackDuckConnectionService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final PluginConfig pluginConfig;
    private final PhoneHomeClient phoneHomeClient;

    private final HubServicesFactory hubServicesFactory;
    private final HubServerConfig hubServerConfig;

    public BlackDuckConnectionService(final PluginConfig pluginConfig, final HubServerConfig hubServerConfig, final String googleAnalyticsTrackingId) {
        this.pluginConfig = pluginConfig;
        this.hubServerConfig = hubServerConfig;

        final BlackDuckRestConnection restConnection = this.hubServerConfig.createRestConnection(logger);
        final Gson gson = HubServicesFactory.createDefaultGson();
        this.hubServicesFactory = new HubServicesFactory(gson, HubServicesFactory.createDefaultJsonParser(), new JsonFieldResolver(gson), restConnection, logger);

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

        final PhoneHomeService phoneHomeService = hubServicesFactory.createPhoneHomeService(Executors.newSingleThreadExecutor());
        final PhoneHomeRequestBody.Builder phoneHomeRequestBodyBuilder = new PhoneHomeRequestBody.Builder();
        phoneHomeRequestBodyBuilder.addToMetaData("third.party.version", thirdPartyVersion);
        phoneHomeRequestBodyBuilder.addAllToMetaData(metadataMap);
        final BlackDuckPhoneHomeCallable blackDuckPhoneHomeCallable = new BlackDuckPhoneHomeCallable(
            logger,
            phoneHomeClient,
            hubServerConfig.getBlackDuckUrl(),
            "blackduck-artifactory",
            pluginVersion,
            hubServicesFactory.getEnvironmentVariables(),
            hubServicesFactory.createHubService(),
            hubServicesFactory.createHubRegistrationService(),
            phoneHomeRequestBodyBuilder
        );

        return phoneHomeService.phoneHome(blackDuckPhoneHomeCallable);
    }

    public void importBomFile(final String codeLocationName, final File bdioFile) throws IntegrationException {
        // TODO: Use CodeLocationCreationService in blackduck-common:40
        final UploadRunner uploadRunner = new UploadRunner(logger, getHubServicesFactory().createHubService());
        final UploadBatch uploadBatch = new UploadBatch();
        uploadBatch.addUploadTarget(UploadTarget.createDefault(codeLocationName, bdioFile));
        uploadRunner.executeUploads(uploadBatch);
    }

    // TODO: Take in a ProjectVersionView instead after blackduck-common:40 upgrade. projectService.addComponentToProjectVersion can accept a projectVersionView.
    public Optional<String> addComponentToProjectVersion(final ExternalId componentExternalId, final String projectName, final String projectVersionName) throws IntegrationException {
        final HubService hubService = hubServicesFactory.createHubService();
        final ProjectService projectService = hubServicesFactory.createProjectService();
        final ComponentService componentService = hubServicesFactory.createComponentService();
        String componentVersionUrl = null;
        // projectService.addComponentToProjectVersion(componentExternalId, projectName, projectVersionName);

        // TODO: projectService.addComponentToProjectVersion should return the componentVersionUrl in blackduck-common:40 so the below code won't be necessary.
        final Optional<ProjectView> projectItem = projectService.getProjectByName(projectName);
        if (projectItem.isPresent()) {
            final Optional<ProjectVersionView> projectVersionView = projectService.getProjectVersion(projectItem.get(), projectVersionName);
            if (projectVersionView.isPresent()) {
                final String projectVersionComponentsUrl = hubService.getFirstLink(projectVersionView.get(), ProjectVersionView.COMPONENTS_LINK);
                final ComponentSearchResultView componentSearchResultView = componentService.getExactComponentMatch(componentExternalId);
                if (StringUtils.isNotBlank(componentSearchResultView.variant)) {
                    componentVersionUrl = componentSearchResultView.variant;
                } else {
                    componentVersionUrl = componentSearchResultView.version;
                }
                projectService.addComponentToProjectVersion("application/json", projectVersionComponentsUrl, componentVersionUrl);
            }
        }

        return Optional.ofNullable(componentVersionUrl);
    }

    // not a good practice, but right now, I do not know a better way, short of searching the entire BOM, to match up a BOM component with a component/version
    // ejk - 2018-01-15
    public UriSingleResponse<VersionBomComponentView> getVersionBomComponentUriResponse(final UriSingleResponse<ProjectVersionView> projectVersionUriResponse, final UriSingleResponse<ComponentVersionView> componentVersionUriResponse) {
        final String projectVersionUri = projectVersionUriResponse.uri;
        final String componentVersionUri = componentVersionUriResponse.uri;
        final String apiComponentsLinkPrefix = "/api/components/";
        final int apiComponentsStart = componentVersionUri.indexOf(apiComponentsLinkPrefix) + apiComponentsLinkPrefix.length();
        final String versionBomComponentUri = projectVersionUri + "/components/" + componentVersionUri.substring(apiComponentsStart);
        return new UriSingleResponse<>(versionBomComponentUri, VersionBomComponentView.class);
    }

    public HubServicesFactory getHubServicesFactory() {
        return hubServicesFactory;
    }
}
