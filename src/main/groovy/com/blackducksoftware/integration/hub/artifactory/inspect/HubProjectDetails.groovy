package com.blackducksoftware.integration.hub.artifactory.inspect

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperties

@Component
class HubProjectDetails {
    @Autowired
    ConfigurationProperties configurationProperties

    String getHubProjectName() {
        if (StringUtils.isBlank(configurationProperties.hubArtifactoryProjectName)) {
            return configurationProperties.hubArtifactoryInspectRepoKey
        } else {
            return configurationProperties.hubArtifactoryProjectName
        }
    }

    String getHubProjectVersionName() {
        if (StringUtils.isBlank(configurationProperties.hubArtifactoryProjectVersionName)) {
            return DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now())
        } else {
            return configurationProperties.hubArtifactoryProjectVersionName
        }
    }
}
