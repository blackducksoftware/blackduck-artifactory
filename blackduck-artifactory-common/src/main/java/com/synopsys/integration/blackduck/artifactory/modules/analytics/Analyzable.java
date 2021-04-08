/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.analytics;

import java.util.List;

import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.AnalyticsCollector;

public interface Analyzable {
    List<AnalyticsCollector> getAnalyticsCollectors();
}
