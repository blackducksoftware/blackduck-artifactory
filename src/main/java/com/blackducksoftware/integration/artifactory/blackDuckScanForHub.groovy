import com.blackducksoftware.integration.hub.api.codelocation.CodeLocationRestService
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
        File etcDir = ctx.artifactoryHome.etcDir

        initializeConfiguration(etcDir)
    }
}

def initializeConfiguration(File etcDir) {
    File blackDuckDirectory = new File(etcDir, "plugins/blackducksoftware")
    File cliDirectory = new File(blackDuckDirectory, "cli")
    cliDirectory.mkdirs()

    Properties properties = loadProperties()
    HubServerConfig hubServerConfig = getHubServerConfig(properties)

    CredentialsRestConnection credentialsRestConnection = new CredentialsRestConnection(hubServerConfig)
    DataServicesFactory dataServicesFactory = new DataServicesFactory(credentialsRestConnection)

    String hubUrl = credentialsRestConnection.getBaseUrl();
    String hubVersion = dataServicesFactory.getHubVersionRestService().getHubVersion();
    installOrUpdateCli(cliDirectory, hubServerConfig, hubUrl, hubVersion)

    CodeLocationRestService codeLocationRestService = dataServicesFactory.getCodeLocationRestService()
    codeLocationRestService.getAllCodeLocations().each { log.warn "${it}" }
}

def loadProperties() {
    File propertiesFile = new File(ctx.artifactoryHome.etcDir, "plugins/blackduck.hub.properties")
    if (!propertiesFile.isFile()) {
        String message = "No profile properties file was found at ${propertiesFile.absolutePath}"
    }
    Properties properties = new Properties()
    properties.load(new FileReader(propertiesFile))
    properties
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

    CLIInstaller cliInstaller = new CLIInstaller(cliLocation, slf4jIntLogger)
    if (hubServerConfig.getProxyInfo().shouldUseProxyForUrl(hubServerConfig.getHubUrl())) {
        cliInstaller.setProxyHost(hubServerConfig.getProxyInfo().getHost());
        cliInstaller.setProxyPort(hubServerConfig.getProxyInfo().getPort());
        cliInstaller.setProxyUserName(hubServerConfig.getProxyInfo().getUsername());
        cliInstaller.setProxyPassword(hubServerConfig.getProxyInfo().getDecryptedPassword());
    }
    cliInstaller.performInstallation(intSlf4jLogger, hubUrl, hubVersion, localHostName)
}
