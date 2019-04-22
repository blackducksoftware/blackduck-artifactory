package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model;

import java.util.List;

import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.PolicyStatusView;
import com.synopsys.integration.util.NameVersion;

public class PolicyStatusNotification extends BlackDuckNotification {
    private final PolicyStatusView policyStatusView;

    public PolicyStatusNotification(final List<NameVersion> affectedProjectVersions, final ComponentVersionView componentVersionView, final PolicyStatusView policyStatusView) {
        super(affectedProjectVersions, componentVersionView);
        this.policyStatusView = policyStatusView;
    }

    public PolicyStatusView getPolicyStatusView() {
        return policyStatusView;
    }
}
