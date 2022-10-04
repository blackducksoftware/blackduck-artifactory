/*
 * Copyright (C) 2022 Synopsys Inc.
 * http://www.synopsys.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Synopsys ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Synopsys.
 */

package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.scaas.ScanAsAServiceBlockingStrategy;
import com.synopsys.integration.blackduck.artifactory.modules.scaas.ScanAsAServiceModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.scaas.ScanAsAServicePropertyService;

public class ScanAsAServiceCancelDecider implements CancelDecider {
    private final Logger logger = LoggerFactory.getLogger(ScanAsAServiceCancelDecider.class);

    private final ScanAsAServiceModuleConfig moduleConfig;

    private final ScanAsAServicePropertyService propertyService;

    private final ArtifactoryPAPIService artifactoryPAPIService;

    public ScanAsAServiceCancelDecider(ScanAsAServiceModuleConfig moduleConfig,
            ScanAsAServicePropertyService propertyService,
            ArtifactoryPAPIService artifactoryPAPIService) {
        this.moduleConfig = moduleConfig;
        this.propertyService = propertyService;
        this.artifactoryPAPIService = artifactoryPAPIService;
    }

    @Override
    public CancelDecision getCancelDecision(RepoPath repoPath) {
        ItemInfo itemInfo = artifactoryPAPIService.getItemInfo(repoPath);
        if (itemInfo.isFolder()) {
            return CancelDecision.NO_CANCELLATION();
        }

        ScanAsAServiceBlockingStrategy blockingStrategy = moduleConfig.getBlockingStrategy();
        return propertyService.getScanStatusProperty(repoPath).map(
                        scanStatus -> {
                            CancelDecision dec;
                            switch (scanStatus) {
                            case FAILED:
                            case SUCCESS_POLICY_VIOLATION:
                                dec = CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; repo: %s", scanStatus.getMessage(), repoPath));
                                break;
                            case SCAN_IN_PROGRESS:
                                switch (blockingStrategy) {
                                case BLOCK_ALL:
                                    dec = CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; repo: %s", scanStatus.getMessage(), repoPath));
                                    break;
                                case BLOCK_NONE:
                                    dec = CancelDecision.NO_CANCELLATION();
                                    break;
                                default:
                                    dec = CancelDecision.CANCEL_DOWNLOAD(
                                            String.format("Download blocked; Unknown blocking strategy: %s; repo: %s", blockingStrategy, repoPath));
                                }
                                break;
                            case SUCCESS_NO_POLICY_VIOLATION:
                                dec = CancelDecision.NO_CANCELLATION();
                                break;
                            default:
                                dec = CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; Unknown ScanStatusProperty: %s; repo: %s", scanStatus, repoPath));
                            }
                            return dec;
                        })
                .orElseGet(() ->{
                        switch (blockingStrategy) {
                            case BLOCK_ALL:
                                return CancelDecision.CANCEL_DOWNLOAD(
                                        String.format("Download blocked; Scan not scheduled and blocking strategy: %s; repo: %s", blockingStrategy, repoPath));
                            case BLOCK_NONE:
                                return CancelDecision.NO_CANCELLATION();
                            default:
                                return CancelDecision.CANCEL_DOWNLOAD(
                                        String.format("Download blocked; Unknown blocking strategy: %s; repo: %s", blockingStrategy, repoPath));
                        }
                });
    }
}
