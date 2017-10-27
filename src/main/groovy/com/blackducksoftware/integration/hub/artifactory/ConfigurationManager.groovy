package com.blackducksoftware.integration.hub.artifactory

import javax.annotation.PostConstruct

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.DefaultPropertiesPersister

import com.blackducksoftware.integration.hub.artifactory.inspect.HubClient
import com.blackducksoftware.integration.hub.artifactory.inspect.HubProjectDetails

@Component
class ConfigurationManager {
    @Autowired
    HubClient hubClient

    @Autowired
    RestTemplateContainer restTemplateContainer

    @Autowired
    ArtifactoryRestClient artifactoryRestClient

    @Autowired
    ConfigurationProperties configurationProperties

    @Autowired
    HubProjectDetails hubProjectDetails

    File userSpecifiedProperties

    @PostConstruct
    void init() {
        File configDirectory = new File (configurationProperties.currentUserDirectory, 'config')
        if (!configDirectory.exists()) {
            configDirectory.mkdirs()
        }
        userSpecifiedProperties = new File (configDirectory, 'application.properties')
        if (!userSpecifiedProperties.exists()) {
            persistValues()
        }
    }

    boolean needsBaseConfigUpdate() {
        StringUtils.isBlank(configurationProperties.hubUrl) || StringUtils.isBlank(configurationProperties.hubUsername) || StringUtils.isBlank(configurationProperties.hubPassword) || StringUtils.isBlank(configurationProperties.hubArtifactoryWorkingDirectoryPath) || StringUtils.isBlank(configurationProperties.hubAlwaysTrustCerts)
    }

