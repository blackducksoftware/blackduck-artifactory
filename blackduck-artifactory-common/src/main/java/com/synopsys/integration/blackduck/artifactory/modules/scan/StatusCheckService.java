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

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.artifactory.BlackDuckConnectionService;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;

public class StatusCheckService {
    private final ScanModuleConfig scanModuleConfig;
    private final BlackDuckConnectionService blackDuckConnectionService;
    private final RepositoryIdentificationService repositoryIdentificationService;
    private final DateTimeManager dateTimeManager;

    public StatusCheckService(final ScanModuleConfig scanModuleConfig, final BlackDuckConnectionService blackDuckConnectionService,
        final RepositoryIdentificationService repositoryIdentificationService, final DateTimeManager dateTimeManager) {
        this.scanModuleConfig = scanModuleConfig;
        this.blackDuckConnectionService = blackDuckConnectionService;
        this.repositoryIdentificationService = repositoryIdentificationService;
        this.dateTimeManager = dateTimeManager;
    }

    public String getStatusMessage() {
        final StringBuilder statusMessageBuilder = new StringBuilder();

        String connectMessage = "OK";
        if (blackDuckConnectionService == null) {
            connectMessage = "Could not create the connection to BlackDuck - you will have to check the artifactory logs.";
        }

        Set<RepoPath> repoPaths = null;
        String artifactsFoundMessage = "UNKNOWN - you will have to check the artifactory logs.";
        repoPaths = repositoryIdentificationService.searchForRepoPaths();

        if (repoPaths != null) {
            artifactsFoundMessage = String.valueOf(repoPaths.size());
        }

        String cutoffMessage = "The date cutoff is not specified so all artifacts that are found will be scanned.";
        if (StringUtils.isNotBlank(scanModuleConfig.getArtifactCutoffDate())) {
            try {
                dateTimeManager.getTimeFromString(scanModuleConfig.getArtifactCutoffDate());
                cutoffMessage = "The date cutoff is specified correctly.";
            } catch (final Exception e) {
                cutoffMessage = String.format("The pattern: %s does not match the date string: %s: %s", dateTimeManager.getDateTimePattern(), scanModuleConfig.getArtifactCutoffDate(), e.getMessage());
            }
        }

        statusMessageBuilder.append(String.format("canConnectToHub: %s%n", connectMessage));
        statusMessageBuilder.append(String.format("artifactsFound: %s%n", artifactsFoundMessage));
        statusMessageBuilder.append(String.format("dateCutoffStatus: %s%n", cutoffMessage));

        return statusMessageBuilder.toString();
    }
}
