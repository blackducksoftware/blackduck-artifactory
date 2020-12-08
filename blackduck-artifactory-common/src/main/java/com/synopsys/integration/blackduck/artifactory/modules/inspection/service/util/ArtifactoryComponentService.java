/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.service.util;

import java.util.List;

import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VulnerabilityView;
import com.synopsys.integration.blackduck.http.BlackDuckRequestBuilder;
import com.synopsys.integration.blackduck.http.BlackDuckRequestFactory;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.blackduck.service.model.ComponentVersionVulnerabilities;
import com.synopsys.integration.exception.IntegrationException;

// TODO: Remove when ComponentService uses the updated mime type.
public class ArtifactoryComponentService {
    private final BlackDuckRequestFactory blackDuckRequestFactory;
    private final BlackDuckApiClient blackDuckApiClient;

    public ArtifactoryComponentService(BlackDuckRequestFactory blackDuckRequestFactory, BlackDuckApiClient blackDuckApiClient) {
        this.blackDuckRequestFactory = blackDuckRequestFactory;
        this.blackDuckApiClient = blackDuckApiClient;
    }

    public ComponentVersionVulnerabilities getComponentVersionVulnerabilities(ComponentVersionView componentVersion) throws IntegrationException {
        BlackDuckRequestBuilder requestBuilder = blackDuckRequestFactory.createCommonGetRequestBuilder()
                                                     .acceptMimeType("application/vnd.blackducksoftware.vulnerability-4+json");
        List<VulnerabilityView> vulnerabilityList = blackDuckApiClient.getAllResponses(componentVersion, ComponentVersionView.VULNERABILITIES_LINK_RESPONSE, requestBuilder);
        return new ComponentVersionVulnerabilities(componentVersion, vulnerabilityList);
    }
}
