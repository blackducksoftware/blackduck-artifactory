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

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.artifactory.fs.FileLayoutInfo
import org.artifactory.repo.RepoPath
import org.artifactory.resource.ResourceStreamHandle
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

import com.blackducksoftware.integration.hub.api.view.MetaHandler
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig
import com.blackducksoftware.integration.hub.artifactory.BlackDuckProperty
import com.blackducksoftware.integration.hub.artifactory.PluginProperty
import com.blackducksoftware.integration.hub.builder.HubScanConfigBuilder
import com.blackducksoftware.integration.hub.dataservice.cli.CLIDataService
import com.blackducksoftware.integration.hub.dataservice.phonehome.PhoneHomeDataService
import com.blackducksoftware.integration.hub.dataservice.policystatus.PolicyStatusDescription
import com.blackducksoftware.integration.hub.exception.HubIntegrationException
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.model.request.ProjectRequest
import com.blackducksoftware.integration.hub.model.view.ProjectVersionView
import com.blackducksoftware.integration.hub.model.view.VersionBomPolicyStatusView
import com.blackducksoftware.integration.hub.request.builder.ProjectRequestBuilder
import com.blackducksoftware.integration.hub.rest.ApiKeyRestConnection
import com.blackducksoftware.integration.hub.scan.HubScanConfig
import com.blackducksoftware.integration.hub.service.HubService
import com.blackducksoftware.integration.hub.service.HubServicesFactory
import com.blackducksoftware.integration.hub.util.ProjectNameVersionGuess
import com.blackducksoftware.integration.hub.util.ProjectNameVersionGuesser
import com.blackducksoftware.integration.log.Slf4jIntLogger
import com.blackducksoftware.integration.phonehome.enums.ThirdPartyName
import com.blackducksoftware.integration.util.ResourceUtil

import groovy.transform.Field

// propertiesFilePathOverride allows you to specify an absolute path to the blackDuckScanForHub.properties file.
// If this is empty, we will default to ${ARTIFACTORY_HOME}/etc/plugins/lib/blackDuckScanForHub.properties
@Field String propertiesFilePathOverride = ""

@Field BlackDuckArtifactoryConfig blackDuckArtifactoryConfig
@Field HubServicesFactory hubServicesFactory
@Field File cronLogFile
@Field File cliDirectory

@Field int scanMemory
@Field boolean scanDryRun
@Field boolean logVerboseCronLog
@Field List<String> reposToSearch
@Field List<String> patternsToScan

