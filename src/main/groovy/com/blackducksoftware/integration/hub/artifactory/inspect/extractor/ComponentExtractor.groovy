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
class ComponentExtractor {
    @Autowired
    List<Extractor> extractors

    @Autowired
    ConfigurationProperties configurationProperties

    boolean shouldExtractComponent(String filename, Map jsonObject) {
        def lastUpdatedString = jsonObject.lastUpdated
        long lastUpdated = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(lastUpdatedString)).epochSecond
        long cutoffTime = ZonedDateTime.parse(configurationProperties.hubArtifactoryInspectLatestUpdatedCutoff, DateTimeFormatter.ofPattern(configurationProperties.hubArtifactoryDateTimePattern)).toEpochSecond()
        return lastUpdated >= cutoffTime
    }

    List<com.blackducksoftware.bdio.model.Component> extract(String artifactName, Map jsonObject) {
        def components = []

        def extension = getExtension(artifactName)
        for (Extractor extractor : extractors) {
            if (extractor.shouldAttemptExtract(artifactName, extension, jsonObject)) {
                def component = extractor.extract(artifactName, jsonObject)
                if (component != null) {
                    components.add(component)
                }
            }
        }

        return components
    }

    public String getExtension(String filename) {
        StringUtils.trimToEmpty(FilenameUtils.getExtension(filename)).toLowerCase()
    }
}