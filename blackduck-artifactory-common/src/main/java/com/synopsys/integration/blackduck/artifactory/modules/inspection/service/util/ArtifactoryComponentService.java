/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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
