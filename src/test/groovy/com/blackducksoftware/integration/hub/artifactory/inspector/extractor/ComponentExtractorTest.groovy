package com.blackducksoftware.integration.hub.artifactory.inspector.extractor

import org.junit.Assert
import org.junit.Test

import com.blackducksoftware.integration.hub.artifactory.ConfigurationManager
import com.blackducksoftware.integration.hub.artifactory.inspect.extractor.ComponentExtractor

class ComponentExtractorTest {
    @Test
    void testDateCutoffOkay() {
        ConfigurationManager configurationManager = new ConfigurationManager()
        configurationManager.hubArtifactoryDateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX"
        configurationManager.hubArtifactoryInspectLatestUpdatedCutoff = "2015-01-01T00:00:00.000Z"

        def componentExtractor = new ComponentExtractor()
        componentExtractor.configurationManager = configurationManager

        Assert.assertTrue(componentExtractor.shouldExtractComponent("", ["repo":"repoKey", "path":"repoPath", "lastUpdated":"2017-01-14T07:05:36.059-05:00"]))
    }

    @Test
    void testDateCutoffOkay2() {
        ConfigurationManager configurationManager = new ConfigurationManager()
        configurationManager.hubArtifactoryDateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX"
        configurationManager.hubArtifactoryInspectLatestUpdatedCutoff = "2017-01-14T12:04:00.000Z"

        def componentExtractor = new ComponentExtractor()
        componentExtractor.configurationManager = configurationManager

        Assert.assertTrue(componentExtractor.shouldExtractComponent("", ["repo":"repoKey", "path":"repoPath", "lastUpdated":"2017-01-14T07:05:36.059-05:00"]))
    }

    @Test
    void testDateCutoffNotOkay() {
        ConfigurationManager configurationManager = new ConfigurationManager()
        configurationManager.hubArtifactoryDateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX"
        configurationManager.hubArtifactoryInspectLatestUpdatedCutoff = "2017-01-14T12:06:00.000Z"

        def componentExtractor = new ComponentExtractor()
        componentExtractor.configurationManager = configurationManager

        Assert.assertFalse(componentExtractor.shouldExtractComponent("", ["repo":"repoKey", "path":"repoPath", "lastUpdated":"2017-01-14T07:05:36.059-05:00"]))
    }
}
