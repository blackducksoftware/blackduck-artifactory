/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules;

import com.synopsys.integration.blackduck.artifactory.modules.analytics.Analyzable;

public interface Module extends Analyzable {
    ModuleConfig getModuleConfig();
}
