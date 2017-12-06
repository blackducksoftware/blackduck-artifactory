/**
 * Hub Artifactory Plugin
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
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
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.artifactory.fs.FileLayoutInfo
import org.artifactory.repo.RepoPath
import org.artifactory.resource.ResourceStreamHandle
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

import com.blackducksoftware.integration.hub.api.item.MetaService
import com.blackducksoftware.integration.hub.builder.HubScanConfigBuilder
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder
import com.blackducksoftware.integration.hub.dataservice.cli.CLIDataService
import com.blackducksoftware.integration.hub.dataservice.phonehome.PhoneHomeDataService
import com.blackducksoftware.integration.hub.dataservice.policystatus.PolicyStatusDescription
import com.blackducksoftware.integration.hub.exception.HubIntegrationException
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.model.request.ProjectRequest
import com.blackducksoftware.integration.hub.model.view.ProjectVersionView
import com.blackducksoftware.integration.hub.model.view.VersionBomPolicyStatusView
import com.blackducksoftware.integration.hub.request.builder.ProjectRequestBuilder
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection
import com.blackducksoftware.integration.hub.scan.HubScanConfig
import com.blackducksoftware.integration.hub.service.HubResponseService
import com.blackducksoftware.integration.hub.service.HubServicesFactory
import com.blackducksoftware.integration.hub.util.ProjectNameVersionGuess
import com.blackducksoftware.integration.hub.util.ProjectNameVersionGuesser
import com.blackducksoftware.integration.log.Slf4jIntLogger
import com.blackducksoftware.integration.phonehome.enums.ThirdPartyName

import groovy.transform.Field

@Field final String HUB_URL=""
@Field final int HUB_TIMEOUT=120
@Field final String HUB_USERNAME=""
@Field final String HUB_PASSWORD=""

@Field final String HUB_PROXY_HOST=""
@Field final int HUB_PROXY_PORT=0
@Field final String HUB_PROXY_USERNAME=""
@Field final String HUB_PROXY_PASSWORD=""

@Field final boolean HUB_ALWAYS_TRUST_CERTS=false

@Field final int HUB_SCAN_MEMORY=4096
@Field final boolean HUB_SCAN_DRY_RUN=false

@Field final String ARTIFACTORY_REPOS_TO_SEARCH="ext-release-local,libs-release"
@Field final String ARTIFACT_NAME_PATTERNS_TO_SCAN="*.war,*.zip,*.tar.gz,*.hpi"
@Field final String BLACK_DUCK_SCAN_BINARIES_DIRECTORY_PATH="etc/blackducksoftware"

@Field final boolean logVerboseCronLog=false

@Field final String DATE_TIME_PATTERN="yyyy-MM-dd'T'HH:mm:ss.SSS"
@Field final String BLACK_DUCK_SCAN_TIME_PROPERTY_NAME="blackduck.scanTime"
@Field final String BLACK_DUCK_SCAN_RESULT_PROPERTY_NAME="blackduck.scanResult"
@Field final String BLACK_DUCK_PROJECT_VERSION_URL_PROPERTY_NAME="blackduck.apiUrl"
@Field final String BLACK_DUCK_PROJECT_VERSION_UI_URL_PROPERTY_NAME="blackduck.uiUrl"
@Field final String BLACK_DUCK_POLICY_STATUS_PROPERTY_NAME="blackduck.policyStatus"
@Field final String BLACK_DUCK_OVERALL_POLICY_STATUS_PROPERTY_NAME="blackduck.overallPolicyStatus"


//if this is set, only artifacts with a modified date later than the CUTOFF will be scanned. You will have to use the
//DATE_TIME_PATTERN defined above for the cutoff to work properly. With the default pattern, to scan only artifacts newer than January 01, 2016 you would use
//the cutoff string = "2016-01-01T00:00:00.000"
@Field final String ARTIFACT_CUTOFF_DATE="2016-01-01T00:00:00.000"

@Field boolean initialized=false
@Field File etcDir
@Field File homeDir
@Field File blackDuckDirectory
@Field File cliDirectory


executions {
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

        initializeConfiguration()
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

        initializeConfiguration()
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

        initializeConfiguration()

        FileUtils.deleteDirectory(blackDuckDirectory)
        blackDuckDirectory.mkdirs()

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

        initializeConfiguration()

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

        initializeConfiguration()

        Set<RepoPath> repoPaths = searchForRepoPaths()
        repoPaths.each {
            if(repositories.getProperty(it, BLACK_DUCK_SCAN_RESULT_PROPERTY_NAME)?.equals('FAILURE')){
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

        initializeConfiguration()

        logCronRun('blackDuckScan')

        Set<RepoPath> repoPaths = searchForRepoPaths()
        scanArtifactPaths(repoPaths)

        log.info('...completed blackDuckScan cron job.')
    }

    blackDuckAddPolicyStatus(cron: "0 0/1 * 1/1 * ?") {
        log.info('Starting blackDuckAddPolicyStatus cron job...')

        initializeConfiguration()

        logCronRun('blackDuckAddPolicyStatus')

        Set<RepoPath> repoPaths = searchForRepoPaths()
        HubServicesFactory hubServicesFactory = createHubServicesFactory()
        HubResponseService hubResponseService = hubServicesFactory.createHubResponseService()

        populatePolicyStatuses(hubResponseService, repoPaths)

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
    def reposToSearch = ARTIFACTORY_REPOS_TO_SEARCH.tokenize(',')
    def patternsToScan = ARTIFACT_NAME_PATTERNS_TO_SCAN.tokenize(',')

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
    if (StringUtils.isNotBlank(ARTIFACT_CUTOFF_DATE)) {
        try {
            long cutoffTime = getTimeFromString(ARTIFACT_CUTOFF_DATE)
            shouldCutoffPreventScanning = lastModifiedTime < cutoffTime
        } catch (Exception e) {
            log.error("The pattern: ${DATE_TIME_PATTERN} does not match the date string: ${ARTIFACT_CUTOFF_DATE}: ${e.message}")
            shouldCutoffPreventScanning = false
        }
    }

    if (shouldCutoffPreventScanning) {
        log.warn("${itemInfo.name} was not scanned because the cutoff was set and the artifact is too old")
        return false
    }

    String blackDuckScanTimeProperty = repositories.getProperty(repoPath, BLACK_DUCK_SCAN_TIME_PROPERTY_NAME)
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
            String timeString = DateTime.now().withZone(DateTimeZone.UTC).toString(DateTimeFormat.forPattern(DATE_TIME_PATTERN).withZoneUTC())
            repositories.setProperty(it, BLACK_DUCK_SCAN_TIME_PROPERTY_NAME, timeString)
            FileLayoutInfo fileLayoutInfo = getArtifactFromPath(it)
            ProjectVersionView projectVersionView = scanArtifact(it, it.name, fileLayoutInfo)
            writeScanProperties(it, projectVersionView)
        } catch (Exception e) {
            log.error("Please investigate the scan logs for details - the Black Duck Scan did not complete successfully on ${it.name}: ${e.message}", e)
            repositories.setProperty(it, BLACK_DUCK_SCAN_RESULT_PROPERTY_NAME, 'FAILURE')
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
        fileOutputStream = new FileOutputStream(new File(blackDuckDirectory, repoPath.name))
        fileOutputStream << inputStream
    } catch (Exception e) {
        log.error("There was an error getting ${repoPath.name}: ${e.message}")
    } finally {
        IOUtils.closeQuietly(inputStream)
        IOUtils.closeQuietly(fileOutputStream)
        resourceStream.close()
    }
    fileLayoutInfo
}

private ProjectVersionView scanArtifact(RepoPath repoPath, String fileName, FileLayoutInfo fileLayoutInfo){
    ProjectRequestBuilder projectRequestBuilder = new ProjectRequestBuilder()
    HubScanConfigBuilder hubScanConfigBuilder = new HubScanConfigBuilder()
    hubScanConfigBuilder.scanMemory = HUB_SCAN_MEMORY
    hubScanConfigBuilder.dryRun = HUB_SCAN_DRY_RUN
    hubScanConfigBuilder.toolsDir = cliDirectory
    hubScanConfigBuilder.workingDirectory = blackDuckDirectory
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
    HubServicesFactory hubServicesFactory = createHubServicesFactory()
    CLIDataService cliDataService = hubServicesFactory.createCLIDataService(HUB_TIMEOUT * 1000)
    PhoneHomeDataService phoneHomeDataService = hubServicesFactory.createPhoneHomeDataService()

    HubServerConfig hubServerConfig = createHubServerConfig()
    ProjectRequest projectRequest = projectRequestBuilder.build()
    String thirdPartyVersion = '???'
    String pluginVersion = '???'
    try {
        thirdPartyVersion = ctx?.versionProvider?.running?.versionName
        pluginVersion = new File('lib/version.txt')?.text
    } catch(Exception e) {
    }

    cliDataService.installAndRunControlledScan(hubServerConfig, hubScanConfig, projectRequest, false, ThirdPartyName.ARTIFACTORY, thirdPartyVersion, pluginVersion)
}

private void deletePathArtifact(String fileName){
    try {
        boolean deleteOk = new File(blackDuckDirectory, fileName).delete()
        log.info("Successfully deleted temporary ${fileName}: ${Boolean.toString(deleteOk)}")
    } catch (Exception e) {
        log.error("Exception deleting ${fileName}: ${e.message}")
    }
}

private void writeScanProperties(RepoPath repoPath, ProjectVersionView projectVersionView){
    HubServicesFactory hubServicesFactory = createHubServicesFactory()
    HubResponseService hubResponseService = hubServicesFactory.createHubResponseService()
    log.info("${repoPath.name} was successfully scanned by the BlackDuck CLI.")
    repositories.setProperty(repoPath, BLACK_DUCK_SCAN_RESULT_PROPERTY_NAME, 'SUCCESS')

    if (projectVersionView) {
        String projectVersionUrl = ''
        String projectVersionUIUrl = ''
        try {
            projectVersionUrl = hubResponseService.getHref(projectVersionView)
            if (StringUtils.isNotEmpty(projectVersionUrl)) {
                repositories.setProperty(repoPath, BLACK_DUCK_PROJECT_VERSION_URL_PROPERTY_NAME, projectVersionUrl)
                log.info("Added ${projectVersionUrl} to ${repoPath.name}")
            }
            projectVersionUIUrl = hubResponseService.getFirstLinkSafely(projectVersionView, 'components')
            if (StringUtils.isNotEmpty(projectVersionUIUrl)) {
                repositories.setProperty(repoPath, BLACK_DUCK_PROJECT_VERSION_UI_URL_PROPERTY_NAME, projectVersionUIUrl)
                log.info("Added ${projectVersionUIUrl} to ${repoPath.name}")
            }
        } catch (Exception e) {
            log.error("Exception getting code location url: ${e.message}")
        }
    } else {
        log.warn('No scan summaries were available for a successful scan. This is expected if this was a dry run, but otherwise there should be summaries.')
    }
}

private void populatePolicyStatuses(HubResponseService hubResponseService, Set<RepoPath> repoPaths) {
    boolean problemRetrievingPolicyStatus = false
    repoPaths.each {
        try {
            String projectVersionUrl = repositories.getProperty(it, BLACK_DUCK_PROJECT_VERSION_URL_PROPERTY_NAME)
            if (StringUtils.isNotBlank(projectVersionUrl)) {
                projectVersionUrl = updateUrlPropertyToCurrentHubServer(projectVersionUrl)
                repositories.setProperty(it, BLACK_DUCK_PROJECT_VERSION_URL_PROPERTY_NAME, projectVersionUrl)
                ProjectVersionView projectVersionView = hubResponseService.getItem(projectVersionUrl, ProjectVersionView.class)
                try{
                    String policyStatusUrl = hubResponseService.getFirstLink(projectVersionView, MetaService.POLICY_STATUS_LINK)
                    log.info("Looking up policy status: ${policyStatusUrl}")
                    VersionBomPolicyStatusView versionBomPolicyStatusView = hubResponseService.getItem(policyStatusUrl, VersionBomPolicyStatusView.class)
                    log.info("policy status json: ${versionBomPolicyStatusView.json}")
                    PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(versionBomPolicyStatusView)
                    repositories.setProperty(it, BLACK_DUCK_POLICY_STATUS_PROPERTY_NAME, policyStatusDescription.policyStatusMessage)
                    repositories.setProperty(it, BLACK_DUCK_OVERALL_POLICY_STATUS_PROPERTY_NAME, versionBomPolicyStatusView.overallStatus.toString())
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
    if (StringUtils.isNotBlank(ARTIFACT_CUTOFF_DATE)) {
        try {
            getTimeFromString(ARTIFACT_CUTOFF_DATE)
            cutoffMessage = 'The date cutoff is specified correctly.'
        } catch (Exception e) {
            cutoffMessage = "The pattern: ${DATE_TIME_PATTERN} does not match the date string: ${ARTIFACT_CUTOFF_DATE}: ${e.message}"
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
    repositories.deleteProperty(repoPath, BLACK_DUCK_SCAN_TIME_PROPERTY_NAME)
    repositories.deleteProperty(repoPath, BLACK_DUCK_SCAN_RESULT_PROPERTY_NAME)
    repositories.deleteProperty(repoPath, BLACK_DUCK_PROJECT_VERSION_URL_PROPERTY_NAME)
    repositories.deleteProperty(repoPath, BLACK_DUCK_PROJECT_VERSION_UI_URL_PROPERTY_NAME)
    repositories.deleteProperty(repoPath, BLACK_DUCK_POLICY_STATUS_PROPERTY_NAME)
    repositories.deleteProperty(repoPath, BLACK_DUCK_OVERALL_POLICY_STATUS_PROPERTY_NAME)
}

/**
 * If the hub server being used changes, the existing properties in artifactory could be inaccurate so we will update them when they differ from the hub url established in the properties file.
 */
