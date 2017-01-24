package com.blackducksoftware.integration.hub.artifactory.inspect

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.artifactory.ConfigurationManager

@Component
class HubProjectDetails {
    @Autowired
    ConfigurationManager configurationManager

    String getHubProjectName() {
        if (StringUtils.isBlank(configurationManager.hubArtifactoryProjectName)) {
            return configurationManager.hubArtifactoryInspectRepoKey
        } else {
            return configurationManager.hubArtifactoryProjectName
        }
    }

    String getHubProjectVersionName() {
        if (StringUtils.isBlank(configurationManager.hubArtifactoryProjectVersionName)) {
            return DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now())
        } else {
            return configurationManager.hubArtifactoryProjectVersionName
        }
    }
}
