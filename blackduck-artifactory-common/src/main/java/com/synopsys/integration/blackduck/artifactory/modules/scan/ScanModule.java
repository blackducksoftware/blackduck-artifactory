/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2019 Synopsys, Inc.
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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.UpdateStatus;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.SimpleAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.scan.service.ArtifactScanService;
import com.synopsys.integration.blackduck.artifactory.modules.scan.service.PostScanActionsService;
import com.synopsys.integration.blackduck.artifactory.modules.scan.service.RepositoryIdentificationService;
import com.synopsys.integration.blackduck.artifactory.modules.scan.service.ScanPolicyService;
import com.synopsys.integration.blackduck.codelocation.Result;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ScanModule implements Module {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ScanModuleConfig scanModuleConfig;

    private final RepositoryIdentificationService repositoryIdentificationService;
    private final ArtifactScanService artifactScanService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final SimpleAnalyticsCollector simpleAnalyticsCollector;
    private final ScanPolicyService scanPolicyService;
    private final PostScanActionsService postScanActionsService;

    public ScanModule(final ScanModuleConfig scanModuleConfig, final RepositoryIdentificationService repositoryIdentificationService, final ArtifactScanService artifactScanService,
        final ArtifactoryPropertyService artifactoryPropertyService, final ArtifactoryPAPIService artifactoryPAPIService, final SimpleAnalyticsCollector simpleAnalyticsCollector, final ScanPolicyService scanPolicyService,
        final PostScanActionsService postScanActionsService) {
        this.scanModuleConfig = scanModuleConfig;
        this.repositoryIdentificationService = repositoryIdentificationService;
        this.artifactScanService = artifactScanService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.simpleAnalyticsCollector = simpleAnalyticsCollector;
        this.scanPolicyService = scanPolicyService;
        this.postScanActionsService = postScanActionsService;
    }

    public static File setUpCliDuckDirectory(final File blackDuckDirectory) throws IOException, IntegrationException {
        final File cliDirectory = new File(blackDuckDirectory, "cli");
        if (!cliDirectory.exists() && !cliDirectory.mkdir()) {
            throw new IntegrationException(String.format("Failed to create cliDirectory: %s", cliDirectory.getCanonicalPath()));
        }

        return cliDirectory;
    }

    @Override
    public ScanModuleConfig getModuleConfig() {
        return scanModuleConfig;
    }

    public void triggerScan() {
        final Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths();
        artifactScanService.scanArtifactPaths(repoPaths);
        updateAnalyticsData();
    }

    public void addPolicyStatus() {
        final Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths().stream()
                                            .filter(repoPath -> {
                                                final Optional<String> property = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_RESULT);
                                                return property.isPresent() && property.get().equals(Result.SUCCESS.name());
                                            })
                                            .collect(Collectors.toSet());
        scanPolicyService.populatePolicyStatuses(repoPaths);
        updateAnalyticsData();
    }

    public void performPostScanActions() {
        postScanActionsService.performPostScanActions(scanModuleConfig.getRepos());
        updateAnalyticsData();
    }

    public void deleteScanProperties(final Map<String, List<String>> params) {
        scanModuleConfig.getRepos()
            .forEach(repoKey -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepo(repoKey, params, logger));
        updateAnalyticsData();
    }

    public void deleteScanPropertiesFromFailures(final Map<String, List<String>> params) {
        final List<RepoPath> repoPathsWithFailures = scanModuleConfig.getRepos().stream()
                                                         .map(repoKey -> artifactoryPropertyService.getItemsContainingProperties(repoKey, BlackDuckArtifactoryProperty.SCAN_RESULT))
                                                         .flatMap(List::stream)
                                                         .filter(repoPath -> artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_RESULT).equals(Optional.of(Result.FAILURE.toString())))
                                                         .collect(Collectors.toList());

        repoPathsWithFailures.forEach(repoPath -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepoPath(repoPath, params, logger));
        updateAnalyticsData();
    }

    public void deleteScanPropertiesFromOutOfDate(final Map<String, List<String>> params) {
        final List<RepoPath> repoPathsOutOfDate = scanModuleConfig.getRepos().stream()
                                                      .map(repoKey -> artifactoryPropertyService.getItemsContainingProperties(repoKey, BlackDuckArtifactoryProperty.UPDATE_STATUS))
                                                      .flatMap(List::stream)
                                                      .filter(repoPath -> artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS).equals(Optional.of(UpdateStatus.OUT_OF_DATE.toString())))
                                                      .collect(Collectors.toList());

        repoPathsOutOfDate.forEach(repoPath -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepoPath(repoPath, params, logger));
        updateAnalyticsData();
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return Collections.singletonList(simpleAnalyticsCollector);
    }

    private void updateAnalyticsData() {
        final List<String> scanRepositoryKeys = scanModuleConfig.getRepos();
        simpleAnalyticsCollector.putMetadata("scan.repo.count", scanRepositoryKeys.size());
        simpleAnalyticsCollector.putMetadata("scan.artifact.count", artifactoryPAPIService.getArtifactCount(scanRepositoryKeys));
    }
}
