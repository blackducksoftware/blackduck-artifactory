/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.deleteme;

import java.util.Arrays;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;

// TODO: blackduck-common:55.0.0 will support parsing UUIDs from BlackDuckUrls and should be used in the next release of blackduck-artifactory. - JM 05/2021
public class ComponentVersionIdUtil {

    private ComponentVersionIdUtil() {}

    public static String extractComponentVersionId(String componentVersionUrl) throws IntegrationException {
        TempBlackDuckUrl tempBlackDuckUrl = new TempBlackDuckUrl(new HttpUrl(componentVersionUrl));
        return tempBlackDuckUrl.parseId(Arrays.asList(TempBlackDuckUrlSearchTerm.COMPONENTS, TempBlackDuckUrlSearchTerm.VERSIONS));
    }

}

