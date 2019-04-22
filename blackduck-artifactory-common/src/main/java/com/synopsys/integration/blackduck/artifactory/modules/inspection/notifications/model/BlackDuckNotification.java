package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model;

import java.util.List;

import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.util.NameVersion;

public class BlackDuckNotification {
    private final List<NameVersion> affectedProjectVersions;
    private final ComponentVersionView componentVersionView;

    public BlackDuckNotification(final List<NameVersion> affectedProjectVersions, final ComponentVersionView componentVersionView) {
        this.affectedProjectVersions = affectedProjectVersions;
        this.componentVersionView = componentVersionView;
    }

    public List<NameVersion> getAffectedProjectVersions() {
        return affectedProjectVersions;
    }

    public ComponentVersionView getComponentVersionView() {
        return componentVersionView;
    }
}
