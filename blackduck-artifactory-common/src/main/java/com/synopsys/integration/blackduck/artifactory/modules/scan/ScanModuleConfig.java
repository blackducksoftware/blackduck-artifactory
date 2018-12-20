/**
 * blackduck-artifactory-common
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
import java.util.List;
import java.util.stream.Collectors;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckPropertyManager;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;
import com.synopsys.integration.util.BuilderStatus;

public class ScanModuleConfig extends ModuleConfig {
    private final String addPolicyStatusCron;
    private final String binariesDirectoryPath;
    private final String artifactCutoffDate;
    private final Boolean dryRun;
    private final List<String> namePatterns;
    private final Integer memory;
    private final Boolean repoPathCodelocation;
    private final List<String> repos;
    private final String scanCron;

    private final File cliDirectory;

    private final DateTimeManager dateTimeManager;

    public ScanModuleConfig(final Boolean enabled, final String addPolicyStatusCron, final String binariesDirectoryPath, final String artifactCutoffDate, final Boolean dryRun, final List<String> namePatterns, final Integer memory,
        final Boolean repoPathCodelocation, final List<String> repos, final String scanCron, final File cliDirectory, final DateTimeManager dateTimeManager) {
        super(ScanModule.class.getSimpleName(), enabled);
        this.addPolicyStatusCron = addPolicyStatusCron;
        this.binariesDirectoryPath = binariesDirectoryPath;
        this.artifactCutoffDate = artifactCutoffDate;
        this.dryRun = dryRun;
        this.namePatterns = namePatterns;
        this.memory = memory;
        this.repoPathCodelocation = repoPathCodelocation;
        this.repos = repos;
        this.scanCron = scanCron;
        this.cliDirectory = cliDirectory;
        this.dateTimeManager = dateTimeManager;
    }

    public static ScanModuleConfig createFromProperties(final BlackDuckPropertyManager blackDuckPropertyManager, final ArtifactoryPAPIService artifactoryPAPIService, final File cliDirectory, final DateTimeManager dateTimeManager)
        throws IOException {
        final String addPolicyStatusCron = blackDuckPropertyManager.getProperty(ScanModuleProperty.ADD_POLICY_STATUS_CRON);
        final String binariesDirectoryPath = blackDuckPropertyManager.getProperty(ScanModuleProperty.BINARIES_DIRECTORY_PATH);
        final String artifactCutoffDate = blackDuckPropertyManager.getProperty(ScanModuleProperty.CUTOFF_DATE);
        final Boolean dryRun = blackDuckPropertyManager.getBooleanProperty(ScanModuleProperty.DRY_RUN);
        final Boolean enabled = blackDuckPropertyManager.getBooleanProperty(ScanModuleProperty.ENABLED);
        final List<String> namePatterns = blackDuckPropertyManager.getPropertyAsList(ScanModuleProperty.NAME_PATTERNS, blackDuckPropertyManager::getProperty);
        final Integer memory = blackDuckPropertyManager.getIntegerProperty(ScanModuleProperty.MEMORY);
        final Boolean repoPathCodelocation = blackDuckPropertyManager.getBooleanProperty(ScanModuleProperty.REPO_PATH_CODELOCATION);
        final List<String> repos = blackDuckPropertyManager.getRepositoryKeysFromProperties(ScanModuleProperty.REPOS, ScanModuleProperty.REPOS_CSV_PATH).stream()
                                       .filter(artifactoryPAPIService::isValidRepository)
                                       .collect(Collectors.toList());
        final String scanCron = blackDuckPropertyManager.getProperty(ScanModuleProperty.SCAN_CRON);

        return new ScanModuleConfig(enabled, addPolicyStatusCron, binariesDirectoryPath, artifactCutoffDate, dryRun, namePatterns, memory, repoPathCodelocation, repos, scanCron, cliDirectory, dateTimeManager);
    }

    @Override
    public void validate(final BuilderStatus builderStatus) {
        validateCronExpression(builderStatus, ScanModuleProperty.ADD_POLICY_STATUS_CRON, addPolicyStatusCron);
        validateNotBlank(builderStatus, ScanModuleProperty.BINARIES_DIRECTORY_PATH, binariesDirectoryPath, "Please specify a path");
        validateDate(builderStatus, ScanModuleProperty.CUTOFF_DATE, artifactCutoffDate, dateTimeManager);
        validateBoolean(builderStatus, ScanModuleProperty.DRY_RUN, dryRun);
        validateBoolean(builderStatus, ScanModuleProperty.ENABLED, isEnabledUnverified());
        validateInteger(builderStatus, ScanModuleProperty.MEMORY, memory);
        validateList(builderStatus, ScanModuleProperty.NAME_PATTERNS, namePatterns, "Please provide name patterns to match against");
        validateBoolean(builderStatus, ScanModuleProperty.REPO_PATH_CODELOCATION, repoPathCodelocation);
        validateList(builderStatus, ScanModuleProperty.REPOS, repos,
            String.format("No valid repositories specified. Please set the %s or %s property with valid repositories", ScanModuleProperty.REPOS.getKey(), ScanModuleProperty.REPOS_CSV_PATH.getKey()));
        validateCronExpression(builderStatus, ScanModuleProperty.SCAN_CRON, scanCron);
    }

    public String getAddPolicyStatusCron() {
        return addPolicyStatusCron;
    }

    public String getBinariesDirectoryPath() {
        return binariesDirectoryPath;
    }

    public String getArtifactCutoffDate() {
        return artifactCutoffDate;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public List<String> getNamePatterns() {
        return namePatterns;
    }

    public Integer getMemory() {
        return memory;
    }

    public Boolean getRepoPathCodelocation() {
        return repoPathCodelocation;
    }

    public List<String> getRepos() {
        return repos;
    }

    public String getScanCron() {
        return scanCron;
    }

    public File getCliDirectory() {
        return cliDirectory;
    }
}
