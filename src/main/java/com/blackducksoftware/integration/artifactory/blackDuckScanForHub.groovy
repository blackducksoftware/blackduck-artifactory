import org.apache.commons.io.IOUtils
import org.artifactory.exception.CancelException
import org.artifactory.repo.RepoPath
import org.artifactory.resource.ResourceStreamHandle

import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder
import com.blackducksoftware.integration.hub.cli.CLIInstaller
import com.blackducksoftware.integration.hub.cli.CLILocation
import com.blackducksoftware.integration.hub.dataservices.DataServicesFactory
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection
import com.blackducksoftware.integration.log.Slf4jIntLogger
import com.blackducksoftware.integration.util.CIEnvironmentVariables

executions {
    scanForHub(version:"0.0.1", description:"what the execution does", httpMethod: "GET") { params ->
        log.warn("Hello from the Hub")

        initializeConfiguration()
        List<RepoPath> repoPaths = searchForRepoPaths()
        scanArtifactPaths(repoPaths)
    }
}

def File etcDir
def File blackDuckDirectory
def Properties properties
def List<String> reposToSearch
def List<String> artifactPatternsToFind

def searchForRepoPaths() {
    def repoPaths = []
    artifactPatternsToFind.each {
        repoPaths.addAll(searches.artifactsByName(it, reposToSearch.toArray(new String[reposToSearch.size()])))
    }

    repoPaths.each {
        log.warn(it.getName())
        log.warn(it.getPath())
        log.warn(it.getRepoKey())
        log.warn(Boolean.toString(it.isFile()))
    }
    repoPaths
}

def scanArtifactPaths(List<RepoPath> repoPaths) {
    repoPaths.each {
        ResourceStreamHandle resourceStream = repositories.getContent(it)
        def inputStream
        def fileOutputStream
        try {
            inputStream = resourceStream.getInputStream()
            fileOutputStream = new FileOutputStream(new File(blackDuckDirectory, it.getName()))
            fileOutputStream << inputStream
        } finally {
            IOUtils.closeQuietly(inputStream)
            IOUtils.closeQuietly(fileOutputStream)
        }
    }
}

def initializeConfiguration() {
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

    reposToSearch = properties.getProperty("artifactory.repos.to.search").tokenize(",")
    artifactPatternsToFind = properties.getProperty("artifact.name.patterns").tokenize(",")
    log.warn("properties")
    reposToSearch.each {
        log.warn("${it}")
    }
    artifactPatternsToFind.each {
        log.warn("${it}")
    }
    log.warn("done with properties")

    HubServerConfig hubServerConfig = getHubServerConfig(properties)

    CredentialsRestConnection credentialsRestConnection = new CredentialsRestConnection(hubServerConfig)
    DataServicesFactory dataServicesFactory = new DataServicesFactory(credentialsRestConnection)

    String hubUrl = credentialsRestConnection.getBaseUrl()
    String hubVersion = dataServicesFactory.getHubVersionRestService().getHubVersion()
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
    def localHostName = InetAddress.getLocalHost().getHostName()

    CIEnvironmentVariables ciEnvironmentVariables = new CIEnvironmentVariables()
    ciEnvironmentVariables.putAll(System.getenv())

    CLILocation cliLocation = new CLILocation(cliDirectory)

    Slf4jIntLogger slf4jIntLogger = new Slf4jIntLogger(log)

    CLIInstaller cliInstaller = new CLIInstaller(cliLocation, ciEnvironmentVariables)
    if (hubServerConfig.getProxyInfo().shouldUseProxyForUrl(hubServerConfig.getHubUrl())) {
        cliInstaller.setProxyHost(hubServerConfig.getProxyInfo().getHost())
        cliInstaller.setProxyPort(hubServerConfig.getProxyInfo().getPort())
        cliInstaller.setProxyUserName(hubServerConfig.getProxyInfo().getUsername())
        cliInstaller.setProxyPassword(hubServerConfig.getProxyInfo().getDecryptedPassword())
    }
    cliInstaller.performInstallation(slf4jIntLogger, hubUrl, hubVersion, localHostName)
}
