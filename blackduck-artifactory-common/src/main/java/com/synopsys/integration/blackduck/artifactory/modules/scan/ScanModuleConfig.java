/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationPropertyManager;
import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyGroupReport;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;

public class ScanModuleConfig extends ModuleConfig {
    private final String cron;
    private final String binariesDirectoryPath;
    private final String artifactCutoffDate;
    private final Boolean dryRun;
    private final List<String> namePatterns;
    private final Integer memory;
    private final Boolean repoPathCodelocation;
    private final List<String> repos;
    private final Boolean codelocationIncludeHostname;

    private final File cliDirectory;

    private final DateTimeManager dateTimeManager;

    public ScanModuleConfig(final Boolean enabled, final String cron, final String binariesDirectoryPath, final String artifactCutoffDate, final Boolean dryRun, final List<String> namePatterns, final Integer memory,
        final Boolean repoPathCodelocation, final List<String> repos, final Boolean codelocationIncludeHostname, final File cliDirectory, final DateTimeManager dateTimeManager) {
        super(ScanModule.class.getSimpleName(), enabled);
        this.cron = cron;
        this.binariesDirectoryPath = binariesDirectoryPath;
        this.artifactCutoffDate = artifactCutoffDate;
        this.dryRun = dryRun;
        this.namePatterns = namePatterns;
        this.memory = memory;
        this.repoPathCodelocation = repoPathCodelocation;
        this.repos = repos;
        this.codelocationIncludeHostname = codelocationIncludeHostname;
        this.cliDirectory = cliDirectory;
        this.dateTimeManager = dateTimeManager;
    }

    public static ScanModuleConfig createFromProperties(final ConfigurationPropertyManager configurationPropertyManager, final ArtifactoryPAPIService artifactoryPAPIService, final File cliDirectory, final DateTimeManager dateTimeManager)
        throws IOException {
        final String cron = configurationPropertyManager.getProperty(ScanModuleProperty.CRON);
        final String binariesDirectoryPath = configurationPropertyManager.getProperty(ScanModuleProperty.BINARIES_DIRECTORY_PATH);
        final String artifactCutoffDate = configurationPropertyManager.getProperty(ScanModuleProperty.CUTOFF_DATE);
        final Boolean dryRun = configurationPropertyManager.getBooleanProperty(ScanModuleProperty.DRY_RUN);
        final Boolean enabled = configurationPropertyManager.getBooleanProperty(ScanModuleProperty.ENABLED);
        final List<String> namePatterns = configurationPropertyManager.getPropertyAsList(ScanModuleProperty.NAME_PATTERNS);
        final Integer memory = configurationPropertyManager.getIntegerProperty(ScanModuleProperty.MEMORY);
        final Boolean repoPathCodelocation = configurationPropertyManager.getBooleanProperty(ScanModuleProperty.REPO_PATH_CODELOCATION);
        final List<String> repos = configurationPropertyManager.getRepositoryKeysFromProperties(ScanModuleProperty.REPOS, ScanModuleProperty.REPOS_CSV_PATH).stream()
                                       .filter(artifactoryPAPIService::isValidRepository)
                                       .collect(Collectors.toList());
        final Boolean codelocationIncludeHostname = configurationPropertyManager.getBooleanProperty(ScanModuleProperty.CODELOCATION_INCLUDE_HOSTNAME);

        return new ScanModuleConfig(enabled, cron, binariesDirectoryPath, artifactCutoffDate, dryRun, namePatterns, memory, repoPathCodelocation, repos, codelocationIncludeHostname, cliDirectory, dateTimeManager);
    }

    @Override
    public void validate(final PropertyGroupReport propertyGroupReport) {
        validateCronExpression(propertyGroupReport, ScanModuleProperty.CRON, cron);
        validateNotBlank(propertyGroupReport, ScanModuleProperty.BINARIES_DIRECTORY_PATH, binariesDirectoryPath, "Please specify a path");
        validateDate(propertyGroupReport, ScanModuleProperty.CUTOFF_DATE, artifactCutoffDate, dateTimeManager);
        validateBoolean(propertyGroupReport, ScanModuleProperty.DRY_RUN, dryRun);
        validateBoolean(propertyGroupReport, ScanModuleProperty.ENABLED, isEnabledUnverified());
        validateInteger(propertyGroupReport, ScanModuleProperty.MEMORY, memory);
        validateList(propertyGroupReport, ScanModuleProperty.NAME_PATTERNS, namePatterns, "Please provide name patterns to match against");
        validateBoolean(propertyGroupReport, ScanModuleProperty.REPO_PATH_CODELOCATION, repoPathCodelocation);
        validateList(propertyGroupReport, ScanModuleProperty.REPOS, repos,
            String.format("No valid repositories specified. Please set the %s or %s property with valid repositories", ScanModuleProperty.REPOS.getKey(), ScanModuleProperty.REPOS_CSV_PATH.getKey()));
    }

    public String getCron() {
        return cron;
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

    public File getCliDirectory() {
        return cliDirectory;
    }

    public Boolean getCodelocationIncludeHostname() {
        return codelocationIncludeHostname;
    }
}
