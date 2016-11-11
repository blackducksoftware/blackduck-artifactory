import org.artifactory.exception.CancelException

import com.google.gson.Gson
import com.google.gson.JsonParser

import com.blackducksoftware.integration.util.CIEnvironmentVariables
import com.blackducksoftware.integration.hub.cli.CLILocation
import com.blackducksoftware.integration.hub.cli.CLIInstaller
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection
import com.blackducksoftware.integration.hub.api.codelocation.CodeLocationRestService
import com.blackducksoftware.integration.hub.api.codelocation.CodeLocationItem

executions {
    scanForHub(version:"0.0.1", description:"what the execution does", httpMethod: "GET") { params ->
        log.warn("Hello from the Hub")

        File blackDuckDirectory = new File(ctx.artifactoryHome.etcDir, "plugins/blackducksoftware")
        File cliDirectory = new File(blackDuckDirectory, "cli")
        cliDirectory.mkdirs()

        Properties properties = loadProperties()

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
        CredentialsRestConnection credentialsRestConnection = new CredentialsRestConnection(hubServerConfig)
        DataServicesFactory dataServicesFactory = new DataServicesFactory(credentialsRestConnection)

        CodeLocationRestService codeLocationRestService = dataServicesFactory.getCodeLocationRestService()
        codeLocationRestService.getAllCodeLocations().each {
            log.warn "${it}"
        }
    }
}

def installOrUpdateCli(File cliDirectory) {
    CIEnvironmentVariables ciEnvironmentVariables = new CIEnvironmentVariables()
    ciEnvironmentVariables.putAll(System.getenv())
    CLiLocation cliLocation = new CLILocation(cliDirectory)
    IntSlf4jLogger intSlf4jLogger = new Slf4jLogger(log)
    CLIInstaller cliInstaller = new CLIInstaller(cliLocation, intSlf4jLogger)
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
