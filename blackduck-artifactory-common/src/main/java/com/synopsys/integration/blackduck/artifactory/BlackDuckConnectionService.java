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

import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.api.UriSingleResponse;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadBatch;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadRunner;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadTarget;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

// TODO: Remove this class in favor of using blackduck-common directly
public class BlackDuckConnectionService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final BlackDuckServicesFactory blackDuckServicesFactory;

    private final ProjectService projectService;

    public BlackDuckConnectionService(final BlackDuckServerConfig BlackDuckServerConfig) {
        this.blackDuckServicesFactory = BlackDuckServerConfig.createBlackDuckServicesFactory(logger);

        projectService = blackDuckServicesFactory.createProjectService();
    }

    public void importBomFile(final String codeLocationName, final File bdioFile) throws IntegrationException {
        // TODO: Use CodeLocationCreationService in blackduck-common:40
        final UploadRunner uploadRunner = new UploadRunner(logger, getBlackDuckServicesFactory().createBlackDuckService());
        final UploadBatch uploadBatch = new UploadBatch();
        uploadBatch.addUploadTarget(UploadTarget.createDefault(codeLocationName, bdioFile));
        uploadRunner.executeUploads(uploadBatch);
    }

    // TODO: Take in a ProjectVersionView instead after blackduck-common:40 upgrade. projectService.addComponentToProjectVersion can accept a projectVersionView.
    public void addComponentToProjectVersion(final ExternalId componentExternalId, final ProjectVersionView projectVersionView) throws IntegrationException {
        projectService.addComponentToProjectVersion(componentExternalId, projectVersionView);
    }

    // not a good practice, but right now, I do not know a better way, short of searching the entire BOM, to match up a BOM component with a component/version
    // ejk - 2018-01-15
    public UriSingleResponse<VersionBomComponentView> getVersionBomComponentUriResponse(final UriSingleResponse<ProjectVersionView> projectVersionUriResponse, final UriSingleResponse<ComponentVersionView> componentVersionUriResponse) {
        final String projectVersionUri = projectVersionUriResponse.getUri();
        final String componentVersionUri = componentVersionUriResponse.getUri();
        final String apiComponentsLinkPrefix = "/api/components/";
        final int apiComponentsStart = componentVersionUri.indexOf(apiComponentsLinkPrefix) + apiComponentsLinkPrefix.length();
        final String versionBomComponentUri = projectVersionUri + "/components/" + componentVersionUri.substring(apiComponentsStart);
        return new UriSingleResponse<>(versionBomComponentUri, VersionBomComponentView.class);
    }

    public BlackDuckServicesFactory getBlackDuckServicesFactory() {
        return blackDuckServicesFactory;
    }
}