    boolean needsArtifactoryInspectUpdate() {
        StringUtils.isBlank(configurationProperties.artifactoryUrl) || StringUtils.isBlank(configurationProperties.artifactoryUsername) || StringUtils.isBlank(configurationProperties.artifactoryPassword) || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectRepoKey)
    }

    boolean needsArtifactoryScanUpdate() {
        StringUtils.isBlank(configurationProperties.hubArtifactoryScanReposToSearch) || StringUtils.isBlank(configurationProperties.hubArtifactoryScanNamePatterns)
    }

    void updateBaseConfigValues(Console console, PrintStream out) {
        out.println('Updating Config - just hit enter to make no change to a value:')

        configurationProperties.hubUrl = setValueFromInput(console, out, 'Hub Server Url', configurationProperties.hubUrl)
        configurationProperties.hubUsername = setValueFromInput(console, out, 'Hub Server Username', configurationProperties.hubUsername)
        configurationProperties.hubPassword = setPasswordFromInput(console, out, 'Hub Server Password', configurationProperties.hubPassword)
        configurationProperties.hubTimeout = setValueFromInput(console, out, 'Hub Server Timeout', configurationProperties.hubTimeout)
        configurationProperties.hubAlwaysTrustCerts = setValueFromInput(console, out, 'Always Trust Server Certificates', configurationProperties.hubAlwaysTrustCerts)
        configurationProperties.hubArtifactoryWorkingDirectoryPath = setValueFromInput(console, out, 'Local Working Directory', configurationProperties.hubArtifactoryWorkingDirectoryPath)
        persistValues()

        boolean ok = false
        try {
            hubClient.testHubConnection()
            out.println 'Your Hub configuration is valid and a successful connection to the Hub was established.'
            ok = true
        } catch (Exception e) {
            out.println("Your Hub configuration is not valid: ${e.message}")
        }

        if (!ok) {
            out.println('You may need to manually edit the \'config/application.properties\' file to provide proxy details. If you wish to re-enter the base configuration, enter \'y\', otherwise, just press <enter> to continue.')
            String userValue = StringUtils.trimToEmpty(console.readLine())
            if ('y' == userValue) {
                updateBaseConfigValues(console, out)
            }
        }
    }

    void updateArtifactoryInspectValues(Console console, PrintStream out) {
        configurationProperties.artifactoryUrl = setValueFromInput(console, out, 'Artifactory Url', configurationProperties.artifactoryUrl)
        configurationProperties.artifactoryUsername = setValueFromInput(console, out, 'Artifactory Username', configurationProperties.artifactoryUsername)
        configurationProperties.artifactoryPassword = setPasswordFromInput(console, out, 'Artifactory Password', configurationProperties.artifactoryPassword)
        configurationProperties.hubArtifactoryInspectRepoKey = setValueFromInput(console, out, 'Artifactory Repository To Inspect', configurationProperties.hubArtifactoryInspectRepoKey)
        configurationProperties.hubArtifactoryInspectSkipBomCalculation = setValueFromInput(console, out, 'Skip Post-Inspection BOM Calculation', configurationProperties.hubArtifactoryInspectSkipBomCalculation)

        out.println('')
        out.println("If no value is supplied for the Hub Artifactory Project Name, the repository name, ${configurationProperties.hubArtifactoryInspectRepoKey}, will be used.")
        configurationProperties.hubArtifactoryProjectName = setValueFromInput(console, out, 'Hub Artifactory Project Name', configurationProperties.hubArtifactoryProjectName)

        out.println('')
        out.println('If no value is supplied for the Hub Artifactory Project Version Name, today\'s date will be used.')
        configurationProperties.hubArtifactoryProjectVersionName = setValueFromInput(console, out, 'Hub Artifactory Project Version Name', configurationProperties.hubArtifactoryProjectVersionName)

        restTemplateContainer.init()
        persistValues()

        boolean ok = false
        try {
            String response = artifactoryRestClient.checkSystem()
            if ('OK' == response) {
                out.println('Your Artifactory configuration is valid and a successful connection to the Artifactory server was established.')
                ok = true
            } else {
                out.println("A successful connection could not be established to the Artifactory server. The response was: ${response}")
            }
        } catch (Exception e) {
            out.println("A successful connection could not be established to the Artifactory server: ${e.message}")
        }

        if (ok) {
            ok = false
            try {
                def jsonResponse = artifactoryRestClient.getInfoForPath(configurationProperties.hubArtifactoryInspectRepoKey, "")
                if (jsonResponse != null && jsonResponse.children != null && jsonResponse.children.size() > 0) {
                    ok = true
                } else {
                    out.println("Could not get information for the ${configurationProperties.hubArtifactoryInspectRepoKey} repo. The response was: ${jsonResponse}")
                }
            } catch (Exception e) {
                out.println("Could not get information for the ${configurationProperties.hubArtifactoryInspectRepoKey} repo: ${e.message}")
            }
        }

        if (!ok) {
            out.println('You may need to manually edit the \'config/application.properties\' file but if you wish to re-enter the Artifactory inspect configuration, enter \'y\', otherwise, just press <enter> to continue.')
            String userValue = StringUtils.trimToEmpty(console.readLine())
            if ('y' == userValue) {
                updateArtifactoryInspectValues(console, out)
            }
        }
    }

    void updateArtifactoryScanValues(Console console, PrintStream out) {
        configurationProperties.hubArtifactoryScanBinariesDirectoryPath = setValueFromInput(console, out, 'Plugin Scan Binaries Directory', configurationProperties.hubArtifactoryScanBinariesDirectoryPath)
        String reposToSearch = configurationProperties.hubArtifactoryScanReposToSearch
        def repositoryNames = reposToSearch ? reposToSearch.tokenize(',') : []
        out.println("The current set of repositories to search is ${reposToSearch}. You will be prompted to add new repositories. If you would first like to clear the currently configured repositories, type 'clear'.")
        String userValue = StringUtils.trimToEmpty(console.readLine())
        if ('clear' == userValue) {
            repositoryNames = []
        }

        out.println('Enter Artifactory repositories to search for artifacts, one at a time. If you are finished, just press <enter>.')
        out.print "Enter repository name (current repositories=${repositoryNames.join(',')}): "
        userValue = StringUtils.trimToEmpty(console.readLine())
        while (StringUtils.isNotBlank(userValue)) {
            repositoryNames.add(userValue)
            out.print 'Enter repository name: '
            userValue = StringUtils.trimToEmpty(console.readLine())
        }

        String namePatternsToScan = configurationProperties.hubArtifactoryScanNamePatterns
        def namePatterns = namePatternsToScan ? namePatternsToScan.tokenize(',') : []
        out.println("The current set of patterns to scan is ${namePatternsToScan}. You will be prompted to add new patterns. If you would first like to clear the currently configured patterns, type 'clear'.")
        userValue = StringUtils.trimToEmpty(console.readLine())
        if ('clear' == userValue) {
            namePatterns = []
        }

        out.println('Enter Artifactory artifact filename patterns to scan, one at a time. If you are finished, just press <enter>.')
        out.print "Enter pattern (current patterns=${namePatterns.join(',')}): "
        userValue = StringUtils.trimToEmpty(console.readLine())
        while (StringUtils.isNotBlank(userValue)) {
            namePatterns.add(userValue)
            out.print 'Enter pattern: '
            userValue = StringUtils.trimToEmpty(console.readLine())
        }

        configurationProperties.hubArtifactoryScanReposToSearch = repositoryNames.join(',')
        configurationProperties.hubArtifactoryScanNamePatterns = namePatterns.join(',')
        persistValues()

        out.println("These repositories (${configurationProperties.hubArtifactoryScanReposToSearch}) will be searched for these artifact name patterns (${configurationProperties.hubArtifactoryScanNamePatterns}) which will then be scanned.")
        out.print('If this is incorrect, enter \'n\' to enter new values, otherwise, if they are correct, just press <enter>.')
        userValue = StringUtils.trimToEmpty(console.readLine())
        if ('n' == userValue) {
            updateArtifactoryScanValues(console, out)
        }
    }

    private persistValues() {
        Properties properties = new Properties()
        properties.setProperty('hub.url', configurationProperties.hubUrl)
        properties.setProperty('hub.timeout', configurationProperties.hubTimeout)
        properties.setProperty('hub.username', configurationProperties.hubUsername)
        properties.setProperty('hub.password', configurationProperties.hubPassword)
        properties.setProperty('hub.trust.cert', configurationProperties.hubAlwaysTrustCerts)
        properties.setProperty('hub.proxy.host', configurationProperties.hubProxyHost)
        properties.setProperty('hub.proxy.port', configurationProperties.hubProxyPort)
        properties.setProperty('hub.proxy.username', configurationProperties.hubProxyUsername)
        properties.setProperty('hub.proxy.password', configurationProperties.hubProxyPassword)
        properties.setProperty('artifactory.url', configurationProperties.artifactoryUrl)
        properties.setProperty('artifactory.username', configurationProperties.artifactoryUsername)
        properties.setProperty('artifactory.password', configurationProperties.artifactoryPassword)
        properties.setProperty('hub.artifactory.working.directory.path', configurationProperties.hubArtifactoryWorkingDirectoryPath)
        properties.setProperty('hub.artifactory.scan.binaries.directory.path', configurationProperties.hubArtifactoryScanBinariesDirectoryPath)
        properties.setProperty('hub.artifactory.project.name', configurationProperties.hubArtifactoryProjectName)
        properties.setProperty('hub.artifactory.project.version.name', configurationProperties.hubArtifactoryProjectVersionName)
        properties.setProperty('hub.artifactory.date.time.pattern', configurationProperties.hubArtifactoryDateTimePattern)
        properties.setProperty('hub.artifactory.inspect.repo.key', configurationProperties.hubArtifactoryInspectRepoKey)
        properties.setProperty('hub.artifactory.inspect.latest.updated.cutoff', configurationProperties.hubArtifactoryInspectLatestUpdatedCutoff)
        properties.setProperty('hub.artifactory.inspect.skip.bom.calculation', configurationProperties.hubArtifactoryInspectSkipBomCalculation)
        properties.setProperty('hub.artifactory.scan.repos.to.search', configurationProperties.hubArtifactoryScanReposToSearch)
        properties.setProperty('hub.artifactory.scan.name.patterns', configurationProperties.hubArtifactoryScanNamePatterns)

        def defaultPropertiesPersister = new DefaultPropertiesPersister()
        new FileOutputStream(userSpecifiedProperties).withStream {
            defaultPropertiesPersister.store(properties, it, null)
        }
    }

    private String setValueFromInput(Console console, PrintStream out, String propertyName, String oldValue) {
        out.print("Enter ${propertyName} (current value=\"${oldValue}\"): ")
        String userValue = StringUtils.trimToEmpty(console.readLine())
        if (StringUtils.isNotBlank(userValue)) {
            userValue
        } else {
            oldValue
        }
    }

    private String setPasswordFromInput(Console console, PrintStream out, String propertyName, String oldValue) {
        out.print("Enter ${propertyName}: ")
        char[] password = console.readPassword()
        if (null == password || password.length == 0) {
            oldValue
        } else {
            String passwordString = StringUtils.trimToEmpty(new String(password))
            if (StringUtils.isNotBlank(passwordString)) {
                passwordString
            } else {
                oldValue
            }
        }
    }
}