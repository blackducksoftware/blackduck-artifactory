package com.blackducksoftware.integration.hub.artifactory.inspector.extractor

import org.junit.Assert
import org.junit.Test

import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperties
import com.blackducksoftware.integration.hub.artifactory.inspect.extractor.ComponentExtractor

class ComponentExtractorTest {
    @Test
    void testDateCutoffOkay() {
        ConfigurationProperties configurationProperties = new ConfigurationProperties()
        configurationProperties.hubArtifactoryDateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX"
        configurationProperties.hubArtifactoryInspectLatestUpdatedCutoff = "2015-01-01T00:00:00.000Z"

        def componentExtractor = new ComponentExtractor()
        componentExtractor.configurationProperties = configurationProperties

        Assert.assertTrue(componentExtractor.shouldExtractComponent("", ["repo":"repoKey", "path":"repoPath", "lastUpdated":"2017-01-14T07:05:36.059-05:00"]))
    }

    @Test
    void testDateCutoffOkay2() {
        ConfigurationProperties configurationProperties = new ConfigurationProperties()
        configurationProperties.hubArtifactoryDateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX"
        configurationProperties.hubArtifactoryInspectLatestUpdatedCutoff = "2017-01-14T12:04:00.000Z"

        def componentExtractor = new ComponentExtractor()
        componentExtractor.configurationProperties = configurationProperties

        Assert.assertTrue(componentExtractor.shouldExtractComponent("", ["repo":"repoKey", "path":"repoPath", "lastUpdated":"2017-01-14T07:05:36.059-05:00"]))
    }

    @Test
    void testDateCutoffNotOkay() {
        ConfigurationProperties configurationProperties = new ConfigurationProperties()
        configurationProperties.hubArtifactoryDateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX"
        configurationProperties.hubArtifactoryInspectLatestUpdatedCutoff = "2017-01-14T12:06:00.000Z"

        def componentExtractor = new ComponentExtractor()
        componentExtractor.configurationProperties = configurationProperties

        Assert.assertFalse(componentExtractor.shouldExtractComponent("", ["repo":"repoKey", "path":"repoPath", "lastUpdated":"2017-01-14T07:05:36.059-05:00"]))
    }
}
