import groovy.transform.Field

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.artifactory.exception.CancelException
import org.artifactory.fs.FileLayoutInfo
import org.artifactory.repo.RepoPath
import org.artifactory.resource.ResourceStreamHandle
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

import com.blackducksoftware.integration.hub.HubSupportHelper
import com.blackducksoftware.integration.hub.ScanExecutor.Result
import com.blackducksoftware.integration.hub.api.HubServicesFactory
import com.blackducksoftware.integration.hub.api.HubVersionRestService
import com.blackducksoftware.integration.hub.api.codelocation.CodeLocationItem
import com.blackducksoftware.integration.hub.api.codelocation.CodeLocationRestService
import com.blackducksoftware.integration.hub.api.scan.ScanSummaryItem
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder
import com.blackducksoftware.integration.hub.cli.CLIDownloadService
import com.blackducksoftware.integration.hub.cli.SimpleScanService
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection
import com.blackducksoftware.integration.log.Slf4jIntLogger
import com.blackducksoftware.integration.util.CIEnvironmentVariables

@Field final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS"
@Field final String BLACK_DUCK_SCAN_TIME_PROPERTY_NAME = "blackDuckScanTime"
@Field final String BLACK_DUCK_SCAN_RESULT_PROPERTY_NAME = "blackDuckScanResult"
@Field final String BLACK_DUCK_SCAN_CODE_LOCATION_URL_PROPERTY_NAME = "blackDuckScanCodeLocationUrl"
@Field final String BLACK_DUCK_PROJECT_VERSION_URL_PROPERTY_NAME = "blackDuckProjectVersionUrl"

executions {
    /**
     * This will search your artifactory repositories for the filename patterns designated in the properties file.
     * For example, if the properties are set:
     *
     * artifactory.repos.to.search=my-releases
     * artifact.name.patterns=*.war
     *
     * then this REST call will search 'my-releases' for all .war (web archive) files, scan them, and publish the BOM to the provided Hub server.
     *
     * The scanning process will add several properties to your files in artifactory. Namely:
     *
     * blackDuckScanResult - SUCCESS or FAILURE, depending on the result of the scan
     * blackDuckScanTime - the last time a SUCCESS scan was completed
     * blackDuckScanCodeLocationUrl - the url for the code location created in the Hub
     *
     * The same functionality is provided via the scanForHub cron job to enable scheduled scans to run consistently.
     */
    scanForHub(httpMethod: "GET") { params ->
        log.info("Starting scanForHub REST request...")

        initializeConfiguration()
        Set<RepoPath> repoPaths = searchForRepoPaths()
        scanArtifactPaths(repoPaths)

        log.info("...completed scanForHub REST request.")
    }

    clearAllScanResults() { params ->
        log.info("Starting clearAllScanResults REST request...")
        initializeConfiguration()

        log.info("...completed clearAllScanResults REST request.")
    }
}

jobs {
    /**
     * This will search your artifactory repositories for the filename patterns designated in the properties file.
     * For example, if the properties are set:
     *
     * artifactory.repos.to.search=my-releases
     * artifact.name.patterns=*.war
     *
     * then this cron job will search 'my-releases' for all .war (web archive) files, scan them, and publish the BOM to the provided Hub server.
     *
     * The scanning process will add several properties to your files in artifactory. Namely:
     *
     * blackDuckScanResult - SUCCESS or FAILURE, depending on the result of the scan
     * blackDuckScanTime - the last time a SUCCESS scan was completed
     * blackDuckScanCodeLocationUrl - the url for the code location created in the Hub
     *
     * The same functionality is provided via the scanForHub execution to enable a one-time scan triggered via a REST call.
     */
    scanForHub(cron: "0 0/1 * 1/1 * ?") {
        log.info("Starting scanForHub cron job...")

        initializeConfiguration()
        Set<RepoPath> repoPaths = searchForRepoPaths()
        scanArtifactPaths(repoPaths)

        log.info("...completed scanForHub cron job.")
    }

    /**
     * For those artifacts that are scanned that have a blackDuckScanCodeLocationUrl, this cron job
     * will update that property to the blackDuckProjectVersionUrl property, which will link directly to your BOM in the Hub.
     */
    addProjectVersionUrl(cron: "0 0/1 * 1/1 * ?") {
        log.info("Starting addProjectVersionUrl cron job...")

        initializeConfiguration()
        Set<RepoPath> repoPaths = searchForRepoPaths()
        populateProjectVersionUrls(repoPaths)

        log.info("...completed addProjectVersionUrl cron job.")
    }
}

