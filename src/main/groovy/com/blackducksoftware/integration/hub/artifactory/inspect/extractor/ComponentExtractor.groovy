package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperties
import com.blackducksoftware.integration.hub.artifactory.inspect.InspectionResults
import com.blackducksoftware.integration.hub.bdio.model.BdioComponent

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

    List<BdioComponent> extract(String artifactName, Map jsonObject, InspectionResults inspectionResults) {
        def components = []

        boolean extractAttempted = false
        for (Extractor extractor : extractors) {
            if (extractor.shouldAttemptExtract(artifactName, jsonObject)) {
                extractAttempted = true
                BdioComponent bdioComponent = extractor.extract(artifactName, jsonObject)
                if (bdioComponent != null) {
                    bdioComponent.id = jsonObject.downloadUri
                    components.add(bdioComponent)
                }
            }
        }

        if (extractAttempted) {
            inspectionResults.totalExtractAttempts++
        }

        return components
    }
}
