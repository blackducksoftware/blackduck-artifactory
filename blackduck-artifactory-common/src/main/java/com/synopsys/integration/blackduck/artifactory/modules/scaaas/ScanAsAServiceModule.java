/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scaaas;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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

    private static final String BLACKDUCK_SCAN_AS_A_SERVICE_DOWNLOAD_PARAM = "X-BD-SCAN-AS-A-SERVICE-SCANNER-REQUEST";

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
        // If the request comes from the Scan-as-a-Service Scanner, the following Header will be present in the
        // Request; Allow the download
        logger.debug(String.format("Request headers: %s", request.getHeaders().entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(";"))));
        if (!Boolean.TRUE.equals(Boolean.valueOf(request.getHeader(BLACKDUCK_SCAN_AS_A_SERVICE_DOWNLOAD_PARAM)))) {
            cancelDeciders.forEach(cancelDecider -> cancelDecider.handleBeforeDownloadEvent(repoPath));
        } else {
            logger.info(String.format("BlackDuck Header present; Allowing download: repo: %s", repoPath));
        }
    }
}
