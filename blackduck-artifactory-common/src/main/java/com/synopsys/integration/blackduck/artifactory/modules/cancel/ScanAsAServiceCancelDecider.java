/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionComponentPolicyStatusType;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServiceBlockingStrategy;
import com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServiceModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServicePropertyService;

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

        if (!moduleConfig.getBlockingRepos().stream()
                .anyMatch(block -> {
                    // If any blocking repo matches these predicates then continue validation
                    // If no blocking repo matches then we can short circuit with no-cancellation
                    if (!block.contains("/") && block.equals(repoPath.getRepoKey())) {
                        return true;
                    } else if (block.contains("/")) {
                        // If block includes a branch, need to check against repo and path
                        String wholePath = repoPath.toPath();
                        // Not equal check since need to validate when block=repo/branch and repoAndPath=repo/branch/child
                        String regex = "^" + block + "/.*";
                        if (wholePath.matches(regex)) {
                            return true;
                        }
                    }
                    return false;
                }))
        {
            return CancelDecision.NO_CANCELLATION();
        }

        AtomicBoolean isBeforeCutoffTime = new AtomicBoolean(false);
        moduleConfig.getCutoffDateString().ifPresent(cutoffDateString ->
                {
                    long cutoffTime = moduleConfig.getDateTimeManager().getTimeFromString(cutoffDateString);
                    if (itemInfo.getLastUpdated() <= cutoffTime) {
                        logger.info(String.format("Item last updated prior to cutoff time; No Blocking Strategy applied; repo: %s", repoPath));
                        isBeforeCutoffTime.set(true);
                    }
                }
        );
        if (isBeforeCutoffTime.get()) {
            return CancelDecision.NO_CANCELLATION();
        }

        File file = new File(itemInfo.getName());
        Optional<List<String>> allowedNamePatterns = moduleConfig.getAllowedFileNamePatterns();
        Optional<List<String>> excludedNamePatterns = moduleConfig.getExcludedFileNamePatterns();

        if (applyBlockingStrategyBasedOnFilePattern(allowedNamePatterns.orElse(null), excludedNamePatterns.orElse(null), file)) {
            ScanAsAServiceBlockingStrategy blockingStrategy = moduleConfig.getBlockingStrategy();
            Optional<ProjectVersionComponentPolicyStatusType> policyViolationStatus = propertyService.getPolicyStatus(repoPath);
            return propertyService.getScanStatusProperty(repoPath).map(
                            scanStatus -> {
                                CancelDecision dec;
                                switch (scanStatus) {
                                case FAILED:
                                    switch (blockingStrategy) {
                                    case BLOCK_ALL:
                                        dec = CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; repo: %s", scanStatus.getMessage(), repoPath));
                                        break;
                                    case BLOCK_NONE:
                                        logger.info("Download NOT blocked; Blocking Strategy: {}; {}; repo: {}", blockingStrategy, scanStatus.getMessage(), repoPath);
                                        dec = CancelDecision.NO_CANCELLATION();
                                        break;
                                    case BLOCK_OFF:
                                        logger.info("Download NOT blocked; Blocking Strategy: {}; Download would have been blocked; {}; repo: {}",
                                                ScanAsAServiceBlockingStrategy.BLOCK_OFF, scanStatus.getMessage(), repoPath);
                                        dec = CancelDecision.NO_CANCELLATION();
                                        break;
                                    default:
                                        dec = CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; Unknown blocking strategy: %s; repo: %s", blockingStrategy, repoPath));
                                    }
                                    break;
                                case PROCESSING:
                                    switch (blockingStrategy) {
                                    case BLOCK_ALL:
                                        dec = CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; repo: %s", scanStatus.getMessage(), repoPath));
                                        break;
                                    case BLOCK_NONE:
                                        dec = CancelDecision.NO_CANCELLATION();
                                        break;
                                    case BLOCK_OFF:
                                        logger.info("Download NOT Blocked; Blocking Strategy: {}; {}; repo: {}", ScanAsAServiceBlockingStrategy.BLOCK_OFF,
                                                scanStatus.getMessage(), repoPath);
                                        dec = CancelDecision.NO_CANCELLATION();
                                        break;
                                    default:
                                        dec = CancelDecision.CANCEL_DOWNLOAD(
                                                String.format("Download blocked; Unknown blocking strategy: %s; repo: %s", blockingStrategy, repoPath));
                                    }
                                    break;
                                case SUCCESS:
                                    if (policyViolationStatus.isPresent()) {
                                        switch (policyViolationStatus.get()) {
                                        case NOT_IN_VIOLATION:
                                            dec = CancelDecision.NO_CANCELLATION();
                                            break;
                                        case IN_VIOLATION:
                                            if (ScanAsAServiceBlockingStrategy.BLOCK_OFF == blockingStrategy) {
                                                logger.info("Download NOT blocked; Blocking Strategy: {}; {}; repo: {}", ScanAsAServiceBlockingStrategy.BLOCK_OFF,
                                                        scanStatus.getMessage(), repoPath);
                                                dec = CancelDecision.NO_CANCELLATION();
                                            } else {
                                                dec = CancelDecision.CANCEL_DOWNLOAD(
                                                        String.format("Download blocked; %s; Policy Violation Status: %s; repo: %s", scanStatus.getMessage(),
                                                                policyViolationStatus.get(), repoPath));
                                            }
                                            break;
                                        default:
                                            if (ScanAsAServiceBlockingStrategy.BLOCK_OFF == blockingStrategy) {
                                                logger.info("Download NOT blocked; Blocking Strategy: {}; {}; Unknown policy violation status: {}; repo: {}",
                                                        ScanAsAServiceBlockingStrategy.BLOCK_OFF, scanStatus.getMessage(), policyViolationStatus.get(), repoPath);
                                                dec = CancelDecision.NO_CANCELLATION();
                                            } else {
                                                dec = CancelDecision.CANCEL_DOWNLOAD(
                                                        String.format("Download blocked; %s; Unknown policy violation status: %s; repo: %s",
                                                                scanStatus.getMessage(), policyViolationStatus.get(), repoPath));
                                            }
                                        }
                                    } else {
                                        logger.warn(String.format("%s; %s property not present; Using block strategy: %s; repo: %s", scanStatus.getMessage(),
                                                BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS.getPropertyName(), blockingStrategy, repoPath));
                                        switch (blockingStrategy) {
                                        case BLOCK_ALL:
                                            dec = CancelDecision.CANCEL_DOWNLOAD(
                                                    String.format("Download blocked; %s; %s not present; block strategy: %s; repo: %s", scanStatus.getMessage(),
                                                            BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS, blockingStrategy, repoPath));
                                            break;
                                        case BLOCK_NONE:
                                        case BLOCK_OFF:
                                            dec = CancelDecision.NO_CANCELLATION();
                                            break;
                                        default:
                                            dec = CancelDecision.CANCEL_DOWNLOAD(
                                                    String.format("Download blocked; %s not present; Unknown blocking strategy: %s; repo: %s",
                                                            BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS, blockingStrategy, repoPath));
                                        }
                                    }
                                    break;
                                default:
                                    logger.warn(String.format("Unknown scan status detected: %s", scanStatus));
                                    if (ScanAsAServiceBlockingStrategy.BLOCK_OFF == blockingStrategy) {
                                        logger.info("Download NOT blocked; Blocking Strategy: {}; Unknown ScanStatusProperty: {}; repo: {}",
                                                ScanAsAServiceBlockingStrategy.BLOCK_OFF, scanStatus, repoPath);
                                        dec = CancelDecision.NO_CANCELLATION();
                                    } else {
                                        dec = CancelDecision.CANCEL_DOWNLOAD(
                                                String.format("Download blocked; Unknown ScanStatusProperty: %s; repo: %s", scanStatus, repoPath));
                                    }
                                }
                                return dec;
                            })
                    .orElseGet(() -> {
                        switch (blockingStrategy) {
                        case BLOCK_ALL:
                            return CancelDecision.CANCEL_DOWNLOAD(
                                    String.format("Download blocked; Scan not scheduled and blocking strategy: %s; repo: %s", blockingStrategy, repoPath));
                        case BLOCK_NONE:
                            return CancelDecision.NO_CANCELLATION();
                        case BLOCK_OFF:
                            logger.info("Download NOT blocked; Blocking Strategy: {}; Scan not scheduled; repo: {}", ScanAsAServiceBlockingStrategy.BLOCK_OFF,
                                    repoPath);
                            return CancelDecision.NO_CANCELLATION();
                        default:
                            return CancelDecision.CANCEL_DOWNLOAD(
                                    String.format("Download blocked; Unknown blocking strategy: %s; repo: %s", blockingStrategy, repoPath));
                        }
                    });
        } else {
            logger.info("Download NOT blocked; File allowed/excluded pattern check failed; repo: {}", repoPath);
            return CancelDecision.NO_CANCELLATION();
        }
    }

    /**
     * Check to see if a given file is should be checked to see if the blocking strategy should be applied. If no allowed patterns are specified, then all
     * files are subject to the blocking strategy.
     *
     * @param allowedPatterns String list of the allowed file patterns; null if none set
     * @param excludedPatterns String list of the excluded file patterns; null if none set
     * @param file File to check
     * @return {@code true} if the pattern of the given file is in the allowedPatterns while at the same time NOT
     * in the excludedPatterns; {@code false}, otherwise
     */
    private boolean applyBlockingStrategyBasedOnFilePattern(List<String> allowedPatterns, List<String> excludedPatterns, File file) {
        AtomicBoolean ret = new AtomicBoolean(true);
        AtomicBoolean awcp = new AtomicBoolean(false);
        AtomicBoolean awcm = new AtomicBoolean(false);
        AtomicBoolean ewcp = new AtomicBoolean(false);
        AtomicBoolean ewcm = new AtomicBoolean(false);

        Optional<WildcardFileFilter> allowedWildCardFilter = Optional.ofNullable(allowedPatterns == null ? null : new WildcardFileFilter(allowedPatterns));
        Optional<WildcardFileFilter> excludedWildCardFilter = Optional.ofNullable(excludedPatterns == null ? null : new WildcardFileFilter(excludedPatterns));

        logger.debug("File: [{}]; Using allowed wildcard filter: [{}]", file.getName(), allowedWildCardFilter.orElse(null));
        logger.debug("File: [{}]; Using excluded wildcard filter: [{}]", file.getName(), excludedWildCardFilter.orElse(null));

        // Check to see if there are include file patterns to check against
        allowedWildCardFilter.ifPresentOrElse(allowedFilter -> {
            awcp.set(true);
            if (allowedFilter.accept(file)) {
                // Matches an include pattern set so now check if there are exclude patterns to check against
                awcm.set(true);
                excludedWildCardFilter.ifPresent(excludeFilter -> {
                    ewcp.set(true);
                    if (excludeFilter.accept(file)) {
                        // Exclude pattern matched, do NOT apply blocking strategy
                        ewcm.set(true);
                        ret.set(false);
                    }
                });
            } else {
                // Not in the include pattern set so do NOT apply blocking strategy
                ret.set(false);
            }
        }, () -> {
            // There are no include patterns so check if there are exclude patterns
            excludedWildCardFilter.ifPresent(excludeFilter -> {
                ewcp.set(true);
                if (excludeFilter.accept(file)) {
                    // Exclude pattern matched, do NOT apply blocking strategy
                    ewcm.set(true);
                    ret.set(false);
                }
            });
        });
        logger.debug("File: [{}]; Allowed Filter Evaluated: [{}]; Allowed Filter Matched: [{}]; Exclude Filter Evaluated: [{}]; Exclude Filter Matched: [{}]; Return Value: [{}]",
                file.getName(), awcp, awcm, ewcp, ewcm, ret);
        return ret.get();
    }
}
