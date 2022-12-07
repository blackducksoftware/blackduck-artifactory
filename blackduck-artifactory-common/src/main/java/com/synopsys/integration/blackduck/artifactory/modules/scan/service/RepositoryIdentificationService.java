/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scan.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModule;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModuleConfig;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class RepositoryIdentificationService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(RepositoryIdentificationService.class));

    private final ScanModuleConfig scanModuleConfig;
    private final DateTimeManager dateTimeManager;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final ArtifactoryPAPIService artifactoryPAPIService;

    public RepositoryIdentificationService(ScanModuleConfig scanModuleConfig, DateTimeManager dateTimeManager, ArtifactoryPropertyService artifactoryPropertyService, ArtifactoryPAPIService artifactoryPAPIService) {
        this.scanModuleConfig = scanModuleConfig;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.dateTimeManager = dateTimeManager;
    }

    public Set<RepoPath> searchForRepoPaths() {
        List<String> patternsToScan = scanModuleConfig.getNamePatterns();
        List<String> repoKeysToScan = scanModuleConfig.getRepos();
        List<RepoPath> repoPaths = new ArrayList<>();

        if (!repoKeysToScan.isEmpty()) {
            repoKeysToScan.stream()
                .map(repoKey -> artifactoryPAPIService.searchForArtifactsByPatterns(repoKey, patternsToScan))
                .forEach(repoPaths::addAll);
        } else {
            logger.info(String.format("Please specify valid repos to scan or disable the %s", ScanModule.class.getSimpleName()));
        }

        logger.debug(String.format("patternsToScan: %d", patternsToScan.size()));
        logger.debug(String.format("repoKeysToScan: %d", repoKeysToScan.size()));
        logger.debug(String.format("repoPaths: %d", repoPaths.size()));
        return new HashSet<>(repoPaths);
    }

    /**
     * If artifact's last modified time is newer than the scan time, or we have no record of the scan time, we should scan now, unless, if the cutoff date is set, only scan if the modified date is greater than or equal to the cutoff.
     */
    boolean shouldRepoPathBeScannedNow(RepoPath repoPath) {
        ItemInfo itemInfo = artifactoryPAPIService.getItemInfo(repoPath);
        long lastModifiedTime = itemInfo.getLastModified();
        String artifactCutoffDate = scanModuleConfig.getArtifactCutoffDate();

        boolean shouldCutoffPreventScanning = false;
        if (StringUtils.isNotBlank(artifactCutoffDate)) {
            try {
                long cutoffTime = dateTimeManager.getTimeFromString(artifactCutoffDate);
                shouldCutoffPreventScanning = lastModifiedTime < cutoffTime;
            } catch (Exception e) {
                logger.error(String.format("The pattern: %s does not match the date string: %s", dateTimeManager.getDateTimePattern(), artifactCutoffDate), e);
                shouldCutoffPreventScanning = false;
            }
        }

        if (shouldCutoffPreventScanning) {
            logger.warn(String.format("%s was not scanned because the cutoff was set and the artifact is too old", itemInfo.getName()));
            return false;
        }

        Optional<String> blackDuckScanTimeProperty = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_TIME);
        if (!blackDuckScanTimeProperty.isPresent()) {
            return true;
        }

        try {
            long blackDuckScanTime = dateTimeManager.getTimeFromString(blackDuckScanTimeProperty.get());
            return lastModifiedTime >= blackDuckScanTime;
        } catch (Exception e) {
            //if the date format changes, the old format won't parse, so just cleanup the property by returning true and re-scanning
            logger.error("Exception parsing the scan date (most likely the format changed)", e);
        }

        return true;
    }
}
