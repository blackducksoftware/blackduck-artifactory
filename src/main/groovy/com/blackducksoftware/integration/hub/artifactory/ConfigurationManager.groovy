package com.blackducksoftware.integration.hub.artifactory

import javax.annotation.PostConstruct

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.DefaultPropertiesPersister

@Component
class ConfigurationManager {
    @Value('${user.dir}')
    String currentUserDirectory

    @Value('${hub.url}')
    String hubUrl

    @Value('${hub.timeout}')
    String hubTimeout

    @Value('${hub.username}')
    String hubUsername

    @Value('${hub.password}')
    String hubPassword

    @Value('${hub.proxy.host}')
    String hubProxyHost

    @Value('${hub.proxy.port}')
    String hubProxyPort

    @Value('${hub.proxy.ignored.proxy.hosts}')
    String hubProxyIgnoredProxyHosts

    @Value('${hub.proxy.username}')
    String hubProxyUsername

    @Value('${hub.proxy.password}')
    String hubProxyPassword

    @Value('${artifactory.username}')
    String artifactoryUsername

    @Value('${artifactory.password}')
    String artifactoryPassword

    @Value('${artifactory.url}')
    String artifactoryUrl

    @Value('${hub.artifactory.mode}')
    String hubArtifactoryMode

    @Value('${hub.artifactory.working.directory.path}')
    String hubArtifactoryWorkingDirectoryPath

    @Value('${hub.artifactory.project.name}')
    String hubArtifactoryProjectName

    @Value('${hub.artifactory.project.version.name}')
    String hubArtifactoryProjectVersionName

    @Value('${hub.artifactory.date.time.pattern}')
    String hubArtifactoryDateTimePattern

    @Value('${hub.artifactory.inspect.repo.key}')
    String hubArtifactoryInspectRepoKey

    @Value('${hub.artifactory.inspect.latest.updated.cutoff}')
    String hubArtifactoryInspectLatestUpdatedCutoff

    @Value('${hub.artifactory.scan.repos.to.search}')
    String hubArtifactoryScanReposToSearch

    @Value('${hub.artifactory.scan.name.patterns}')
    String hubArtifactoryScanNamePatterns

    @Value('${hub.artifactory.scan.latest.modified.cutoff}')
    String hubArtifactoryScanLatestModifiedCutoff

    File userSpecifiedProperties

    @PostConstruct
    void init() {
        File configDirectory = new File (currentUserDirectory, "config")
        if (!configDirectory.exists()) {
            configDirectory.mkdirs()
        }
        userSpecifiedProperties = new File (configDirectory, "application.properties")
        if (!userSpecifiedProperties.exists()) {
            persistValues()
        }
    }

    boolean needsHubConfigUpdate() {
        StringUtils.isBlank(hubUrl) || StringUtils.isBlank(hubUsername) || StringUtils.isBlank(hubPassword)
    }

    boolean needsArtifactoryUpdate() {
        StringUtils.isBlank(artifactoryUrl) || StringUtils.isBlank(artifactoryUsername) || StringUtils.isBlank(artifactoryPassword) || StringUtils.isBlank(hubArtifactoryMode) || StringUtils.isBlank(hubArtifactoryWorkingDirectoryPath)
    }

    boolean needsArtifactoryInspectUpdate() {
        StringUtils.isBlank(hubArtifactoryInspectRepoKey)
    }

    void updateHubConfigValues(Console console, PrintStream out) {
        out.println('Updating Hub Server Config - just hit enter to make no change to a value:')

        hubUrl = setValueFromInput(console, out, "Hub Server Url", hubUrl)
        hubTimeout = setValueFromInput(console, out, "Hub Server Timeout", hubTimeout)
        hubUsername = setValueFromInput(console, out, "Hub Server Username", hubUsername)
        hubPassword = setPasswordFromInput(console, out, "Hub Server Password", hubPassword)

        persistValues()
    }

    void updateHubProxyValues() {
    }

    void updateArtifactoryValues(Console console, PrintStream out) {
        artifactoryUsername = setValueFromInput(console, out, "Artifactory Username", artifactoryUsername)
        artifactoryPassword = setPasswordFromInput(console, out, "Artifactory Password", artifactoryPassword)
        artifactoryUrl = setValueFromInput(console, out, "Artifactory Url", artifactoryUrl)
        hubArtifactoryMode = setValueFromInput(console, out, "Hub Artifactory Mode (inspect or scan)", hubArtifactoryMode)
        hubArtifactoryWorkingDirectoryPath = setValueFromInput(console, out, "Hub Artifactory Working Directory Path", hubArtifactoryWorkingDirectoryPath)

        persistValues()
    }

    void updateArtifactoryInspectValues(Console console, PrintStream out) {
        hubArtifactoryProjectName = setValueFromInput(console, out, "Hub Artifactory Project Name (optional)", hubArtifactoryProjectName)
        hubArtifactoryProjectVersionName = setValueFromInput(console, out, "Hub Artifactory Project Version Name (optional)", hubArtifactoryProjectVersionName)
        hubArtifactoryInspectRepoKey = setValueFromInput(console, out, "Artifactory Repository To Inspect", hubArtifactoryInspectRepoKey)

        persistValues()
    }

    private persistValues() {
        Properties properties = new Properties()
        properties.setProperty("hub.url", hubUrl)
        properties.setProperty("hub.timeout", hubTimeout)
        properties.setProperty("hub.username", hubUsername)
        properties.setProperty("hub.password", hubPassword)
        properties.setProperty("hub.proxy.host", hubProxyHost)
        properties.setProperty("hub.proxy.port", hubProxyPort)
        properties.setProperty("hub.proxy.ignored.proxy.hosts", hubProxyIgnoredProxyHosts)
        properties.setProperty("hub.proxy.username", hubProxyUsername)
        properties.setProperty("hub.proxy.password", hubProxyPassword)
        properties.setProperty("artifactory.url", artifactoryUrl)
        properties.setProperty("artifactory.username", artifactoryUsername)
        properties.setProperty("artifactory.password", artifactoryPassword)
        properties.setProperty("hub.artifactory.mode", hubArtifactoryMode)
        properties.setProperty("hub.artifactory.working.directory.path", hubArtifactoryWorkingDirectoryPath)
        properties.setProperty("hub.artifactory.project.name", hubArtifactoryProjectName)
        properties.setProperty("hub.artifactory.project.version.name", hubArtifactoryProjectVersionName)
        properties.setProperty("hub.artifactory.date.time.pattern", hubArtifactoryDateTimePattern)
        properties.setProperty("hub.artifactory.inspect.repo.key", hubArtifactoryInspectRepoKey)
        properties.setProperty("hub.artifactory.inspect.latest.updated.cutoff", hubArtifactoryInspectLatestUpdatedCutoff)
        properties.setProperty("hub.artifactory.scan.repos.to.search", hubArtifactoryScanReposToSearch)
        properties.setProperty("hub.artifactory.scan.name.patterns", hubArtifactoryScanNamePatterns)
        properties.setProperty("hub.artifactory.scan.latest.modified.cutoff", hubArtifactoryScanLatestModifiedCutoff)

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
        if (null == password) {
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