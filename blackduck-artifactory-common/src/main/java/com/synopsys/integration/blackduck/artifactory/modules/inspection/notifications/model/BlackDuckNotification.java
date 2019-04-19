package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model;

import java.util.List;

import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.manual.component.AffectedProjectVersion;

public class BlackDuckNotification {
    private final List<AffectedProjectVersion> affectedProjectVersions;
    private final ComponentVersionView componentVersionView;

    public BlackDuckNotification(final List<AffectedProjectVersion> affectedProjectVersions, final ComponentVersionView componentVersionView) {
        this.affectedProjectVersions = affectedProjectVersions;
        this.componentVersionView = componentVersionView;
    }

    public List<AffectedProjectVersion> getAffectedProjectVersions() {
        return affectedProjectVersions;
    }

    public ComponentVersionView getComponentVersionView() {
        return componentVersionView;
    }
}
