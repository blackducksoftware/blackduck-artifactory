package com.blackducksoftware.integration.hub.artifactory.inspector.extractor

import org.junit.Assert
import org.junit.Test

import com.blackducksoftware.bdio.model.Component
import com.blackducksoftware.bdio.model.ExternalIdentifierBuilder
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader
import com.blackducksoftware.integration.hub.artifactory.inspect.extractor.NpmExtractor

class NpmExtractorTest {
    @Test
    void testExtractingGrunt() {
        URL url = this.getClass().getResource("/grunt-1.0.0.tgz")
        File file = new File(url.getFile())

        def mockDownloader = [download: {String downloadUri, String artifactName -> file}] as ArtifactoryDownloader

        NpmExtractor npmExtractor = new NpmExtractor()
        npmExtractor.artifactoryDownloader = mockDownloader
        npmExtractor.externalIdentifierBuilder = ExternalIdentifierBuilder.create()

        Map jsonObject = ["downloadUri":"test"]
        Component component = npmExtractor.extract("grunt-1.0.0.tgz", jsonObject)
        Assert.assertEquals("grunt", component.name)
        Assert.assertEquals("1.0.0", component.version)
        Assert.assertEquals("grunt@1.0.0", component.externalIdentifiers.first().externalId)
    }

    @Test
    void testExtractingAngular() {
        URL url = this.getClass().getResource("/angular-1.6.1.tgz")
        File file = new File(url.getFile())

        def mockDownloader = [download: {String downloadUri, String artifactName -> file}] as ArtifactoryDownloader

        NpmExtractor npmExtractor = new NpmExtractor()
        npmExtractor.artifactoryDownloader = mockDownloader
        npmExtractor.externalIdentifierBuilder = ExternalIdentifierBuilder.create()

        Map jsonObject = ["downloadUri":"test"]
        Component component = npmExtractor.extract("angular-1.6.1.tgz", jsonObject)
        Assert.assertEquals("angular", component.name)
        Assert.assertEquals("1.6.1", component.version)
        Assert.assertEquals("angular@1.6.1", component.externalIdentifiers.first().externalId)
    }

    @Test
    void testExtractingBabelPolyfill() {
        URL url = this.getClass().getResource("/babel-polyfill-6.22.0.tgz")
        File file = new File(url.getFile())

        def mockDownloader = [download: {String downloadUri, String artifactName -> file}] as ArtifactoryDownloader

        NpmExtractor npmExtractor = new NpmExtractor()
        npmExtractor.artifactoryDownloader = mockDownloader
        npmExtractor.externalIdentifierBuilder = ExternalIdentifierBuilder.create()

        Map jsonObject = ["downloadUri":"test"]
        Component component = npmExtractor.extract("babel-polyfill-6.22.0", jsonObject)
        Assert.assertEquals("babel-polyfill", component.name)
        Assert.assertEquals("6.22.0", component.version)
        Assert.assertEquals("babel-polyfill@6.22.0", component.externalIdentifiers.first().externalId)
    }

    @Test
    void testExtractingBabelPolyfill6_3_14() {
        URL url = this.getClass().getResource("/babel-polyfill-6.3.14.tgz")
        File file = new File(url.getFile())

        def mockDownloader = [download: {String downloadUri, String artifactName -> file}] as ArtifactoryDownloader

        NpmExtractor npmExtractor = new NpmExtractor()
        npmExtractor.artifactoryDownloader = mockDownloader
        npmExtractor.externalIdentifierBuilder = ExternalIdentifierBuilder.create()

        Map jsonObject = ["downloadUri":"test"]
        Component component = npmExtractor.extract("babel-polyfill-6.3.14", jsonObject)
        Assert.assertEquals("babel-polyfill", component.name)
        Assert.assertEquals("6.3.14", component.version)
        Assert.assertEquals("babel-polyfill@6.3.14", component.externalIdentifiers.first().externalId)
    }
}
