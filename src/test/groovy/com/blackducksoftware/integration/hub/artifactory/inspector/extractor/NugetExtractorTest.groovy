package com.blackducksoftware.integration.hub.artifactory.inspector.extractor

import org.junit.Assert
import org.junit.Test

import com.blackducksoftware.bdio.model.Component
import com.blackducksoftware.bdio.model.ExternalIdentifierBuilder
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader
import com.blackducksoftware.integration.hub.artifactory.inspect.extractor.NugetExtractor;

class NugetExtractorTest {
    @Test
    void testExtractingComponent() {
        URL url = this.getClass().getResource("/Microsoft.Net.Http.2.2.29.nupkg")
        File file = new File(url.getFile())

        def mockDownloader = [download: {String downloadUri, String artifactName -> file}] as ArtifactoryDownloader

        NugetExtractor nugetExtractor = new NugetExtractor()
        nugetExtractor.artifactoryDownloader = mockDownloader
        nugetExtractor.externalIdentifierBuilder = ExternalIdentifierBuilder.create()

        Map jsonObject = ["downloadUri":"test"]
        Component component = nugetExtractor.extract("Microsoft.Net.Http.2.2.29.nupkg", jsonObject)
        Assert.assertEquals("Microsoft.Net.Http", component.name)
        Assert.assertEquals("2.2.29", component.version)
        Assert.assertEquals("Microsoft.Net.Http/2.2.29", component.externalIdentifiers.first().externalId)
    }
}