@Field String dateTimePattern
@Field String artifactCutoffDate

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

        Set<RepoPath> repoPaths = searchForRepoPaths()
        scanArtifactPaths(repoPaths)

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

        Set<RepoPath> repoPaths = searchForRepoPaths()
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

        Set<RepoPath> repoPaths = searchForRepoPaths()
        repoPaths.each {
            if(repositories.getProperty(it, BlackDuckProperty.SCAN_RESULT.getName())?.equals('FAILURE')){
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
    blackDuckScan(cron: "0 0/1 * 1/1 * ?") {
        log.info('Starting blackDuckScan cron job...')

        logCronRun('blackDuckScan')

        Set<RepoPath> repoPaths = searchForRepoPaths()
        scanArtifactPaths(repoPaths)

        log.info('...completed blackDuckScan cron job.')
    }

    blackDuckAddPolicyStatus(cron: "0 0/1 * 1/1 * ?") {
        log.info('Starting blackDuckAddPolicyStatus cron job...')

        logCronRun('blackDuckAddPolicyStatus')

        Set<RepoPath> repoPaths = searchForRepoPaths()
        HubService hubService = hubServicesFactory.createHubService()

        populatePolicyStatuses(hubService, repoPaths)

        log.info('...completed blackDuckAddPolicyStatus cron job.')
    }
}

/**
 * Takes a FileLayoutInfo object and returns the project name as it will appear in the Hub. (By default, this returns the module of the FileLayoutInfo object)
 *
 * Feel free to modify this method to transform the FileLayoutInfo object as necessary to construct your desired project name.
 */
private String getProjectNameFromFileLayoutInfo(FileLayoutInfo fileLayoutInfo) {
    log.info('Constructing project name...')

    String constructedProjectName = fileLayoutInfo.module

    log.info('...project name constructed')
    return constructedProjectName
}

/**
 * Takes a FileLayoutInfo object and returns the project version name for as it will appear in the Hub. (By default, this returns the baseRevision of the FileLayoutInfo object)
 *
 * Feel free to modify this method to transform the FileLayoutInfo object as necessary to construct your desired project version name.
 */
private String getProjectVersionNameFromFileLayoutInfo(FileLayoutInfo fileLayoutInfo) {
    log.info('Constructing project version name...')

    String constructedProjectVersionName = fileLayoutInfo.baseRevision

    log.info('...project version constructed')
    return constructedProjectVersionName
}

//####################################################
//PLEASE MAKE NO EDITS BELOW THIS LINE - NO TOUCHY!!!
//####################################################
private Set<RepoPath> searchForRepoPaths() {
    def repoPaths = []
    patternsToScan.each {
        repoPaths.addAll(searches.artifactsByName(it, reposToSearch.toArray(new String[reposToSearch.size])))
    }

    repoPaths.toSet()
}

/**
 * If artifact's last modified time is newer than the scan time, or we have no record of the scan time, we should scan now, unless, if the cutoff date is set, only scan if the modified date is greater than or equal to the cutoff.
 */
private boolean shouldRepoPathBeScannedNow(RepoPath repoPath) {
    def itemInfo = repositories.getItemInfo(repoPath)
    long lastModifiedTime = itemInfo.lastModified

    boolean shouldCutoffPreventScanning = false
    if (StringUtils.isNotBlank(artifactCutoffDate)) {
        try {
            long cutoffTime = getTimeFromString(artifactCutoffDate)
            shouldCutoffPreventScanning = lastModifiedTime < cutoffTime
        } catch (Exception e) {
            log.error("The pattern: ${dateTimePattern} does not match the date string: ${artifactCutoffDate}: ${e.message}")
            shouldCutoffPreventScanning = false
        }
    }

    if (shouldCutoffPreventScanning) {
        log.warn("${itemInfo.name} was not scanned because the cutoff was set and the artifact is too old")
        return false
    }

    String blackDuckScanTimeProperty = repositories.getProperty(repoPath, BlackDuckProperty.SCAN_TIME.getName())
    if (StringUtils.isBlank(blackDuckScanTimeProperty)) {
        return true
    }

    try {
        long blackDuckScanTime = getTimeFromString(blackDuckScanTimeProperty)
        return lastModifiedTime >= blackDuckScanTime
    } catch (Exception e) {
        //if the date format changes, the old format won't parse, so just cleanup the property by returning true and re-scanning
        log.error("Exception parsing the scan date (most likely the format changed): ${e.message}")
    }

    return true
}

private void scanArtifactPaths(Set<RepoPath> repoPaths) {
    repoPaths = repoPaths.findAll {shouldRepoPathBeScannedNow(it)}
    repoPaths.each {
        try{
            String timeString = getNowString()
            repositories.setProperty(it, BlackDuckProperty.SCAN_TIME.getName(), timeString)
            FileLayoutInfo fileLayoutInfo = getArtifactFromPath(it)
            ProjectVersionView projectVersionView = scanArtifact(it, it.name, fileLayoutInfo)
            writeScanProperties(it, projectVersionView)
        } catch (Exception e) {
            log.error("Please investigate the scan logs for details - the Black Duck Scan did not complete successfully on ${it.name}: ${e.message}", e)
            repositories.setProperty(it, BlackDuckProperty.SCAN_RESULT.getName(), 'FAILURE')
        } finally {
            deletePathArtifact(it.name)
        }
    }
}

private FileLayoutInfo getArtifactFromPath(RepoPath repoPath) {
    ResourceStreamHandle resourceStream = repositories.getContent(repoPath)
    FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath)
    def inputStream
    def fileOutputStream
    try {
        inputStream = resourceStream.inputStream
        fileOutputStream = new FileOutputStream(new File(blackDuckArtifactoryConfig.blackDuckDirectory, repoPath.name))
        fileOutputStream << inputStream
    } catch (Exception e) {
        log.error("There was an error getting ${repoPath.name}: ${e.message}")
    } finally {
        ResourceUtil.closeQuietly(inputStream)
        ResourceUtil.closeQuietly(fileOutputStream)
        resourceStream.close()
    }
    fileLayoutInfo
}

