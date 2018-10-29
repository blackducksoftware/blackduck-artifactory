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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.Analyzable;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.SimpleAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.UpdateStatus;
import com.synopsys.integration.blackduck.summary.Result;
import com.synopsys.integration.exception.IntegrationException;

public class ScanModule implements Analyzable, Module {
    private final ScanModuleConfig scanModuleConfig;

    private final RepositoryIdentificationService repositoryIdentificationService;
    private final ArtifactScanService artifactScanService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final StatusCheckService statusCheckService;
    private final SimpleAnalyticsCollector simpleAnalyticsCollector;
    private final ScanPolicyService scanPolicyService;

    public ScanModule(final ScanModuleConfig scanModuleConfig, final RepositoryIdentificationService repositoryIdentificationService, final ArtifactScanService artifactScanService,
        final ArtifactoryPropertyService artifactoryPropertyService, final StatusCheckService statusCheckService, final SimpleAnalyticsCollector simpleAnalyticsCollector, final ScanPolicyService scanPolicyService) {
        this.scanModuleConfig = scanModuleConfig;
        this.repositoryIdentificationService = repositoryIdentificationService;
        this.artifactScanService = artifactScanService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.statusCheckService = statusCheckService;
        this.simpleAnalyticsCollector = simpleAnalyticsCollector;
        this.scanPolicyService = scanPolicyService;
    }

    public static File setUpCliDuckDirectory(final File blackDuckDirectory) throws IOException, IntegrationException {
        final File cliDirectory = new File(blackDuckDirectory, "cli");
        if (!cliDirectory.exists() && !cliDirectory.mkdir()) {
            throw new IntegrationException(String.format("Failed to create cliDirectory: %s", cliDirectory.getCanonicalPath()));
        }

        return cliDirectory;
    }

    public ScanModuleConfig getModuleConfig() {
        return scanModuleConfig;
    }

    public void triggerScan() {
        final Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths();
        artifactScanService.scanArtifactPaths(repoPaths);
        updateAnalyticsData();
    }

    public void addPolicyStatus() {
        final Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths();
        scanPolicyService.populatePolicyStatuses(repoPaths);
        updateAnalyticsData();
    }

    public void deleteScanProperties(final Map<String, List<String>> params) {
        repositoryIdentificationService.getRepoKeysToScan()
            .forEach(repoKey -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepo(repoKey, params));
        updateAnalyticsData();
    }

    public void deleteScanPropertiesFromFailures(final Map<String, List<String>> params) {
        final List<RepoPath> repoPathsWithFailures = repositoryIdentificationService.getRepoKeysToScan().stream()
                                                         .map(repoKey -> artifactoryPropertyService.getAllItemsInRepoWithProperties(repoKey, BlackDuckArtifactoryProperty.SCAN_RESULT))
                                                         .flatMap(List::stream)
                                                         .filter(repoPath -> artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_RESULT).equals(Optional.of(Result.FAILURE.toString())))
                                                         .collect(Collectors.toList());

        repoPathsWithFailures.forEach(repoPath -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepoPath(repoPath, params));
        updateAnalyticsData();
    }

    public void deleteScanPropertiesFromOutOfDate(final Map<String, List<String>> params) {
        final List<RepoPath> repoPathsOutOfDate = repositoryIdentificationService.getRepoKeysToScan().stream()
                                                      .map(repoKey -> artifactoryPropertyService.getAllItemsInRepoWithProperties(repoKey, BlackDuckArtifactoryProperty.UPDATE_STATUS))
                                                      .flatMap(List::stream)
                                                      .filter(repoPath -> artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS).equals(Optional.of(UpdateStatus.OUT_OF_DATE.toString())))
                                                      .collect(Collectors.toList());

        repoPathsOutOfDate.forEach(repoPath -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepoPath(repoPath, params));
        updateAnalyticsData();
    }

    public void updateDeprecatedProperties() {
        repositoryIdentificationService.getRepoKeysToScan()
            .forEach(artifactoryPropertyService::updateAllBlackDuckPropertiesFromRepoKey);
        updateAnalyticsData();
    }

    public String getStatusCheckMessage() {
        return statusCheckService.getStatusMessage();
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return Arrays.asList(simpleAnalyticsCollector);
    }

    private void updateAnalyticsData() {
        final List<String> scanRepositoryKeys = repositoryIdentificationService.getRepoKeysToScan();
        simpleAnalyticsCollector.putMetadata("scan.repo.count", scanRepositoryKeys.size());
        simpleAnalyticsCollector.putMetadata("scan.artifact.count", repositoryIdentificationService.getArtifactCount(scanRepositoryKeys));
    }
}
