/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.model;

import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionComponentView;

public class ComponentViewWrapper {
    private final ProjectVersionComponentView projectVersionComponentView;
    private final ComponentVersionView componentVersionView;

    public ComponentViewWrapper(ProjectVersionComponentView projectVersionComponentView, ComponentVersionView componentVersionView) {
        this.projectVersionComponentView = projectVersionComponentView;
        this.componentVersionView = componentVersionView;
    }

    public ProjectVersionComponentView getProjectVersionComponentView() {
        return projectVersionComponentView;
    }

    public ComponentVersionView getComponentVersionView() {
        return componentVersionView;
    }
}