private ProjectVersionView scanArtifact(RepoPath repoPath, String fileName, FileLayoutInfo fileLayoutInfo){
    ProjectRequestBuilder projectRequestBuilder = new ProjectRequestBuilder()
    HubScanConfigBuilder hubScanConfigBuilder = new HubScanConfigBuilder()
    hubScanConfigBuilder.scanMemory = scanMemory
    hubScanConfigBuilder.dryRun = scanDryRun
    hubScanConfigBuilder.toolsDir = cliDirectory
    hubScanConfigBuilder.workingDirectory = blackDuckArtifactoryConfig.blackDuckDirectory
    hubScanConfigBuilder.disableScanTargetPathExistenceCheck()

    String project = getProjectNameFromFileLayoutInfo(fileLayoutInfo)
    String version = getProjectVersionNameFromFileLayoutInfo(fileLayoutInfo)
    if (StringUtils.isBlank(project) || StringUtils.isBlank(version)) {
        String filenameWithoutExtension = FilenameUtils.getBaseName(fileName)
        ProjectNameVersionGuesser guesser = new ProjectNameVersionGuesser()
        ProjectNameVersionGuess guess = guesser.guessNameAndVersion(filenameWithoutExtension)
        project = guess.projectName
        version = guess.versionName
    }
    def scanFile = new File(hubScanConfigBuilder.workingDirectory, fileName)
    def scanTargetPath = scanFile.canonicalPath
    projectRequestBuilder.projectName = project
    projectRequestBuilder.versionName = version
    hubScanConfigBuilder.addScanTargetPath(scanTargetPath)

    HubScanConfig hubScanConfig = hubScanConfigBuilder.build()
    int hubTimeout = hubServicesFactory.getRestConnection().timeout
    CLIDataService cliDataService = hubServicesFactory.createCLIDataService(hubTimeout * 1000)
    PhoneHomeDataService phoneHomeDataService = hubServicesFactory.createPhoneHomeDataService()

    HubServerConfig hubServerConfig = blackDuckArtifactoryConfig.hubServerConfig
    ProjectRequest projectRequest = projectRequestBuilder.build()
    String thirdPartyVersion = '???'
    String pluginVersion = '???'
    try {
        thirdPartyVersion = ctx?.versionProvider?.running?.versionName
        pluginVersion = new File(blackDuckArtifactoryConfig.pluginsLibDirectory, 'version.txt')?.text
    } catch(Exception e) {
    }

    cliDataService.installAndRunControlledScan(hubServerConfig, hubScanConfig, projectRequest, false, ThirdPartyName.ARTIFACTORY, thirdPartyVersion, pluginVersion)
}

private void deletePathArtifact(String fileName){
    try {
        boolean deleteOk = new File(blackDuckArtifactoryConfig.blackDuckDirectory, fileName).delete()
        log.info("Successfully deleted temporary ${fileName}: ${Boolean.toString(deleteOk)}")
    } catch (Exception e) {
        log.error("Exception deleting ${fileName}: ${e.message}")
    }
}

private void writeScanProperties(RepoPath repoPath, ProjectVersionView projectVersionView){
    HubServicesFactory hubServicesFactory = createHubServicesFactory()
    HubService hubService = hubServicesFactory.createHubService()
    log.info("${repoPath.name} was successfully scanned by the BlackDuck CLI.")
    repositories.setProperty(repoPath, BlackDuckProperty.SCAN_RESULT.getName(), 'SUCCESS')

    if (projectVersionView) {
        String projectVersionUrl = ''
        String projectVersionUIUrl = ''
        try {
            projectVersionUrl = hubService.getHref(projectVersionView)
            if (StringUtils.isNotEmpty(projectVersionUrl)) {
                repositories.setProperty(repoPath, BlackDuckProperty.PROJECT_VERSION_URL.getName(), projectVersionUrl)
                log.info("Added ${projectVersionUrl} to ${repoPath.name}")
            }
            projectVersionUIUrl = hubService.getFirstLinkSafely(projectVersionView, 'components')
            if (StringUtils.isNotEmpty(projectVersionUIUrl)) {
                repositories.setProperty(repoPath, BlackDuckProperty.PROJECT_VERSION_UI_URL.getName(), projectVersionUIUrl)
                log.info("Added ${projectVersionUIUrl} to ${repoPath.name}")
            }
        } catch (Exception e) {
            log.error("Exception getting code location url: ${e.message}")
        }
    } else {
        log.warn('No scan summaries were available for a successful scan. This is expected if this was a dry run, but otherwise there should be summaries.')
    }
}

