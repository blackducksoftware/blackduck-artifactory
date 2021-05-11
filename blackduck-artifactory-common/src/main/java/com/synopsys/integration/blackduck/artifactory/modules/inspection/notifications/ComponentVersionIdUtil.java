/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.synopsys.integration.blackduck.api.generated.discovery.BlackDuckMediaTypeDiscovery;
import com.synopsys.integration.exception.IntegrationException;

public class ComponentVersionIdUtil {
    private static final Pattern COMPONENT_VERSION_ID_PATTERN = Pattern.compile(String.format(".*/components/%s/versions/(%s).*", BlackDuckMediaTypeDiscovery.UUID_REGEX, BlackDuckMediaTypeDiscovery.UUID_REGEX));

    private ComponentVersionIdUtil() {}

    public static String extractComponentVersionId(String componentVersionUrl) throws IntegrationException {
        Matcher matcher = COMPONENT_VERSION_ID_PATTERN.matcher(componentVersionUrl);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            throw new IntegrationException(String.format("Component Version URL does not match pattern %s", COMPONENT_VERSION_ID_PATTERN.pattern()));
        }
    }
}