def Slf4jIntLogger slf4jIntLogger
def File etcDir
def File blackDuckDirectory
def File cliDirectory
def Properties properties
def CIEnvironmentVariables ciEnvironmentVariables
def HubServerConfig hubServerConfig
def HubSupportHelper hubSupportHelper
def CodeLocationRestService codeLocationRestService
def CLIDownloadService cliDownloadService
def SimpleScanService simpleScanService
def Set<String> reposToSearch
def Set<String> artifactPatternsToFind

def searchForRepoPaths() {
    def repoPaths = []
    artifactPatternsToFind.each {
        repoPaths.addAll(searches.artifactsByName(it, reposToSearch.toArray(new String[reposToSearch.size()])))
    }

    repoPaths.toSet()
}

/**
 * If artifact's last modified time is newer than the scan time, or we have no record of the scan time, we should scan now.
 */
def shouldRepoPathBeScannedNow(RepoPath repoPath) {
    String blackDuckScanTimeProperty = repositories.getProperty(repoPath, BLACK_DUCK_SCAN_TIME_PROPERTY_NAME)
    if (StringUtils.isBlank(blackDuckScanTimeProperty)) {
        return true
    }

    def itemInfo = repositories.getItemInfo(repoPath)
    long lastModifiedTime = itemInfo.lastModified
    try {
        long blackDuckScanTime = DateTime.parse(blackDuckScanTimeProperty, DateTimeFormat.forPattern(DATE_TIME_PATTERN).withZoneUTC()).toDate().time
        return lastModifiedTime >= blackDuckScanTime
    } catch (Exception e) {
        //if the date format changes, the old format won't parse, so just cleanup the property by returning true and re-scanning
        log.error("Exception parsing the scan date (most likely the format changed): ${e.message}")
    }

    return true
}

def scanArtifactPaths(Set<RepoPath> repoPaths) {
    def filenamesToLayout = [:]
    def filenamesToRepoPath = [:]

    repoPaths = repoPaths.findAll {shouldRepoPathBeScannedNow(it)}
    repoPaths.each {
        ResourceStreamHandle resourceStream = repositories.getContent(it)
        FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(it)
        def inputStream
        def fileOutputStream
        try {
            inputStream = resourceStream.inputStream
            fileOutputStream = new FileOutputStream(new File(blackDuckDirectory, it.name))
            fileOutputStream << inputStream
            filenamesToLayout.put(it.name, fileLayoutInfo)
            filenamesToRepoPath.put(it.name, it)
        } catch (Exception e) {
            log.error("There was an error getting ${it.name}: ${e.message}")
        } finally {
            IOUtils.closeQuietly(inputStream)
            IOUtils.closeQuietly(fileOutputStream)
        }
    }

    String workingDirectoryPath = blackDuckDirectory.canonicalPath
    int scanMemory = NumberUtils.toInt(properties.getProperty("hub.scan.memory"), 4096)
    boolean verboseRun = Boolean.parseBoolean(properties.getProperty("hub.scan.verbose"))
    boolean dryRun = Boolean.parseBoolean(properties.getProperty("hub.scan.dry.run"))
    filenamesToLayout.each { key, value ->
        try {
            String project = value.module
            String version = value.baseRevision
            def scanFile = new File(workingDirectoryPath, key)
            def scanTargetPaths = [scanFile.canonicalPath]

            Result result = simpleScanService.setupAndExecuteScan(hubServerConfig, hubSupportHelper, ciEnvironmentVariables, cliDirectory, scanMemory, verboseRun, dryRun, project,version, scanTargetPaths, workingDirectoryPath)

            if (Result.SUCCESS == result) {
                log.info("${key} was successfully scanned by the BlackDuck CLI.")
                String timeString = DateTime.now().withZone(DateTimeZone.UTC).toString(DateTimeFormat.forPattern(DATE_TIME_PATTERN).withZoneUTC())
                repositories.setProperty(filenamesToRepoPath[key], BLACK_DUCK_SCAN_TIME_PROPERTY_NAME, timeString)
                List<ScanSummaryItem> scanSummaryItems = simpleScanExecutor.scanSummaryItems
                if (null != scanSummaryItems && scanSummaryItems.size() == 1) {
                    try {
                        String codeLocationUrl = scanSummaryItems.get(0).getLink(ScanSummaryItem.CODE_LOCATION_LINK)
                        repositories.setProperty(filenamesToRepoPath[key], BLACK_DUCK_SCAN_CODE_LOCATION_URL_PROPERTY_NAME, codeLocationUrl)
                        repositories.deleteProperty(it, BLACK_DUCK_PROJECT_VERSION_URL_PROPERTY_NAME)
                    } catch (Exception e) {
                        log.error("Exception getting code location url: ${e.message}")
                    }
                }
            } else {
                log.error("The BlackDuck Scan did not complete successfully. Please investigate the scan logs for details.")
            }
            repositories.setProperty(filenamesToRepoPath[key], BLACK_DUCK_SCAN_RESULT_PROPERTY_NAME, result.toString())
        } catch (Exception e) {
            log.error("The BlackDuck Scan did not complete successfully: ${e.message}", e)
            repositories.setProperty(filenamesToRepoPath[key], BLACK_DUCK_SCAN_RESULT_PROPERTY_NAME, Result.FAILURE.toString())
        }

        try {
            boolean deleteOk = new File(blackDuckDirectory, key).delete()
            log.info("Successfully deleted temporary ${key}: ${Boolean.toString(deleteOk)}")
        } catch (Exception e) {
            log.error("Exception deleting ${key}: ${e.message}")
        }
    }
}

