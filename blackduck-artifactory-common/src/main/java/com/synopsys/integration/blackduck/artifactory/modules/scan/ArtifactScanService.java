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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.artifactory.resource.ResourceStreamHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.BlackDuckPropertyManager;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.service.model.ProjectNameVersionGuess;
import com.synopsys.integration.blackduck.service.model.ProjectNameVersionGuesser;
import com.synopsys.integration.blackduck.signaturescanner.ScanJob;
import com.synopsys.integration.blackduck.signaturescanner.ScanJobBuilder;
import com.synopsys.integration.blackduck.signaturescanner.ScanJobManager;
import com.synopsys.integration.blackduck.signaturescanner.ScanJobOutput;
import com.synopsys.integration.blackduck.signaturescanner.command.ScanCommandOutput;
import com.synopsys.integration.blackduck.signaturescanner.command.ScanTarget;
import com.synopsys.integration.blackduck.summary.Result;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.HostNameHelper;
import com.synopsys.integration.util.NameVersion;
import com.synopsys.integration.util.ResourceUtil;

public class ArtifactScanService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ScanModuleConfig scanModuleConfig;
    private final HubServerConfig hubServerConfig;
    private final File blackDuckDirectory;
    private final BlackDuckPropertyManager blackDuckPropertyManager;
    private final RepositoryIdentificationService repositoryIdentificationService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final Repositories repositories;
    private final DateTimeManager dateTimeManager;

    public ArtifactScanService(final ScanModuleConfig scanModuleConfig, final HubServerConfig hubServerConfig, final File blackDuckDirectory, final BlackDuckPropertyManager blackDuckPropertyManager,
        final RepositoryIdentificationService repositoryIdentificationService, final ArtifactoryPropertyService artifactoryPropertyService, final Repositories repositories, final DateTimeManager dateTimeManager) {
        this.scanModuleConfig = scanModuleConfig;
        this.hubServerConfig = hubServerConfig;
        this.blackDuckDirectory = blackDuckDirectory;
        this.blackDuckPropertyManager = blackDuckPropertyManager;
        this.repositoryIdentificationService = repositoryIdentificationService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.repositories = repositories;
        this.dateTimeManager = dateTimeManager;
    }

    public void scanArtifactPaths(final Set<RepoPath> repoPaths) {
        logger.info(String.format("Found %d repoPaths to scan", repoPaths.size()));
        final List<RepoPath> shouldScanRepoPaths = new ArrayList<>();
        for (final RepoPath repoPath : repoPaths) {
            logger.debug(String.format("Verifying if repoPath should be scanned: %s", repoPath.toPath()));
            if (repositoryIdentificationService.shouldRepoPathBeScannedNow(repoPath)) {
                logger.info(String.format("Adding repoPath to scan list: %s", repoPath.toPath()));
                shouldScanRepoPaths.add(repoPath);
            }
        }

        for (final RepoPath repoPath : shouldScanRepoPaths) {
            try {
                final String timeString = dateTimeManager.getStringFromDate(new Date());
                artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_TIME, timeString);
                final NameVersion projectNameVersion = determineProjectNameVersion(repoPath);
                final ScanJobOutput scanJobOutput = scanArtifact(repoPath, projectNameVersion.getName(), projectNameVersion.getVersion());
                writeScanProperties(repoPath, projectNameVersion.getName(), projectNameVersion.getVersion(), scanJobOutput);
            } catch (final Exception e) {
                logger.error(String.format("Please investigate the scan logs for details - the Black Duck Scan did not complete successfully on %s", repoPath.getName()), e);
                artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_RESULT, Result.FAILURE.toString());
            } finally {
                deletePathArtifact(repoPath.getName());
            }
        }
    }

    private FileLayoutInfo getArtifactFromPath(final RepoPath repoPath) {
        final FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath);

        try (final ResourceStreamHandle resourceStream = repositories.getContent(repoPath)) {
            InputStream inputStream = null;
            FileOutputStream fileOutputStream = null;
            try {
                inputStream = resourceStream.getInputStream();
                fileOutputStream = new FileOutputStream(new File(blackDuckDirectory, repoPath.getName()));
                IOUtils.copy(inputStream, fileOutputStream);
            } catch (final IOException e) {
                logger.error(String.format("There was an error getting %s", repoPath.getName()), e);
            } finally {
                ResourceUtil.closeQuietly(inputStream);
                ResourceUtil.closeQuietly(fileOutputStream);
            }
        }

        return fileLayoutInfo;
    }

    private ScanJobOutput scanArtifact(final RepoPath repoPath, final String projectName, final String projectVersionName) throws IntegrationException, IOException {
        final int scanMemory = Integer.parseInt(blackDuckPropertyManager.getProperty(ScanModuleProperty.MEMORY));
        final boolean dryRun = Boolean.parseBoolean(blackDuckPropertyManager.getProperty(ScanModuleProperty.DRY_RUN));
        final boolean useRepoPathAsCodeLocationName = Boolean.parseBoolean(blackDuckPropertyManager.getProperty(ScanModuleProperty.REPO_PATH_CODELOCATION));
        final ScanJobManager scanJobManager = ScanJobManager.createDefaultScanManager(new Slf4jIntLogger(logger), hubServerConfig);
        final ScanJobBuilder scanJobBuilder = new ScanJobBuilder()
                                                  .fromHubServerConfig(hubServerConfig)
                                                  .scanMemoryInMegabytes(scanMemory)
                                                  .dryRun(dryRun)
                                                  .installDirectory(scanModuleConfig.getCliDirectory())
                                                  .outputDirectory(blackDuckDirectory)
                                                  .projectAndVersionNames(projectName, projectVersionName);

        final File scanFile = new File(blackDuckDirectory, repoPath.getName());
        final String scanTargetPath = scanFile.getCanonicalPath();

        String codeLocationName = null;
        if (useRepoPathAsCodeLocationName) {
            final String hostName = HostNameHelper.getMyHostName("UNKNOWN_HOST");
            codeLocationName = String.format("%s#%s", hostName, repoPath.toPath());
        }

        final ScanTarget scanTarget = ScanTarget.createBasicTarget(scanTargetPath, null, codeLocationName);
        scanJobBuilder.addTarget(scanTarget);

        final ScanJob scanJob = scanJobBuilder.build();
        logger.info(String.format("Performing scan on '%s'", scanTargetPath));

        return scanJobManager.executeScans(scanJob);
    }

    private NameVersion determineProjectNameVersion(final RepoPath repoPath) {
        final FileLayoutInfo fileLayoutInfo = getArtifactFromPath(repoPath);
        final String fileName = repoPath.getName();
        String project = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME)
                             .orElse(fileLayoutInfo.getModule());
        String version = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME)
                             .orElse(fileLayoutInfo.getBaseRevision());

        final boolean missingProjectName = StringUtils.isBlank(project);
        final boolean missingProjectVersionName = StringUtils.isBlank(version);
        if (missingProjectName || missingProjectVersionName) {
            final String filenameWithoutExtension = FilenameUtils.getBaseName(fileName);
            final ProjectNameVersionGuesser guesser = new ProjectNameVersionGuesser();
            final ProjectNameVersionGuess guess = guesser.guessNameAndVersion(filenameWithoutExtension);

            if (missingProjectName) {
                project = guess.getProjectName();
            }
            if (missingProjectVersionName) {
                version = guess.getVersionName();
            }
        }

        return new NameVersion(project, version);
    }

    private void writeScanProperties(final RepoPath repoPath, final String projectName, final String projectNameVersion, final ScanJobOutput scanJobOutput) {
        final ScanCommandOutput scanCommandOutput = getFirstScanCommandOutput(scanJobOutput);
        if (scanCommandOutput == null) {
            logger.warn("No scan summaries were available for a successful scan. This is expected if this was a dry run, but otherwise there should be summaries.");
            return;
        }

        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_RESULT, scanCommandOutput.getResult().name());

        if (scanCommandOutput.getResult().equals(Result.FAILURE)) {
            logger.warn(String.format("The BlackDuck CLI failed to scan %s", repoPath.getName()));
            return;
        }

        logger.info(String.format("%s was successfully scanned by the BlackDuck CLI.", repoPath.getName()));

        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME, projectName);
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME, projectNameVersion);
    }

    private ScanCommandOutput getFirstScanCommandOutput(final ScanJobOutput scanJobOutput) {
        final List<ScanCommandOutput> scanCommandOutputs = scanJobOutput.getScanCommandOutputs();
        final ScanCommandOutput scanCommandOutput;
        if (scanCommandOutputs != null && !scanCommandOutputs.isEmpty()) {
            scanCommandOutput = scanCommandOutputs.get(0);
        } else {
            scanCommandOutput = null;
        }

        return scanCommandOutput;
    }

    private void deletePathArtifact(final String fileName) {
        try {
            final boolean deleteOk = new File(blackDuckDirectory, fileName).delete();
            logger.info(String.format("Successfully deleted temporary %s: %s", fileName, Boolean.toString(deleteOk)));
        } catch (final Exception e) {
            logger.error(String.format("Exception deleting %s", fileName), e);
        }
    }
}
