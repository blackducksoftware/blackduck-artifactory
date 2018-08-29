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

import com.blackducksoftware.integration.hub.api.generated.view.VersionBomPolicyStatusView
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryProperty
import com.blackducksoftware.integration.hub.artifactory.BlackDuckHubProperty
import com.blackducksoftware.integration.hub.artifactory.DateTimeManager
import com.blackducksoftware.integration.hub.artifactory.HubConnectionService
import com.blackducksoftware.integration.hub.exception.HubIntegrationException
import com.blackducksoftware.integration.hub.service.model.PolicyStatusDescription
import embedded.org.apache.commons.lang3.StringUtils
import groovy.transform.Field
import org.apache.commons.io.FileUtils
import org.artifactory.repo.RepoPath

// propertiesFilePathOverride allows you to specify an absolute path to the blackDuckScanForHub.properties file.
// If this is empty, we will default to ${ARTIFACTORY_HOME}/etc/plugins/lib/blackDuckScanForHub.properties
@Field String propertiesFilePathOverride = ""

@Field BlackDuckArtifactoryConfig blackDuckArtifactoryConfig
@Field DateTimeManager dateTimeManager
@Field HubConnectionService hubConnectionService
@Field RepositoryIdentifactionService repositoryIdentifactionService
@Field ScanPhoneHomeService scanPhoneHomeService
@Field ArtifactScanService artifactScanService
@Field ScanFileService scanFileService
@Field File cliDirectory
@Field String artifactCutoffDate
@Field String blackDuckScanCron
@Field String blackDuckAddPolicyStatusCron
@Field String thirdPartyVersion

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

        Set<RepoPath> repoPaths = repositoryIdentifactionService.searchForRepoPaths()
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

        message = buildStatusCheckMessage()

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

        Set<RepoPath> repoPaths = repositoryIdentifactionService.searchForRepoPaths()
        repoPaths.each { deleteAllBlackDuckProperties(it) }

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

        Set<RepoPath> repoPaths = repositoryIdentifactionService.searchForRepoPaths()
        repoPaths.each {
            if (repositories.getProperty(it, BlackDuckArtifactoryProperty.SCAN_RESULT.getName())?.equals('FAILURE')) {
                deleteAllBlackDuckProperties(it)
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
    blackDuckScan(cron: blackDuckScanCron) {
        log.info('Starting blackDuckScan cron job...')

        Set<RepoPath> repoPaths = repositoryIdentifactionService.searchForRepoPaths()
        artifactScanService.scanArtifactPaths(repoPaths)

        log.info('...completed blackDuckScan cron job.')
    }

    blackDuckAddPolicyStatus(cron: blackDuckAddPolicyStatusCron) {
        log.info('Starting blackDuckAddPolicyStatus cron job...')

        Set<RepoPath> repoPaths = repositoryIdentifactionService.searchForRepoPaths()
        populatePolicyStatuses(repoPaths)

        log.info('...completed blackDuckAddPolicyStatus cron job.')
    }
}

//####################################################
//PLEASE MAKE NO EDITS BELOW THIS LINE - NO TOUCHY!!!
//####################################################

private void populatePolicyStatuses(Set<RepoPath> repoPaths) {
    boolean problemRetrievingPolicyStatus = false
    repoPaths.each {
        try {
            String projectVersionUrl = repositories.
            getProperty(it, BlackDuckArtifactoryProperty.PROJECT_VERSION_URL.getName())
            if (StringUtils.isNotBlank(projectVersionUrl)) {
                projectVersionUrl = updateUrlPropertyToCurrentHubServer(projectVersionUrl)
                repositories.setProperty(it, BlackDuckArtifactoryProperty.PROJECT_VERSION_URL.getName(), projectVersionUrl)
                try {
                    VersionBomPolicyStatusView versionBomPolicyStatusView = hubConnectionService.getPolicyStatusOfProjectVersion(projectVersionUrl);
                    log.info("policy status json: " + versionBomPolicyStatusView.json);
                    PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(versionBomPolicyStatusView);
                    repositories.setProperty(it, BlackDuckArtifactoryProperty.POLICY_STATUS.getName(), policyStatusDescription.policyStatusMessage)
                    repositories.setProperty(it, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS.getName(), versionBomPolicyStatusView.overallStatus.toString())
                    log.info("Added policy status to ${it.name}")
                    repositories.setProperty(it, BlackDuckArtifactoryProperty.UPDATE_STATUS.getName(), 'UP TO DATE')
                    repositories.setProperty(it, BlackDuckArtifactoryProperty.LAST_UPDATE.getName(), dateTimeManager.getStringFromDate(new Date()))
                    scanPhoneHomeService.phoneHome()
                } catch (HubIntegrationException e) {
                    problemRetrievingPolicyStatus = true
                    def policyStatus = repositories.getProperty(it, BlackDuckArtifactoryProperty.POLICY_STATUS.getName())
                    def overallPolicyStatus = repositories.getProperty(it, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS.getName())
                    if (StringUtils.isNotBlank(policyStatus) || StringUtils.isNotBlank(overallPolicyStatus)) {
                        repositories.setProperty(it, BlackDuckArtifactoryProperty.UPDATE_STATUS.getName(), 'OUT OF DATE')
                    }
                }
            }
        } catch (Exception e) {
            log.error("There was a problem trying to access repository ${it.name}: ", e)
            problemRetrievingPolicyStatus = true
        }
    }
    if (problemRetrievingPolicyStatus) {
        log.warn('There was a problem retrieving policy status for one or more repos. This is expected if you do not have policy management.')
    }
}

private String buildStatusCheckMessage() {
    def connectMessage = 'OK'
    try {
        if (hubConnectionService == null) {
            connectMessage = 'Could not create the connection to the Hub - you will have to check the artifactory logs.'
        }
    } catch (Exception e) {
        connectMessage = e.message
    }

    Set<RepoPath> repoPaths = repositoryIdentifactionService.searchForRepoPaths()

    def cutoffMessage = 'The date cutoff is not specified so all artifacts that are found will be scanned.'
    if (StringUtils.isNotBlank(artifactCutoffDate)) {
        try {
            dateTimeManager.getTimeFromString(artifactCutoffDate)
            cutoffMessage = 'The date cutoff is specified correctly.'
        } catch (Exception e) {
            cutoffMessage = "The pattern: ${dateTimeManager.dateTimePattern} does not match the date string: ${artifactCutoffDate}: ${e.message}"
        }
    }

    return """canConnectToHub: ${connectMessage}
artifactsFound: ${repoPaths.size()}
dateCutoffStatus: ${cutoffMessage}
"""
}

private void deleteAllBlackDuckProperties(RepoPath repoPath) {
    repositories.deleteProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_TIME.getName())
    repositories.deleteProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_RESULT.getName())
    repositories.deleteProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_URL.getName())
    repositories.deleteProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL.getName())
    repositories.deleteProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS.getName())
    repositories.deleteProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS.getName())
}

