/*
 * hub-artifactory
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
package com.blackducksoftware.integration.hub.artifactory.scan

import com.blackducksoftware.integration.hub.artifactory.ArtifactoryPropertyService
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryProperty
import com.blackducksoftware.integration.hub.artifactory.HubConnectionService
import groovy.transform.Field
import org.apache.commons.io.FileUtils
import org.artifactory.repo.RepoPath

// propertiesFilePathOverride allows you to specify an absolute path to the blackDuckScanForHub.properties file.
// If this is empty, we will default to ${ARTIFACTORY_HOME}/etc/plugins/lib/blackDuckScanForHub.properties
@Field String propertiesFilePathOverride = ""

@Field BlackDuckArtifactoryConfig blackDuckArtifactoryConfig
@Field RepositoryIdentificationService repositoryIdentificationService
@Field ArtifactScanService artifactScanService
@Field ScanArtifactoryConfig scanArtifactoryConfig
@Field ArtifactoryPropertyService artifactoryPropertyService
@Field HubConnectionService hubConnectionService
@Field StatusCheckService statusCheckService

initialize()

executions {
    /**
     * This will attempt to reload the properties file and initialize the scanner with the new values.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckReloadScanner"
     */
    blackDuckReloadScanner(httpMethod: 'POST') { params ->
        log.info('Starting blackDuckReloadScanner REST request...')

        initialize()

        log.info('...completed blackDuckReloadScanner REST request.')
    }

    /**
     * This will search your artifactory ARTIFACTORY_REPOS_TO_SEARCH repositories for the filename patterns designated in ARTIFACT_NAME_PATTERNS_TO_SCAN.
     * For example:
     *
     * ARTIFACTORY_REPOS_TO_SEARCH="my-releases,my-snapshots"
     * ARTIFACT_NAME_PATTERNS_TO_SCAN="*.war,*.zip"
     *
     * then this REST call will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files, scan them, and publish the BOM to the provided Hub server.
     *
     * The scanning process will add several properties to your artifacts in artifactory. Namely:
     *
     * blackDuckScanResult - SUCCESS or FAILURE, depending on the result of the scan
     * blackDuckScanTime - the last time a SUCCESS scan was completed
     * blackDuckScanCodeLocationUrl - the url for the code location created in the Hub
     *
     * The same functionality is provided via the scanForHub cron job to enable scheduled scans to run consistently.
     *
     * This can be triggered with the following curl command:
     * curl -X GET -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckScan"
     */
    blackDuckScan(httpMethod: 'GET') { params ->
        log.info('Starting blackDuckScan REST request...')

        Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths()
        artifactScanService.scanArtifactPaths(repoPaths)

        log.info('...completed blackDuckScan REST request.')
    }

    /**
     * This will return a current status of the plugin's configuration to verify things are setup properly.
     *
     * This can be triggered with the following curl command:
     * curl -X GET -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckTestConfig"
     */
    blackDuckTestConfig(httpMethod: 'GET') { params ->
        log.info('Starting blackDuckTestConfig REST request...')

        message = statusCheckService.getStatusMessage()

        log.info('...completed blackDuckTestConfig REST request.')
    }

    /**
     * This will delete, then recreate, the blackducksoftware directory which includes the cli, the cron job log, as well as all the cli logs.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckReloadDirectory"
     */
    blackDuckReloadDirectory() { params ->
        log.info('Starting blackDuckReloadDirectory REST request...')

        FileUtils.deleteDirectory(blackDuckArtifactoryConfig.blackDuckDirectory)
        blackDuckArtifactoryConfig.blackDuckDirectory.mkdirs()

        log.info('...completed blackDuckReloadDirectory REST request.')
    }

    /**
     * This will search your artifactory ARTIFACTORY_REPOS_TO_SEARCH repositories for the filename patterns designated in ARTIFACT_NAME_PATTERNS_TO_SCAN.
     * For example:
     *
     * ARTIFACTORY_REPOS_TO_SEARCH="my-releases,my-snapshots"
     * ARTIFACT_NAME_PATTERNS_TO_SCAN="*.war,*.zip"
     *
     * then this REST call will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files and delete all the properties that the plugin sets.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteScanProperties"
     */
    blackDuckDeleteScanProperties() { params ->
        log.info('Starting blackDuckDeleteScanProperties REST request...')

        Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths()
        repoPaths.each { artifactoryPropertyService.deleteAllBlackDuckPropertiesFrom(it.repoKey) }

        log.info('...completed blackDuckDeleteScanProperties REST request.')
    }

    /**
     * This will search your artifactory ARTIFACTORY_REPOS_TO_SEARCH repositories for the filename patterns designated in ARTIFACT_NAME_PATTERNS_TO_SCAN.
     * For example:
     *
     * ARTIFACTORY_REPOS_TO_SEARCH="my-releases,my-snapshots"
     * ARTIFACT_NAME_PATTERNS_TO_SCAN="*.war,*.zip"
     *
     * then this REST call will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files and checks for the 'blackduck.scanResult' property
     * if that property indicates a scan failure, it delete all the properties that the plugin set on it.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteScanPropertiesFromFailures"
     */
    blackDuckDeleteScanPropertiesFromFailures() { params ->
        log.info('Starting blackDuckDeleteScanPropertiesFromFailures REST request...')

        Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths()
        repoPaths.each {
            if (repositories.getProperty(it, BlackDuckArtifactoryProperty.SCAN_RESULT.getName())?.equals('FAILURE')) {
                artifactoryPropertyService.deleteAllBlackDuckPropertiesFrom(it)
            }
        }

        log.info('...completed blackDuckDeleteScanPropertiesFromFailures REST request.')
    }
}

