/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionComponentPolicyStatusType;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServiceBlockingStrategy;
import com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServiceModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServicePropertyService;

public class ScanAsAServiceCancelDecider implements CancelDecider {
    private final Logger logger = LoggerFactory.getLogger(ScanAsAServiceCancelDecider.class);

    private final ScanAsAServiceModuleConfig moduleConfig;

    private final ScanAsAServicePropertyService propertyService;

    private final PluginRepoPathFactory repoPathFactory;

    private final ArtifactoryPAPIService artifactoryPAPIService;

    private final BiFunction<String, RepoPath, Boolean> pathInManagedRepo = (block, repo) -> {
        // If any blocking repo matches these predicates then continue validation
        // If no blocking repo matches then we can short circuit with no-cancellation
        if (!block.contains("/") && block.equals(repo.getRepoKey())) {
            logger.trace(String.format("Found folder match; repo: %s", repo));
            return true;
        } else if (block.contains("/")) {
            // If block includes a branch, need to check against repo and path
            String wholePath = repo.toPath();
            // Not equal check since need to validate when block=repo/branch and repoAndPath=repo/branch/child
            String regex = "^" + block + "/.*";
            if (wholePath.matches(regex)) {
                logger.trace(String.format("Found folder match (whole path); repo: %s", repo));
                return true;
            }
        }
        logger.trace(String.format("No folder match; repo: %s", repo));
        return false;
    };

    public ScanAsAServiceCancelDecider(ScanAsAServiceModuleConfig moduleConfig,
            ScanAsAServicePropertyService propertyService,
            PluginRepoPathFactory repoPathFactory,
            ArtifactoryPAPIService artifactoryPAPIService) {
        this.moduleConfig = moduleConfig;
        this.propertyService = propertyService;
        this.repoPathFactory = repoPathFactory;
        this.artifactoryPAPIService = artifactoryPAPIService;
    }

