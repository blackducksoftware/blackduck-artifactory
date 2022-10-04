/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scaas;

import java.util.Collection;
import java.util.List;

import org.artifactory.repo.RepoPath;
import org.artifactory.request.Request;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.cancel.CancelDecider;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ScanAsAServiceModule implements Module {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ScanAsAServiceModuleConfig scanAsAServiceModuleConfig;

    private final Collection<CancelDecider> cancelDeciders;

    public ScanAsAServiceModule(ScanAsAServiceModuleConfig scanAsAServiceModuleConfig,
            Collection<CancelDecider> cancelDeciders) {
        this.scanAsAServiceModuleConfig = scanAsAServiceModuleConfig;
        this.cancelDeciders = cancelDeciders;
    }

    @Override
    public ScanAsAServiceModuleConfig getModuleConfig() {
        return scanAsAServiceModuleConfig;
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return null;
    }

    public void handleBeforeDownloadEvent(Request request, RepoPath repoPath) {
        cancelDeciders.forEach(cancelDecider -> cancelDecider.handleBeforeDownloadEvent(repoPath));
    }
}