private String updateUrlPropertyToCurrentHubServer(String urlProperty) {
    if (urlProperty.startsWith(HUB_URL)) {
        return urlProperty
    }

    //get the old hub url from the existing property
    URL urlFromProperty = new URL(urlProperty)
    String hubUrlFromProperty = urlFromProperty.protocol + '://' + urlFromProperty.host
    if (urlFromProperty.port > 0) {
        hubUrlFromProperty += ':' + Integer.toString(urlFromProperty.port)
    }
    String urlEndpoint = urlProperty.replace(hubUrlFromProperty, '')

    String updatedProperty = HUB_URL + urlEndpoint
    updatedProperty
}

private void logCronRun(String methodName) {
    if (logVerboseCronLog) {
        String timeString = DateTime.now().withZone(DateTimeZone.UTC).toString(DateTimeFormat.forPattern(DATE_TIME_PATTERN).withZoneUTC())
        def cronLogFile = new File(blackDuckDirectory, 'blackduck_cron_history')
        if (cronLogFile.length() > 10000) {
            cronLogFile.delete()
            cronLogFile.createNewFile()
        }
        cronLogFile << "${methodName}\t${timeString}${System.lineSeparator}"
    }
}

private List<String> getLatestCronLogItems() {
    def cronLogFile = new File(blackDuckDirectory, 'blackduck_cron_history')
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

private HubServerConfig createHubServerConfig() {
    HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder()
    hubServerConfigBuilder.setHubUrl(HUB_URL)
    hubServerConfigBuilder.setUsername(HUB_USERNAME)
    hubServerConfigBuilder.setPassword(HUB_PASSWORD)
    hubServerConfigBuilder.setTimeout(HUB_TIMEOUT)
    hubServerConfigBuilder.setProxyHost(HUB_PROXY_HOST)
    hubServerConfigBuilder.setProxyPort(HUB_PROXY_PORT)
    hubServerConfigBuilder.setProxyUsername(HUB_PROXY_USERNAME)
    hubServerConfigBuilder.setProxyPassword(HUB_PROXY_PASSWORD)
    hubServerConfigBuilder.setAlwaysTrustServerCertificate(HUB_ALWAYS_TRUST_CERTS)

    return hubServerConfigBuilder.build()
}

private HubServicesFactory createHubServicesFactory() {
    HubServerConfig hubServerConfig = createHubServerConfig()
    CredentialsRestConnection credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(log))
    new HubServicesFactory(credentialsRestConnection)
}

private void initializeConfiguration() {
    if (!initialized) {
        if (BLACK_DUCK_SCAN_BINARIES_DIRECTORY_PATH) {
            homeDir = ctx.artifactoryHome.homeDir
            blackDuckDirectory = new File(homeDir, BLACK_DUCK_SCAN_BINARIES_DIRECTORY_PATH)
        } else {
            etcDir = ctx.artifactoryHome.etcDir
            blackDuckDirectory = new File(etcDir, 'blackducksoftware')
        }
        cliDirectory = new File(blackDuckDirectory, 'cli')
        cliDirectory.mkdirs()

        File cronLogFile = new File(blackDuckDirectory, 'blackduck_cron_history')
        cronLogFile.createNewFile()

        initialized = true
    }
}

private String getNowString() {
    DateTime.now().withZone(DateTimeZone.UTC).toString(DateTimeFormat.forPattern(DATE_TIME_PATTERN).withZoneUTC())
}

private long getTimeFromString(String dateTimeString) {
    DateTime.parse(dateTimeString, DateTimeFormat.forPattern(DATE_TIME_PATTERN).withZoneUTC()).toDate().time
}
