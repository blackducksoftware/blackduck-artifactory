package com.blackducksoftware.integration.hub.artifactory.inspector.extractor

import org.junit.Assert
import org.junit.Test

import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader
import com.blackducksoftware.integration.hub.artifactory.inspect.extractor.NpmExtractor
import com.blackducksoftware.integration.hub.bdio.simple.BdioNodeFactory
import com.blackducksoftware.integration.hub.bdio.simple.BdioPropertyHelper
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent

class NpmExtractorTest {
    @Test
    void testExtractingGrunt() {
        URL url = this.getClass().getResource("/grunt-1.0.0.tgz")
        File file = new File(url.getFile())

        def mockDownloader = [download: {Map jsonObject, String artifactName -> file}] as ArtifactoryDownloader

        NpmExtractor npmExtractor = new NpmExtractor()
        npmExtractor.artifactoryDownloader = mockDownloader
        npmExtractor.bdioPropertyHelper = new BdioPropertyHelper()
        npmExtractor.bdioNodeFactory = new BdioNodeFactory(npmExtractor.bdioPropertyHelper)

        Map jsonObject = ["downloadUri":"test"]
        BdioComponent bdioComponentDetails = npmExtractor.extract("grunt-1.0.0.tgz", jsonObject)
        Assert.assertEquals("grunt", bdioComponentDetails.name)
        Assert.assertEquals("1.0.0", bdioComponentDetails.version)
        Assert.assertEquals("grunt@1.0.0", bdioComponentDetails.bdioExternalIdentifier.externalId)
    }

    @Test
    void testExtractingAngular() {
        URL url = this.getClass().getResource("/angular-1.6.1.tgz")
        File file = new File(url.getFile())

        def mockDownloader = [download: {Map jsonObject, String artifactName -> file}] as ArtifactoryDownloader

        NpmExtractor npmExtractor = new NpmExtractor()
        npmExtractor.artifactoryDownloader = mockDownloader
        npmExtractor.bdioPropertyHelper = new BdioPropertyHelper()
        npmExtractor.bdioNodeFactory = new BdioNodeFactory(npmExtractor.bdioPropertyHelper)

        Map jsonObject = ["downloadUri":"test"]
        BdioComponent bdioComponentDetails = npmExtractor.extract("angular-1.6.1.tgz", jsonObject)
        Assert.assertEquals("angular", bdioComponentDetails.name)
        Assert.assertEquals("1.6.1", bdioComponentDetails.version)
        Assert.assertEquals("angular@1.6.1", bdioComponentDetails.bdioExternalIdentifier.externalId)
    }

    @Test
    void testExtractingBabelPolyfill() {
        URL url = this.getClass().getResource("/babel-polyfill-6.22.0.tgz")
        File file = new File(url.getFile())

        def mockDownloader = [download: {Map jsonObject, String artifactName -> file}] as ArtifactoryDownloader

        NpmExtractor npmExtractor = new NpmExtractor()
        npmExtractor.artifactoryDownloader = mockDownloader
        npmExtractor.bdioPropertyHelper = new BdioPropertyHelper()
        npmExtractor.bdioNodeFactory = new BdioNodeFactory(npmExtractor.bdioPropertyHelper)

        Map jsonObject = ["downloadUri":"test"]
        BdioComponent bdioComponentDetails = npmExtractor.extract("babel-polyfill-6.22.0", jsonObject)
        Assert.assertEquals("babel-polyfill", bdioComponentDetails.name)
        Assert.assertEquals("6.22.0", bdioComponentDetails.version)
        Assert.assertEquals("babel-polyfill@6.22.0", bdioComponentDetails.bdioExternalIdentifier.externalId)
    }

    @Test
    void testExtractingBabelPolyfill6_3_14() {
        URL url = this.getClass().getResource("/babel-polyfill-6.3.14.tgz")
        File file = new File(url.getFile())

        def mockDownloader = [download: {Map jsonObject, String artifactName -> file}] as ArtifactoryDownloader

        NpmExtractor npmExtractor = new NpmExtractor()
        npmExtractor.artifactoryDownloader = mockDownloader
        npmExtractor.bdioPropertyHelper = new BdioPropertyHelper()
        npmExtractor.bdioNodeFactory = new BdioNodeFactory(npmExtractor.bdioPropertyHelper)

        Map jsonObject = ["downloadUri":"test"]
        BdioComponent bdioComponentDetails = npmExtractor.extract("babel-polyfill-6.3.14", jsonObject)
        Assert.assertEquals("babel-polyfill", bdioComponentDetails.name)
        Assert.assertEquals("6.3.14", bdioComponentDetails.version)
        Assert.assertEquals("babel-polyfill@6.3.14", bdioComponentDetails.bdioExternalIdentifier.externalId)
    }

    @Test
    void testExtractingUnderscoreString2_0_0() {
        URL url = this.getClass().getResource("/underscore.string-2.0.0.tgz")
        File file = new File(url.getFile())

        def mockDownloader = [download: {Map jsonObject, String artifactName -> file}] as ArtifactoryDownloader

        NpmExtractor npmExtractor = new NpmExtractor()
        npmExtractor.artifactoryDownloader = mockDownloader
        npmExtractor.bdioPropertyHelper = new BdioPropertyHelper()
        npmExtractor.bdioNodeFactory = new BdioNodeFactory(npmExtractor.bdioPropertyHelper)

        Map jsonObject = ["downloadUri":"test"]
        BdioComponent bdioComponentDetails = npmExtractor.extract("underscore.string-2.0.0", jsonObject)
        Assert.assertEquals("underscore.string", bdioComponentDetails.name)
        Assert.assertEquals("2.0.0", bdioComponentDetails.version)
        Assert.assertEquals("underscore.string@2.0.0", bdioComponentDetails.bdioExternalIdentifier.externalId)
    }
}