/**
 * If the hub server being used changes, the existing properties in artifactory could be inaccurate so we will update them when they differ from the hub url established in the properties file.
 */
private String updateUrlPropertyToCurrentHubServer(String urlProperty) {
    String hubUrl = blackDuckArtifactoryConfig.getProperty(BlackDuckHubProperty.URL);
    if (urlProperty.startsWith(hubUrl)) {
        return urlProperty
    }

    //get the old hub url from the existing property
    URL urlFromProperty = new URL(urlProperty)
    String hubUrlFromProperty = urlFromProperty.protocol + '://' + urlFromProperty.host
    if (urlFromProperty.port > 0) {
        hubUrlFromProperty += ':' + Integer.toString(urlFromProperty.port)
    }
    String urlEndpoint = urlProperty.replace(hubUrlFromProperty, '')

    String updatedProperty = hubUrl + urlEndpoint
    return updatedProperty
}

private void initialize() {
    blackDuckArtifactoryConfig = new BlackDuckArtifactoryConfig()
    blackDuckArtifactoryConfig.setEtcDirectory(ctx.artifactoryHome.etcDir.toString())
    blackDuckArtifactoryConfig.setHomeDirectory(ctx.artifactoryHome.homeDir.toString())
    blackDuckArtifactoryConfig.setPluginsDirectory(ctx.artifactoryHome.pluginsDir.toString())

    thirdPartyVersion = ctx?.versionProvider?.running?.versionName

    loadProperties()

    // Services must be created after the properties are loaded
    scanFileService = new ScanFileService(blackDuckArtifactoryConfig, hubConnectionService, repositories)
    scanFileService.setUpBlackDuckDirectory()
    repositoryIdentifactionService = new RepositoryIdentifactionService(blackDuckArtifactoryConfig, repositories, searches, dateTimeManager)
    scanPhoneHomeService = new ScanPhoneHomeService(blackDuckArtifactoryConfig, hubConnectionService, thirdPartyVersion)
    artifactScanService = new ArtifactScanService(blackDuckArtifactoryConfig, repositoryIdentifactionService, scanFileService, scanPhoneHomeService, repositories, dateTimeManager)
}

private void loadProperties() {
    File propertiesFile
    if (StringUtils.isNotBlank(propertiesFilePathOverride)) {
        propertiesFile = new File(propertiesFilePathOverride)
    } else {
        propertiesFile = new File(blackDuckArtifactoryConfig.pluginsLibDirectory, "${this.getClass().getSimpleName()}.properties")
    }

    try {
        blackDuckArtifactoryConfig.loadProperties(propertiesFile)
        artifactCutoffDate = blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.CUTOFF_DATE)
        blackDuckScanCron = blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.SCAN_CRON)
        blackDuckAddPolicyStatusCron = blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.ADD_POLICY_STATUS_CRON)
        dateTimeManager = new DateTimeManager(blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.DATE_TIME_PATTERN));
        hubConnectionService = new HubConnectionService(blackDuckArtifactoryConfig)
    } catch (Exception e) {
        log.error("Black Duck Scanner encountered an unexpected error when trying to load its properties file at ${propertiesFile.getAbsolutePath()}")
        throw e
    }
}
