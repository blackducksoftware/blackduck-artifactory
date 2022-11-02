/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import java.util.Optional;

import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionComponentPolicyStatusType;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
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
        Optional<ProjectVersionComponentPolicyStatusType> policyViolationStatus = propertyService.getPolicyStatus(repoPath);
        return propertyService.getScanStatusProperty(repoPath).map(
                        scanStatus -> {
                            CancelDecision dec;
                            switch (scanStatus) {
                            case FAILED:
                                dec = CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; repo: %s", scanStatus.getMessage(), repoPath));
                                break;
                            case PROCESSING:
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
                            case SUCCESS:
                                if(policyViolationStatus.isPresent()) {
                                    switch (policyViolationStatus.get()) {
                                    case NOT_IN_VIOLATION:
                                        dec = CancelDecision.NO_CANCELLATION();
                                        break;
                                    case IN_VIOLATION:
                                        dec = CancelDecision.CANCEL_DOWNLOAD(
                                                String.format("Download blocked; %s; Policy Violation Status: %s; repo: %s", scanStatus.getMessage(), policyViolationStatus.get(), repoPath));
                                        break;
                                    default:
                                        dec = CancelDecision.CANCEL_DOWNLOAD(
                                                String.format("Download blocked; %s; Unknown policy violation status: %s; repo: %s", scanStatus.getMessage(), policyViolationStatus.get(), repoPath));
                                    }
                                } else {
                                    logger.warn(String.format("%s; %s property not present; Using block strategy: %s; repo: %s", scanStatus.getMessage(),
                                            BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS.getPropertyName(), blockingStrategy, repoPath));
                                    switch (blockingStrategy) {
                                    case BLOCK_ALL:
                                        dec = CancelDecision.CANCEL_DOWNLOAD(
                                            String.format("Download blocked; %s; %s not present; block strategy: %s; repo: %s", scanStatus.getMessage(), BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS, blockingStrategy, repoPath));
                                        break;
                                    case BLOCK_NONE:
                                        dec = CancelDecision.NO_CANCELLATION();
                                        break;
                                    default:
                                        dec = CancelDecision.CANCEL_DOWNLOAD(
                                                String.format("Download blocked; %s not present; Unknown blocking strategy: %s; repo: %s", BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS, blockingStrategy, repoPath));
                                    }
                                }
                                break;
                            default:
                                logger.warn(String.format("Unknown scan status detected: %s", scanStatus));
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