    @Override
    public CancelDecision getCancelDecision(RepoPath repoPath) {
        AtomicBoolean isManagedDockerRepo = new AtomicBoolean(false);

        ItemInfo itemInfo = artifactoryPAPIService.getItemInfo(repoPath);
        logger.debug(displayItemInfo(itemInfo));

        if (itemInfo.isFolder()) {
            // Check to see if it is a Docker Repo being managed
            moduleConfig.getBlockingDockerRepos().ifPresent(dockerRepos -> {
                isManagedDockerRepo.set(dockerRepos.stream().anyMatch(block -> pathInManagedRepo.apply(block, repoPath)));
            });

            if (!isManagedDockerRepo.get()) {
                // This is a regular folder and not a Docker Repo being managed
                return CancelDecision.NO_CANCELLATION();
            }
        }

        // We are dealing with a file; check if that file is under a managed Docker Repo
        // If under a managed Docker Repo this could be part of a 'docker pull' or an Artifactory download
        moduleConfig.getBlockingDockerRepos().ifPresentOrElse(dockerRepos -> {
            isManagedDockerRepo.set(dockerRepos.stream().anyMatch(block -> pathInManagedRepo.apply(block, repoPath)));
        }, () -> isManagedDockerRepo.set(false));

        // item is not part of a managed Docker Repo; check to see if it is part of a regular managed repo
        if (!isManagedDockerRepo.get() && !moduleConfig.getBlockingRepos().stream()
                .anyMatch(block -> pathInManagedRepo.apply(block, repoPath)))
        {
            return CancelDecision.NO_CANCELLATION();
        }

        if (!isManagedDockerRepo.get()) {
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
        }

        File file = (isManagedDockerRepo.get() ? null : new File(itemInfo.getName()));
        Optional<List<String>> allowedNamePatterns = moduleConfig.getAllowedFileNamePatterns();
        Optional<List<String>> excludedNamePatterns = moduleConfig.getExcludedFileNamePatterns();

        // If this is Docker Repo, set the repoPath so annotations are read from the directory, not the file
        RepoPath repoPathToImageTag = null;
        if (isManagedDockerRepo.get()) {
            // Get RepoPath for the directory used as the docker image tag
            if (!itemInfo.isFolder()) {
                int endIdx = itemInfo.getRelPath().lastIndexOf("/");
                repoPathToImageTag = repoPathFactory.create(itemInfo.getRepoKey(),
                        endIdx == -1 ? itemInfo.getRelPath() : itemInfo.getRelPath().substring(0, endIdx));
            } else {
                repoPathToImageTag = repoPath;
            }
        }
        final RepoPath repoPathToQuery = isManagedDockerRepo.get() ? repoPathToImageTag : repoPath;

        if (isManagedDockerRepo.get() || applyBlockingStrategyBasedOnFilePattern(allowedNamePatterns.orElse(null), excludedNamePatterns.orElse(null), file)) {
            ScanAsAServiceBlockingStrategy blockingStrategy = moduleConfig.getBlockingStrategy();
            Optional<ProjectVersionComponentPolicyStatusType> policyViolationStatus = propertyService.getPolicyStatus(repoPathToQuery);
            return propertyService.getScanStatusProperty(repoPathToQuery).map(
                            scanStatus -> {
                                CancelDecision dec;
                                switch (scanStatus) {
                                case FAILED:
                                    switch (blockingStrategy) {
                                    case BLOCK_ALL:
                                        dec = CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; repo: %s", scanStatus.getMessage(), repoPathToQuery));
                                        break;
                                    case BLOCK_NONE:
                                        logger.info("Download NOT blocked; Blocking Strategy: {}; {}; repo: {}", blockingStrategy, scanStatus.getMessage(), repoPathToQuery);
                                        dec = CancelDecision.NO_CANCELLATION();
                                        break;
                                    case BLOCK_OFF:
                                        logger.info("Download NOT blocked; Blocking Strategy: {}; Download would have been blocked; {}; repo: {}",
                                                ScanAsAServiceBlockingStrategy.BLOCK_OFF, scanStatus.getMessage(), repoPathToQuery);
                                        dec = CancelDecision.NO_CANCELLATION();
                                        break;
                                    default:
                                        dec = CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; Unknown blocking strategy: %s; repo: %s", blockingStrategy, repoPathToQuery));
                                    }
                                    break;
                                case PROCESSING:
                                    switch (blockingStrategy) {
                                    case BLOCK_ALL:
                                        dec = CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; repo: %s", scanStatus.getMessage(), repoPathToQuery));
                                        break;
                                    case BLOCK_NONE:
                                        dec = CancelDecision.NO_CANCELLATION();
                                        break;
                                    case BLOCK_OFF:
                                        logger.info("Download NOT Blocked; Blocking Strategy: {}; {}; repo: {}", ScanAsAServiceBlockingStrategy.BLOCK_OFF,
                                                scanStatus.getMessage(), repoPathToQuery);
                                        dec = CancelDecision.NO_CANCELLATION();
                                        break;
                                    default:
                                        dec = CancelDecision.CANCEL_DOWNLOAD(
                                                String.format("Download blocked; Unknown blocking strategy: %s; repo: %s", blockingStrategy, repoPathToQuery));
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
                                                        scanStatus.getMessage(), repoPathToQuery);
                                                dec = CancelDecision.NO_CANCELLATION();
                                            } else {
                                                dec = CancelDecision.CANCEL_DOWNLOAD(
                                                        String.format("Download blocked; %s; Policy Violation Status: %s; repo: %s", scanStatus.getMessage(),
                                                                policyViolationStatus.get(), repoPathToQuery));
                                            }
                                            break;
                                        default:
                                            if (ScanAsAServiceBlockingStrategy.BLOCK_OFF == blockingStrategy) {
                                                logger.info("Download NOT blocked; Blocking Strategy: {}; {}; Unknown policy violation status: {}; repo: {}",
                                                        ScanAsAServiceBlockingStrategy.BLOCK_OFF, scanStatus.getMessage(), policyViolationStatus.get(), repoPathToQuery);
                                                dec = CancelDecision.NO_CANCELLATION();
                                            } else {
                                                dec = CancelDecision.CANCEL_DOWNLOAD(
                                                        String.format("Download blocked; %s; Unknown policy violation status: %s; repo: %s",
                                                                scanStatus.getMessage(), policyViolationStatus.get(), repoPathToQuery));
                                            }
                                        }
                                    } else {
                                        logger.warn(String.format("%s; %s property not present; Using block strategy: %s; repo: %s", scanStatus.getMessage(),
                                                BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS.getPropertyName(), blockingStrategy, repoPathToQuery));
                                        switch (blockingStrategy) {
                                        case BLOCK_ALL:
                                            dec = CancelDecision.CANCEL_DOWNLOAD(
                                                    String.format("Download blocked; %s; %s not present; block strategy: %s; repo: %s", scanStatus.getMessage(),
                                                            BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS, blockingStrategy, repoPathToQuery));
                                            break;
                                        case BLOCK_NONE:
                                        case BLOCK_OFF:
                                            dec = CancelDecision.NO_CANCELLATION();
                                            break;
                                        default:
                                            dec = CancelDecision.CANCEL_DOWNLOAD(
                                                    String.format("Download blocked; %s not present; Unknown blocking strategy: %s; repo: %s",
                                                            BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS, blockingStrategy, repoPathToQuery));
                                        }
                                    }
                                    break;
                                default:
                                    logger.warn(String.format("Unknown scan status detected: %s", scanStatus));
                                    if (ScanAsAServiceBlockingStrategy.BLOCK_OFF == blockingStrategy) {
                                        logger.info("Download NOT blocked; Blocking Strategy: {}; Unknown ScanStatusProperty: {}; repo: {}",
                                                ScanAsAServiceBlockingStrategy.BLOCK_OFF, scanStatus, repoPathToQuery);
                                        dec = CancelDecision.NO_CANCELLATION();
                                    } else {
                                        dec = CancelDecision.CANCEL_DOWNLOAD(
                                                String.format("Download blocked; Unknown ScanStatusProperty: %s; repo: %s", scanStatus, repoPathToQuery));
                                    }
                                }
                                return dec;
                            })
                    .orElseGet(() -> {
                        switch (blockingStrategy) {
                        case BLOCK_ALL:
                            return CancelDecision.CANCEL_DOWNLOAD(
                                    String.format("Download blocked; Scan not scheduled and blocking strategy: %s; repo: %s", blockingStrategy, repoPathToQuery));
                        case BLOCK_NONE:
                            return CancelDecision.NO_CANCELLATION();
                        case BLOCK_OFF:
                            logger.info("Download NOT blocked; Blocking Strategy: {}; Scan not scheduled; repo: {}", ScanAsAServiceBlockingStrategy.BLOCK_OFF,
                                    repoPathToQuery);
                            return CancelDecision.NO_CANCELLATION();
                        default:
                            return CancelDecision.CANCEL_DOWNLOAD(
                                    String.format("Download blocked; Unknown blocking strategy: %s; repo: %s", blockingStrategy, repoPathToQuery));
                        }
                    });
        } else {
            logger.info("Download NOT blocked; File allowed/excluded pattern check failed; repo: {}", repoPathToQuery);
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

        if (file == null) {
            logger.debug("File: NULL; Cannot determine if file patterns apply");
            return false;
        }

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

    private static String displayItemInfo(ItemInfo item) {
        return MoreObjects.toStringHelper(ItemInfo.class)
                .omitNullValues()
                .add("repoPath", item.getRepoPath())
                .add("isFolder", item.isFolder())
                .add("name", item.getName())
                .add("repoKey", item.getRepoKey())
                .add("relPath", item.getRelPath())
                .add("created", item.getCreated())
                .add("lastModified", item.getLastModified())
                .add("modifiedBy", item.getModifiedBy())
                .add("createdBy", item.getCreatedBy())
                .add("lastUpdate", item.getLastUpdated())
                .toString();
    }
}
