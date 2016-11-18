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
import com.blackducksoftware.integration.hub.SimpleScanExecutor
import com.blackducksoftware.integration.hub.ScanExecutor.Result
import com.blackducksoftware.integration.hub.api.HubVersionRestService
import com.blackducksoftware.integration.hub.api.codelocation.CodeLocationItem
import com.blackducksoftware.integration.hub.api.codelocation.CodeLocationRestService
import com.blackducksoftware.integration.hub.api.scan.ScanSummaryItem
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder
import com.blackducksoftware.integration.hub.cli.CLIInstaller
import com.blackducksoftware.integration.hub.cli.CLILocation
import com.blackducksoftware.integration.hub.dataservices.DataServicesFactory
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection
import com.blackducksoftware.integration.log.Slf4jIntLogger
import com.blackducksoftware.integration.util.CIEnvironmentVariables
import com.google.gson.Gson

@Field final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS"
@Field final String BLACK_DUCK_SCAN_TIME_PROPERTY_NAME = "blackDuckScanTime"
@Field final String BLACK_DUCK_SCAN_RESULT_PROPERTY_NAME = "blackDuckScanResult"
@Field final String BLACK_DUCK_SCAN_CODE_LOCATION_URL_PROPERTY_NAME = "blackDuckScanCodeLocationUrl"
@Field final String BLACK_DUCK_PROJECT_VERSION_URL_PROPERTY_NAME = "blackDuckProjectVersionUrl"

executions {
    scanForHub(httpMethod: "GET") { params ->
        log.info("Starting scanForHub REST request...")

        initializeConfiguration()
        Set<RepoPath> repoPaths = searchForRepoPaths()
        scanArtifactPaths(repoPaths)

        log.info("...completed scanForHub REST request.")
    }
}

