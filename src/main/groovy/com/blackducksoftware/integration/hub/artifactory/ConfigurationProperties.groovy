package com.blackducksoftware.integration.hub.artifactory

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ConfigurationProperties {
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
}
