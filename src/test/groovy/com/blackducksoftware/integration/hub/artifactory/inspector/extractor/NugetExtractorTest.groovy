package com.blackducksoftware.integration.hub.artifactory.inspector.extractor

import org.junit.Assert
import org.junit.Test

import com.blackducksoftware.bdio.model.ExternalIdentifierBuilder
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader
import com.blackducksoftware.integration.hub.artifactory.inspect.BdioComponentDetails
import com.blackducksoftware.integration.hub.artifactory.inspect.extractor.NugetExtractor

class NugetExtractorTest {
    @Test
    void testExtractingComponent() {
        URL url = this.getClass().getResource("/Microsoft.Net.Http.2.2.29.nupkg")
        File file = new File(url.getFile())

        def mockDownloader = [download: {Map jsonObject, String artifactName -> file}] as ArtifactoryDownloader

        NugetExtractor nugetExtractor = new NugetExtractor()
        nugetExtractor.artifactoryDownloader = mockDownloader
        nugetExtractor.externalIdentifierBuilder = ExternalIdentifierBuilder.create()

        Map jsonObject = ["downloadUri":"test"]
        BdioComponentDetails bdioComponentDetails = nugetExtractor.extract("Microsoft.Net.Http.2.2.29.nupkg", jsonObject)
        Assert.assertEquals("Microsoft.Net.Http", bdioComponentDetails.name)
        Assert.assertEquals("2.2.29", bdioComponentDetails.version)
        Assert.assertEquals("Microsoft.Net.Http/2.2.29", bdioComponentDetails.externalIdentifier.externalId)
    }
}