jobs {
    scanForHub(cron: "0 0/3 * 1/1 * ?") {
        log.info("Starting scanForHub cron job...")

        initializeConfiguration()
        Set<RepoPath> repoPaths = searchForRepoPaths()
        scanArtifactPaths(repoPaths)

        log.info("...completed scanForHub cron job.")
    }

    addProjectVersionUrl(cron: "0 0/5 * 1/1 * ?") {
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
def Properties properties
def HubServerConfig hubServerConfig
def Gson gson
def CodeLocationRestService codeLocationRestService
def CIEnvironmentVariables ciEnvironmentVariables
def CLILocation cliLocation
def HubSupportHelper hubSupportHelper
def Set<String> reposToSearch
def Set<String> artifactPatternsToFind

def searchForRepoPaths() {
    def repoPaths = []
    artifactPatternsToFind.each {
        repoPaths.addAll(searches.artifactsByName(it, reposToSearch.toArray(new String[reposToSearch.size()])))
    }

    repoPaths = repoPaths.findAll {shouldRepoPathBeScannedNow(it)}

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

    String workingDirectoryPath = blackDuckDirectory.absolutePath
    int scanMemory = NumberUtils.toInt(properties.getProperty("hub.scan.memory"), 4096)
    boolean verboseRun = Boolean.parseBoolean(properties.getProperty("hub.scan.verbose"))
    boolean dryRun = Boolean.parseBoolean(properties.getProperty("hub.scan.dry.run"))
    filenamesToLayout.each { key, value ->
        try {
            String project = value.module
            String version = value.baseRevision
            def scanFile = new File(workingDirectoryPath, key)
            def scanTargetPaths = [scanFile.absolutePath]

            SimpleScanExecutor simpleScanExecutor = new SimpleScanExecutor(slf4jIntLogger, gson, hubServerConfig, hubSupportHelper, ciEnvironmentVariables, cliLocation, scanMemory, verboseRun, dryRun, project, version, scanTargetPaths, workingDirectoryPath)
            Result result = simpleScanExecutor.setupAndRunScan()
            if (Result.SUCCESS == result) {
                log.info("${key} was successfully scanned by the BlackDuck CLI.")
                String timeString = DateTime.now().withZone(DateTimeZone.UTC).toString(DateTimeFormat.forPattern(DATE_TIME_PATTERN).withZoneUTC())
                repositories.setProperty(filenamesToRepoPath[key], BLACK_DUCK_SCAN_TIME_PROPERTY_NAME, timeString)
                List<ScanSummaryItem> scanSummaryItems = simpleScanExecutor.scanSummaryItems
                if (null != scanSummaryItems && scanSummaryItems.size() == 1) {
                    try {
                        String codeLocationUrl = scanSummaryItems.get(0).getLink(ScanSummaryItem.CODE_LOCATION_LINK)
                        repositories.setProperty(filenamesToRepoPath[key], BLACK_DUCK_SCAN_CODE_LOCATION_URL_PROPERTY_NAME, codeLocationUrl)
                    } catch (Exception e) {
                        log.error("Exception getting code location url: ${e.message}")
                    }
                }
            } else {
                log.error("The BlackDuck Scan did not complete successfully. Please investigate the scan logs for details.")
            }
            repositories.setProperty(filenamesToRepoPath[key], BLACK_DUCK_SCAN_RESULT_PROPERTY_NAME, result.toString())
        } catch (Exception e) {
            log.error("The BlackDuck Scan did not complete successfully: ${e.message}")
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
                String versionId = mappedProjectVersionUrl.substring(mappedProjectVersionUrl.indexOf("/versions/") + "/versions/".length());
                String uiUrl = hubUrl + "/#versions/id:"+ versionId + "/view:bom";
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
    File cliDirectory = new File(blackDuckDirectory, "cli")
    cliDirectory.mkdirs()

    File propertiesFile = new File(etcDir, "plugins/blackduck.hub.properties")
    if (!propertiesFile.isFile()) {
        String message = "No profile properties file was found at ${propertiesFile.absolutePath}"
        log.error(message)
        throw new CancelException(message, 500)
    }
    properties = new Properties()
    properties.load(new FileReader(propertiesFile))

    reposToSearch = properties.getProperty("artifactory.repos.to.search").tokenize(",").toSet()
    artifactPatternsToFind = properties.getProperty("artifact.name.patterns").tokenize(",").toSet()

    hubServerConfig = getHubServerConfig(properties)

    CredentialsRestConnection credentialsRestConnection = new CredentialsRestConnection(hubServerConfig)
    DataServicesFactory dataServicesFactory = new DataServicesFactory(credentialsRestConnection)
    gson = dataServicesFactory.getGson()
    HubVersionRestService hubVersionRestService = dataServicesFactory.hubVersionRestService
    codeLocationRestService = dataServicesFactory.getCodeLocationRestService()

    hubSupportHelper = new HubSupportHelper()
    hubSupportHelper.checkHubSupport(hubVersionRestService, slf4jIntLogger)

    String hubUrl = credentialsRestConnection.baseUrl
    String hubVersion = hubVersionRestService.hubVersion
    installOrUpdateCli(cliDirectory, hubServerConfig, hubUrl, hubVersion)
}

def getHubServerConfig(Properties properties) {
    String hubUrl = properties.get "hub.url"
    String hubUsername = properties.get "hub.username"
    String hubPassword = properties.get "hub.password"
    String hubTimeout = properties.get "hub.timeout"
    String hubProxyHost = properties.get "hub.proxy.host"
    String hubProxyPort = properties.get "hub.proxy.port"
    String hubNoProxyHosts = properties.get "hub.ignored.proxy.hosts"
    String hubProxyUsername = properties.get "hub.proxy.username"
    String hubProxyPassword = properties.get "hub.proxy.password"

    HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder()
    hubServerConfigBuilder.setHubUrl(hubUrl)
    hubServerConfigBuilder.setUsername(hubUsername)
    hubServerConfigBuilder.setPassword(hubPassword)
    hubServerConfigBuilder.setTimeout(hubTimeout)
    hubServerConfigBuilder.setProxyHost(hubProxyHost)
    hubServerConfigBuilder.setProxyPort(hubProxyPort)
    hubServerConfigBuilder.setIgnoredProxyHosts(hubNoProxyHosts)
    hubServerConfigBuilder.setProxyUsername(hubProxyUsername)
    hubServerConfigBuilder.setProxyPassword(hubProxyPassword)

    HubServerConfig hubServerConfig = hubServerConfigBuilder.build()
    hubServerConfig
}

def installOrUpdateCli(File cliDirectory, HubServerConfig hubServerConfig, String hubUrl, String hubVersion) {
    ciEnvironmentVariables = new CIEnvironmentVariables()
    ciEnvironmentVariables.putAll(System.getenv())

    cliLocation = new CLILocation(cliDirectory)

    CLIInstaller cliInstaller = new CLIInstaller(cliLocation, ciEnvironmentVariables)
    if (hubServerConfig.proxyInfo.shouldUseProxyForUrl(hubServerConfig.hubUrl)) {
        cliInstaller.setProxyHost(hubServerConfig.proxyInfo.host)
        cliInstaller.setProxyPort(hubServerConfig.proxyInfo.port)
        cliInstaller.setProxyUserName(hubServerConfig.proxyInfo.username)
        cliInstaller.setProxyPassword(hubServerConfig.proxyInfo.decryptedPassword)
    }

    def localHostName = InetAddress.localHost.hostName
    cliInstaller.performInstallation(slf4jIntLogger, hubUrl, hubVersion, localHostName)
}
