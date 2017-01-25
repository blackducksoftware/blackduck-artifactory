package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperties

@Component
class ComponentExtractor implements Extractor {
    @Autowired
    NugetExtractor nugetExtractor

    @Autowired
    JarExtractor jarExtractor

    @Autowired
    NpmExtractor npmExtractor

    @Autowired
    GemExtractor gemExtractor

    @Autowired
    ConfigurationProperties configurationProperties

    boolean shouldExtractComponent(String filename, Map jsonObject) {
        def lastUpdatedString = jsonObject.lastUpdated
        long lastUpdated = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(lastUpdatedString)).epochSecond
        long cutoffTime = ZonedDateTime.parse(configurationProperties.hubArtifactoryInspectLatestUpdatedCutoff ,DateTimeFormatter.ofPattern(configurationProperties.hubArtifactoryDateTimePattern)).toEpochSecond()
        return lastUpdated >= cutoffTime
    }

    com.blackducksoftware.bdio.model.Component extract(String artifactName, Map jsonObject) {
        def extension = getExtension(artifactName)

        if ("nupkg".equals(extension)) {
            return nugetExtractor.extract(artifactName, jsonObject)
        } else if ("jar".equals(extension)) {
            return jarExtractor.extract(artifactName, jsonObject)
        } else if ("tgz".equals(extension)) {
            return npmExtractor.extract(artifactName, jsonObject)
        } else if ("gem".equals(extension)) {
            return gemExtractor.extract(artifactName, jsonObject)
        }

        return null
    }

    private String getExtension(String filename) {
        StringUtils.trimToEmpty(FilenameUtils.getExtension(filename)).toLowerCase()
    }
}