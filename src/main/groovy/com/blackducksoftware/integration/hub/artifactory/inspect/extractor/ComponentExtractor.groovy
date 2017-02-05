package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperties
import com.blackducksoftware.integration.hub.artifactory.inspect.BdioComponentDetails

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

        for (Extractor extractor : extractors) {
            if (extractor.shouldAttemptExtract(artifactName, jsonObject)) {
                def componentDetails = extractor.extract(artifactName, jsonObject)
                if (componentDetails != null) {
                    def component = buildComponent(jsonObject, componentDetails)
                    components.add(component)
                }
            }
        }

        return components
    }

    private com.blackducksoftware.bdio.model.Component buildComponent(Map jsonObject, BdioComponentDetails bdioComponentDetails) {
        def component = new com.blackducksoftware.bdio.model.Component()
        component.id = jsonObject.downloadUri
        component.name = bdioComponentDetails.name
        component.version = bdioComponentDetails.version
        component.addExternalIdentifier(bdioComponentDetails.externalIdentifier)

        component
    }
}