jobs {
    /**
     * This will search your artifactory ARTIFACTORY_REPOS_TO_SEARCH repositories for the filename patterns designated in ARTIFACT_NAME_PATTERNS_TO_SCAN.
     * For example:
     *
     * ARTIFACTORY_REPOS_TO_SEARCH="my-releases,my-snapshots"
     * ARTIFACT_NAME_PATTERNS_TO_SCAN="*.war,*.zip"
     *
     * then this cron job will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files, scan them, and publish the BOM to the provided Hub server.
     *
     * The scanning process will add several properties to your artifacts in artifactory. Namely:
     *
     * blackduck.scanResult - SUCCESS or FAILURE, depending on the result of the scan
     * blackduck.scanTime - the last time a SUCCESS scan was completed
     * blackduck.uiUrl - the url directly to the scanned BOM in the Hub
     * blackduck.apiUrl - the api url for your project which is needed for further Hub REST calls.
     *
     * The same functionality is provided via the scanForHub execution to enable a one-time scan triggered via a REST call.
     */
    blackDuckScan(cron: scanArtifactoryConfig.blackDuckScanCron) {
        log.info('Starting blackDuckScan cron job...')

        Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths()
        artifactScanService.scanArtifactPaths(repoPaths)

        log.info('...completed blackDuckScan cron job.')
    }

    blackDuckAddPolicyStatus(cron: scanArtifactoryConfig.blackDuckAddPolicyStatusCron) {
        log.info('Starting blackDuckAddPolicyStatus cron job...')

        Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths()
        hubConnectionService.populatePolicyStatuses(repoPaths)

        log.info('...completed blackDuckAddPolicyStatus cron job.')
    }
}

//####################################################
//PLEASE MAKE NO EDITS BELOW THIS LINE - NO TOUCHY!!!
//####################################################

private void initialize() {
    blackDuckArtifactoryConfig = new BlackDuckArtifactoryConfig()
    blackDuckArtifactoryConfig.setEtcDirectory(ctx.artifactoryHome.etcDir.toString())
    blackDuckArtifactoryConfig.setHomeDirectory(ctx.artifactoryHome.homeDir.toString())
    blackDuckArtifactoryConfig.setPluginsDirectory(ctx.artifactoryHome.pluginsDir.toString())
    blackDuckArtifactoryConfig.setThirdPartyVersion(ctx?.versionProvider?.running?.versionName?.toString())
    blackDuckArtifactoryConfig.setPluginName(this.getClass().getSimpleName())
    blackDuckArtifactoryConfig.loadProperties(propertiesFilePathOverride)

    // The ScanArtifactoryConfig must be created before other services
    scanArtifactoryConfig = new ScanArtifactoryConfig(blackDuckArtifactoryConfig)
    scanArtifactoryConfig.setUpBlackDuckDirectory()

    artifactoryPropertyService = new ArtifactoryPropertyService(repositories, searches, scanArtifactoryConfig.getDateTimeManager())
    hubConnectionService = new HubConnectionService(blackDuckArtifactoryConfig, artifactoryPropertyService, scanArtifactoryConfig.getDateTimeManager())
    repositoryIdentificationService = new RepositoryIdentificationService(blackDuckArtifactoryConfig, scanArtifactoryConfig, repositories, searches)
    artifactScanService = new ArtifactScanService(blackDuckArtifactoryConfig, repositoryIdentificationService, scanArtifactoryConfig, hubConnectionService, artifactoryPropertyService, repositories)
    statusCheckService = new StatusCheckService(scanArtifactoryConfig, hubConnectionService, repositoryIdentificationService)
}
