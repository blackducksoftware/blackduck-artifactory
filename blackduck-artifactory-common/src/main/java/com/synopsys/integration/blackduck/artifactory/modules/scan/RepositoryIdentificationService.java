/**
 * hub-artifactory-common
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.repo.Repositories;
import org.artifactory.repo.RepositoryConfiguration;
import org.artifactory.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.BlackDuckPropertyManager;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;

public class RepositoryIdentificationService {
    private final Logger logger = LoggerFactory.getLogger(RepositoryIdentificationService.class);

    private final BlackDuckPropertyManager blackDuckPropertyManager;
    private final DateTimeManager dateTimeManager;
    private final Repositories repositories;
    private final Searches searches;

    private final List<String> repoKeysToScan = new ArrayList<>();

    public RepositoryIdentificationService(final BlackDuckPropertyManager blackDuckPropertyManager, final DateTimeManager dateTimeManager, final Repositories repositories, final Searches searches) {
        this.blackDuckPropertyManager = blackDuckPropertyManager;
        this.searches = searches;
        this.dateTimeManager = dateTimeManager;
        this.repositories = repositories;

        loadRepositoriesToScan();
    }

    private void loadRepositoriesToScan() {
        try {
            repoKeysToScan.addAll(blackDuckPropertyManager.getRepositoryKeysFromProperties(ScanModuleProperty.REPOS, ScanModuleProperty.REPOS_CSV_PATH));
        } catch (final IOException e) {
            logger.error(String.format("Exception while attempting to extract repositories from '%s'", blackDuckPropertyManager.getProperty(ScanModuleProperty.REPOS_CSV_PATH)));
        }

        final List<String> invalidRepoKeys = new ArrayList<>();
        for (final String repoKey : repoKeysToScan) {
            final RepoPath repoPath = RepoPathFactory.create(repoKey);
            final RepositoryConfiguration repositoryConfiguration = repositories.getRepositoryConfiguration(repoKey);
            if (!repositories.exists(repoPath) || repositoryConfiguration == null) {
                invalidRepoKeys.add(repoKey);
                logger.warn(String.format("The Black Duck %s will not scan artifacts in configured repository '%s': Repository was not found or is not a valid repository.", ScanModule.class.getSimpleName(), repoKey));
            }
        }

        repoKeysToScan.removeAll(invalidRepoKeys);
    }

    public Set<RepoPath> searchForRepoPaths() {
        final List<String> patternsToScan = Arrays.asList(blackDuckPropertyManager.getProperty(ScanModuleProperty.NAME_PATTERNS).split(","));
        final List<RepoPath> repoPaths = new ArrayList<>();

        for (final String pattern : patternsToScan) {
            repoPaths.addAll(searches.artifactsByName(pattern, repoKeysToScan.toArray(new String[repoKeysToScan.size()])));
        }
        logger.debug(String.format("patternsToScan: %d", patternsToScan.size()));
        logger.debug(String.format("repoKeysToScan: %d", repoKeysToScan.size()));
        logger.debug(String.format("repoPaths: %d", repoPaths.size()));
        return new HashSet<>(repoPaths);
    }

    /**
     * If artifact's last modified time is newer than the scan time, or we have no record of the scan time, we should scan now, unless, if the cutoff date is set, only scan if the modified date is greater than or equal to the cutoff.
     */
    boolean shouldRepoPathBeScannedNow(final RepoPath repoPath) {
        final ItemInfo itemInfo = repositories.getItemInfo(repoPath);
        final long lastModifiedTime = itemInfo.getLastModified();
        final String artifactCutoffDate = blackDuckPropertyManager.getProperty(ScanModuleProperty.CUTOFF_DATE);

        boolean shouldCutoffPreventScanning = false;
        if (StringUtils.isNotBlank(artifactCutoffDate)) {
            try {
                final long cutoffTime = dateTimeManager.getTimeFromString(artifactCutoffDate);
                shouldCutoffPreventScanning = lastModifiedTime < cutoffTime;
            } catch (final Exception e) {
                logger.error(String.format("The pattern: %s does not match the date string: %s", dateTimeManager.getDateTimePattern(), artifactCutoffDate), e);
                shouldCutoffPreventScanning = false;
            }
        }

        if (shouldCutoffPreventScanning) {
            logger.warn(String.format("%s was not scanned because the cutoff was set and the artifact is too old", itemInfo.getName()));
            return false;
        }

        final String blackDuckScanTimeProperty = repositories.getProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_TIME.getName());
        if (StringUtils.isBlank(blackDuckScanTimeProperty)) {
            return true;
        }

        try {
            final long blackDuckScanTime = dateTimeManager.getTimeFromString(blackDuckScanTimeProperty);
            return lastModifiedTime >= blackDuckScanTime;
        } catch (final Exception e) {
            //if the date format changes, the old format won't parse, so just cleanup the property by returning true and re-scanning
            logger.error("Exception parsing the scan date (most likely the format changed)", e);
        }

        return true;
    }

    public List<String> getRepoKeysToScan() {
        return repoKeysToScan;
    }

    public Long getArtifactCount(final List<String> repoKeys) {
        return repoKeys.stream()
                   .map(RepoPathFactory::create)
                   .map(repositories::getArtifactsCount)
                   .mapToLong(Long::longValue)
                   .sum();
    }
}