def populateProjectVersionUrls(Set<RepoPath> repoPaths) {
    repoPaths.each {
        String codeLocationUrl = repositories.getProperty(it, BLACK_DUCK_SCAN_CODE_LOCATION_URL_PROPERTY_NAME)
        if (StringUtils.isNotBlank(codeLocationUrl)) {
            CodeLocationItem codeLocationItem = codeLocationRestService.getItem(codeLocationUrl)
            String mappedProjectVersionUrl = codeLocationItem.mappedProjectVersion
            if (StringUtils.isNotBlank(mappedProjectVersionUrl)) {
                String hubUrl = hubServerConfig.getHubUrl().toString()
                String versionId = mappedProjectVersionUrl.substring(mappedProjectVersionUrl.indexOf("/versions/") + "/versions/".length())
                String uiUrl = hubUrl + "/#versions/id:"+ versionId + "/view:bom"
                repositories.setProperty(it, BLACK_DUCK_PROJECT_VERSION_URL_PROPERTY_NAME, uiUrl)
                repositories.deleteProperty(it, BLACK_DUCK_SCAN_CODE_LOCATION_URL_PROPERTY_NAME)
                log.info("Added ${mappedProjectVersionUrl} to ${it.name}")
            }
        }
    }
}

def initializeConfiguration() {
    slf4jIntLogger = new Slf4jIntLogger(log)

    etcDir = ctx.artifactoryHome.etcDir
    blackDuckDirectory = new File(etcDir, "plugins/blackducksoftware")
    cliDirectory = new File(blackDuckDirectory, "cli")
    cliDirectory.mkdirs()

    File propertiesFile = new File(etcDir, "plugins/blackduck.hub.properties")
    if (!propertiesFile.isFile()) {
        String message = "No profile properties file was found at ${propertiesFile.canonicalPath}"
        log.error(message)
        throw new CancelException(message, 500)
    }
    properties = new Properties()
    properties.load(new FileReader(propertiesFile))

    reposToSearch = properties.getProperty("artifactory.repos.to.search").tokenize(",").toSet()
    artifactPatternsToFind = properties.getProperty("artifact.name.patterns").tokenize(",").toSet()

    HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder()
    hubServerConfigBuilder.setFromProperties(properties)
    hubServerConfig = hubServerConfigBuilder.build()

    CredentialsRestConnection credentialsRestConnection = new CredentialsRestConnection(hubServerConfig)
    HubServicesFactory hubServicesFactory = new HubServicesFactory(credentialsRestConnection)

    HubVersionRestService hubVersionRestService = hubServicesFactory.createHubVersionRestService()
    codeLocationRestService = hubServicesFactory.createCodeLocationRestService()
    cliDownloadService = hubServicesFactory.createCliDownloadService(slf4jIntLogger)
    simpleScanService = hubServicesFactory.createSimpleScanService(slf4jIntLogger)

    hubSupportHelper = new HubSupportHelper()
    hubSupportHelper.checkHubSupport(hubVersionRestService, slf4jIntLogger)

    String hubUrl = credentialsRestConnection.baseUrl
    String hubVersion = hubVersionRestService.hubVersion
    ciEnvironmentVariables = new CIEnvironmentVariables()
    ciEnvironmentVariables.putAll(System.getenv())

    def localHostName = InetAddress.localHost.hostName
    cliDownloadService.performInstallation(hubServerConfig.getProxyInfo(), cliDirectory, ciEnvironmentVariables, hubUrl, hubVersion, localHostName)
}
