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
import java.net.URL;

import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.api.UriSingleResponse;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadBatch;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadRunner;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadTarget;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.rest.BlackDuckRestConnection;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jsonfield.JsonFieldResolver;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.rest.connection.RestConnection;

// TODO: Remove this class in favor of using blackduck-common directly
public class BlackDuckConnectionService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final HubServicesFactory hubServicesFactory;
    private final HubServerConfig hubServerConfig;

    private final ProjectService projectService;

    public BlackDuckConnectionService(final HubServerConfig hubServerConfig) {
        this.hubServerConfig = hubServerConfig;

        final BlackDuckRestConnection restConnection = this.hubServerConfig.createRestConnection(logger);
        final Gson gson = HubServicesFactory.createDefaultGson();
        this.hubServicesFactory = new HubServicesFactory(gson, HubServicesFactory.createDefaultJsonParser(), new JsonFieldResolver(gson), restConnection, logger);

        projectService = hubServicesFactory.createProjectService();
    }

    public RestConnection createRestConnection() {
        return hubServerConfig.createRestConnection(logger);
    }

    public void importBomFile(final String codeLocationName, final File bdioFile) throws IntegrationException {
        // TODO: Use CodeLocationCreationService in blackduck-common:40
        final UploadRunner uploadRunner = new UploadRunner(logger, getHubServicesFactory().createHubService());
        final UploadBatch uploadBatch = new UploadBatch();
        uploadBatch.addUploadTarget(UploadTarget.createDefault(codeLocationName, bdioFile));
        uploadRunner.executeUploads(uploadBatch);
    }

    // TODO: Take in a ProjectVersionView instead after blackduck-common:40 upgrade. projectService.addComponentToProjectVersion can accept a projectVersionView.
    public void addComponentToProjectVersion(final ExternalId componentExternalId, final String projectName, final String projectVersionName) throws IntegrationException {
        projectService.addComponentToProjectVersion(componentExternalId, projectName, projectVersionName);
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

    public URL getBlackDuckUrl() {
        return hubServerConfig.getBlackDuckUrl();
    }
}
