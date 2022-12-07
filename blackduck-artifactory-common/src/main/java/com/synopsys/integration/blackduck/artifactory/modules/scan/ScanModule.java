/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scan;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.LogUtil;
import com.synopsys.integration.blackduck.artifactory.TriggerType;
import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.UpdateStatus;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.SimpleAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.cancel.CancelDecider;
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
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final SimpleAnalyticsCollector simpleAnalyticsCollector;
    private final ScanPolicyService scanPolicyService;
    private final PostScanActionsService postScanActionsService;
    private final ScanPropertyService scanPropertyService;
    private final ScannerDirectoryUtil scannerDirectoryUtil;
    private final Collection<CancelDecider> cancelDeciders;

    public ScanModule(
        ScanModuleConfig scanModuleConfig,
        RepositoryIdentificationService repositoryIdentificationService,
        ArtifactScanService artifactScanService,
        ArtifactoryPAPIService artifactoryPAPIService,
        SimpleAnalyticsCollector simpleAnalyticsCollector,
        ScanPolicyService scanPolicyService,
        PostScanActionsService postScanActionsService,
        ScanPropertyService scanPropertyService,
        ScannerDirectoryUtil scannerDirectoryUtil,
        Collection<CancelDecider> cancelDeciders
    ) {
        this.scanModuleConfig = scanModuleConfig;
        this.repositoryIdentificationService = repositoryIdentificationService;
        this.artifactScanService = artifactScanService;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.simpleAnalyticsCollector = simpleAnalyticsCollector;
        this.scanPolicyService = scanPolicyService;
        this.postScanActionsService = postScanActionsService;
        this.scanPropertyService = scanPropertyService;
        this.scannerDirectoryUtil = scannerDirectoryUtil;
        this.cancelDeciders = cancelDeciders;
    }

    @Override
    public ScanModuleConfig getModuleConfig() {
        return scanModuleConfig;
    }

    public void reloadScannerDirectory(TriggerType triggerType) {
        LogUtil.start(logger, "blackDuckReloadScannerDirectory", triggerType);

        File scannerDirectory = scannerDirectoryUtil.getRootScannerDirectory();
        String scannerDirectoryPath = scannerDirectory.getAbsolutePath();
        try {
            logger.info(String.format("Deleting scanner directory: %s", scannerDirectoryPath));
            FileUtils.deleteDirectory(scannerDirectory);
            logger.info("Creating new scanner directory.");
            scannerDirectoryUtil.createDirectories();
            logger.info(String.format("New scanner directory created: %s", scannerDirectoryPath));
        } catch (IntegrationException | IOException e) {
            logger.error("Failed to properly reload the scanner directory: " + scannerDirectoryPath);
        }

        LogUtil.finish(logger, "blackDuckReloadScannerDirectory", triggerType);
    }

    public void triggerScan() {
        Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths();
        artifactScanService.scanArtifactPaths(repoPaths);
        updateAnalyticsData();
    }

    public void addPolicyStatus() {
        Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths().stream()
                                      .filter(repoPath -> scanPropertyService.getScanResult(repoPath)
                                                              .filter(Result.SUCCESS::equals)
                                                              .isPresent())
                                      .collect(Collectors.toSet());
        scanPolicyService.populatePolicyStatuses(repoPaths);
        updateAnalyticsData();
    }

    public void performPostScanActions() {
        postScanActionsService.performPostScanActions(scanModuleConfig.getRepos());
        updateAnalyticsData();
    }

    public void deleteScanProperties(Map<String, List<String>> params) {
        for (String repoKey : scanModuleConfig.getRepos()) {
            scanPropertyService.deleteAllBlackDuckPropertiesFromRepo(repoKey, params, logger);
        }
        updateAnalyticsData();
    }

    public void deleteScanPropertiesFromFailures(Map<String, List<String>> params) {
        List<RepoPath> repoPathsWithFailures = scanModuleConfig.getRepos().stream()
                                                   .map(repoKey -> scanPropertyService.getItemsContainingProperties(repoKey, BlackDuckArtifactoryProperty.SCAN_RESULT))
                                                   .flatMap(List::stream)
                                                   .filter(repoPath -> scanPropertyService.getScanResult(repoPath)
                                                                           .filter(Result.FAILURE::equals)
                                                                           .isPresent())
                                                   .collect(Collectors.toList());

        repoPathsWithFailures.forEach(repoPath -> scanPropertyService.deleteAllBlackDuckPropertiesFromRepoPath(repoPath, params, logger));
        updateAnalyticsData();
    }

    public void deleteScanPropertiesFromOutOfDate(Map<String, List<String>> params) {
        List<RepoPath> repoPathsOutOfDate = scanModuleConfig.getRepos().stream()
                                                .map(repoKey -> scanPropertyService.getItemsContainingProperties(repoKey, BlackDuckArtifactoryProperty.UPDATE_STATUS))
                                                .flatMap(List::stream)
                                                .filter(repoPath -> scanPropertyService.getUpdateStatus(repoPath)
                                                                        .filter(UpdateStatus.OUT_OF_DATE::equals)
                                                                        .isPresent())
                                                .collect(Collectors.toList());

        repoPathsOutOfDate.forEach(repoPath -> scanPropertyService.deleteAllBlackDuckPropertiesFromRepoPath(repoPath, params, logger));
        updateAnalyticsData();
    }

    public void handleBeforeDownloadEvent(RepoPath repoPath) {
        cancelDeciders.forEach(cancelDecider -> cancelDecider.handleBeforeDownloadEvent(repoPath));
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return Collections.singletonList(simpleAnalyticsCollector);
    }

    private void updateAnalyticsData() {
        List<String> scanRepositoryKeys = scanModuleConfig.getRepos();
        simpleAnalyticsCollector.putMetadata("scan.repo.count", scanRepositoryKeys.size());
        simpleAnalyticsCollector.putMetadata("scan.artifact.count", artifactoryPAPIService.getArtifactCount(scanRepositoryKeys));
    }
}