private void populatePolicyStatuses(HubService hubService, Set<RepoPath> repoPaths) {
    boolean problemRetrievingPolicyStatus = false
    repoPaths.each {
        try {
            String projectVersionUrl = repositories.getProperty(it, BlackDuckProperty.PROJECT_VERSION_URL.getName())
            if (StringUtils.isNotBlank(projectVersionUrl)) {
                projectVersionUrl = updateUrlPropertyToCurrentHubServer(projectVersionUrl)
                repositories.setProperty(it, BlackDuckProperty.PROJECT_VERSION_URL.getName(), projectVersionUrl)
                ProjectVersionView projectVersionView = hubService.getView(projectVersionUrl, ProjectVersionView.class)
                try{
                    String policyStatusUrl = hubService.getFirstLink(projectVersionView, MetaHandler.POLICY_STATUS_LINK)
                    log.info("Looking up policy status: ${policyStatusUrl}")
                    VersionBomPolicyStatusView versionBomPolicyStatusView = hubService.getView(policyStatusUrl, VersionBomPolicyStatusView.class)
                    log.info("policy status json: ${versionBomPolicyStatusView.json}")
                    PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(versionBomPolicyStatusView)
                    repositories.setProperty(it, BlackDuckProperty.POLICY_STATUS.getName(), policyStatusDescription.policyStatusMessage)
                    repositories.setProperty(it, BlackDuckProperty.OVERALL_POLICY_STATUS.getName(), versionBomPolicyStatusView.overallStatus.toString())
                    log.info("Added policy status to ${it.name}")
                } catch (HubIntegrationException e) {
                    problemRetrievingPolicyStatus = true
                }
            }
        } catch (Exception e) {
            log.error("There was a problem trying to access repository ${it.name}: ", e)
            problemRetrievingPolicyStatus = true
        }
    }
    if(problemRetrievingPolicyStatus){
        log.warn('There was a problem retrieving policy status for one or more repos. This is expected if you do not have policy management.')
    }
}

private String buildStatusCheckMessage() {
    def connectMessage = 'OK'
    try {
        HubServicesFactory hubServicesFactory = createHubServicesFactory()
        if (hubServicesFactory == null) {
            connectMessage = 'Could not create the connection to the Hub - you will have to check the artifactory logs.'
        }
    } catch (Exception e) {
        connectMessage = e.message
    }

    Set<RepoPath> repoPaths = searchForRepoPaths()

    def cutoffMessage = 'The date cutoff is not specified so all artifacts that are found will be scanned.'
    if (StringUtils.isNotBlank(artifactCutoffDate)) {
        try {
            getTimeFromString(artifactCutoffDate)
            cutoffMessage = 'The date cutoff is specified correctly.'
        } catch (Exception e) {
            cutoffMessage = "The pattern: ${dateTimePattern} does not match the date string: ${artifactCutoffDate}: ${e.message}"
        }
    }

    List<String> cronLogItems = getLatestCronLogItems()
    def cronLogResults = StringUtils.join(cronLogItems, '\n')

    """canConnectToHub: ${connectMessage}
artifactsFound: ${repoPaths.size()}
dateCutoffStatus: ${cutoffMessage}
loggedCronRuns:
${cronLogResults}
"""
}

private void deleteAllBlackDuckProperties(RepoPath repoPath) {
    repositories.deleteProperty(repoPath, BlackDuckProperty.SCAN_TIME.getName())
    repositories.deleteProperty(repoPath, BlackDuckProperty.SCAN_RESULT.getName())
    repositories.deleteProperty(repoPath, BlackDuckProperty.PROJECT_VERSION_URL.getName())
    repositories.deleteProperty(repoPath, BlackDuckProperty.PROJECT_VERSION_UI_URL.getName())
    repositories.deleteProperty(repoPath, BlackDuckProperty.POLICY_STATUS.getName())
    repositories.deleteProperty(repoPath, BlackDuckProperty.OVERALL_POLICY_STATUS.getName())
}

/**
 * If the hub server being used changes, the existing properties in artifactory could be inaccurate so we will update them when they differ from the hub url established in the properties file.
 */
private String updateUrlPropertyToCurrentHubServer(String urlProperty) {
    String hubUrl = hubServicesFactory.getRestConnection().hubBaseUrl.toString()
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
    updatedProperty
}

