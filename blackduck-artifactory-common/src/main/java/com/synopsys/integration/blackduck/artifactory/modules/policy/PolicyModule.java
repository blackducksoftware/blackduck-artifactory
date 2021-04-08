/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.policy;

import java.util.Collections;
import java.util.List;

import org.artifactory.exception.CancelException;
import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.FeatureAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.cancel.CancelDecider;

public class PolicyModule implements Module {
    private final PolicyModuleConfig policyModuleConfig;
    private final FeatureAnalyticsCollector featureAnalyticsCollector;
    private final CancelDecider cancelDecider;

    public PolicyModule(PolicyModuleConfig policyModuleConfig, FeatureAnalyticsCollector featureAnalyticsCollector, CancelDecider cancelDecider) {
        this.policyModuleConfig = policyModuleConfig;
        this.featureAnalyticsCollector = featureAnalyticsCollector;
        this.cancelDecider = cancelDecider;
    }

    public void handleBeforeDownloadEvent(RepoPath repoPath) {
        try {
            cancelDecider.handleBeforeDownloadEvent(repoPath);
            logFeatureHit(false);
        } catch (CancelException cancelException) {
            logFeatureHit(true);
            throw cancelException;
        }
    }

    private void logFeatureHit(boolean wasDownloadBlocked) {
        featureAnalyticsCollector.logFeatureHit("handleBeforeDownloadEvent", String.format("blocked:%s", wasDownloadBlocked));
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return Collections.singletonList(featureAnalyticsCollector);
    }

    @Override
    public PolicyModuleConfig getModuleConfig() {
        return policyModuleConfig;
    }
}