private void logCronRun(String methodName) {
    if (logVerboseCronLog) {
        String timeString = getNowString()
        if (cronLogFile.length() > 10000) {
            cronLogFile.delete()
            cronLogFile.createNewFile()
        }
        cronLogFile << "${methodName}\t${timeString}${System.lineSeparator}"
    }
}

private List<String> getLatestCronLogItems() {
    def List<String> lastTenLines = new ArrayList<>()
    cronLogFile.withReader {reader ->
        while (line = reader.readLine()) {
            if (lastTenLines.size() == 10) {
                lastTenLines.remove(0)
            }
            lastTenLines.add(line)
        }
    }

    return lastTenLines
}

private HubServicesFactory createHubServicesFactory() {
    HubServerConfig hubServerConfig = blackDuckArtifactoryConfig.hubServerConfig
    ApiKeyRestConnection apiKeyRestConnection = hubServerConfig.createApiKeyRestConnection(new Slf4jIntLogger(log))
    hubServicesFactory = new HubServicesFactory(apiKeyRestConnection)
}

private void initialize() {
    blackDuckArtifactoryConfig = new BlackDuckArtifactoryConfig()
    blackDuckArtifactoryConfig.setEtcDirectory(ctx.artifactoryHome.etcDir)
    blackDuckArtifactoryConfig.setHomeDirectory(ctx.artifactoryHome.homeDir)
    blackDuckArtifactoryConfig.setPluginsDirectory(ctx.artifactoryHome.pluginsDir)

    loadProperties()
}

private void loadProperties() {
    def propertiesFile
    if (StringUtils.isNotBlank(propertiesFilePathOverride)) {
        propertiesFile = new File(propertiesFilePathOverride);
    } else {
        propertiesFile = new File(blackDuckArtifactoryConfig.pluginsLibDirectory, "${this.getClass().getSimpleName()}.properties")
    }

    try {
        blackDuckArtifactoryConfig.loadProperties(propertiesFile)
        scanMemory = Integer.parseInt(blackDuckArtifactoryConfig.getProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_MEMORY))
        scanDryRun = Boolean.parseBoolean(blackDuckArtifactoryConfig.getProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_DRY_RUN))
        logVerboseCronLog = Boolean.parseBoolean(blackDuckArtifactoryConfig.getProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_CRON_LOG_VERBOSE))
        reposToSearch = blackDuckArtifactoryConfig.getProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_REPOS).tokenize(',')
        patternsToScan = blackDuckArtifactoryConfig.getProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_NAME_PATTERNS).tokenize(',')
        dateTimePattern = blackDuckArtifactoryConfig.getProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_DATE_TIME_PATTERN)
        artifactCutoffDate = blackDuckArtifactoryConfig.getProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_CUTOFF_DATE)

        String scanBinariesDirectory = blackDuckArtifactoryConfig.getProperties().getProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_BINARIES_DIRECTORY_PATH)
        if (scanBinariesDirectory) {
            blackDuckArtifactoryConfig.setBlackDuckDirectory(FilenameUtils.concat(blackDuckArtifactoryConfig.homeDirectory.canonicalPath, scanBinariesDirectory))
        } else {
            blackDuckArtifactoryConfig.setBlackDuckDirectory(FilenameUtils.concat(blackDuckArtifactoryConfig.etcDirectory.canonicalPath, 'blackducksoftware'))
        }
        cliDirectory = new File(blackDuckArtifactoryConfig.blackDuckDirectory, 'cli')
        cliDirectory.mkdirs()

        cronLogFile = new File(blackDuckArtifactoryConfig.blackDuckDirectory, 'blackduck_cron_history')
        cronLogFile.createNewFile()

        createHubServicesFactory()
    } catch (Exception e) {
        log.error("Black Duck Scanner encountered an unexpected error when trying to load its properties file at ${propertiesFile.getAbsolutePath()}", e)
    }
}

private String getNowString() {
    DateTime.now().withZone(DateTimeZone.UTC).toString(DateTimeFormat.forPattern(dateTimePattern).withZoneUTC())
}

private long getTimeFromString(String dateTimeString) {
    DateTime.parse(dateTimeString, DateTimeFormat.forPattern(dateTimePattern).withZoneUTC()).toDate().time
}
