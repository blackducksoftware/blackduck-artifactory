package com.blackducksoftware.integration.hub.artifactory.inspector.extractor

import org.junit.Assert
import org.junit.Test

import com.blackducksoftware.bdio.model.ExternalIdentifierBuilder
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader
import com.blackducksoftware.integration.hub.artifactory.inspect.BdioComponentDetails
import com.blackducksoftware.integration.hub.artifactory.inspect.extractor.PythonExtractor

class PythonExtractorTest {
    @Test
    void testExtractingValohaiYaml() {
        URL url = this.getClass().getResource("/valohai_yaml-0.4-py2.py3-none-any.whl")
        File file = new File(url.getFile())

        def mockDownloader = [download: {Map jsonObject, String artifactName -> file}] as ArtifactoryDownloader

        PythonExtractor pythonExtractor = new PythonExtractor()
        pythonExtractor.artifactoryDownloader = mockDownloader
        pythonExtractor.externalIdentifierBuilder = ExternalIdentifierBuilder.create()

        Map jsonObject = ["downloadUri":"test"]
        BdioComponentDetails bdioComponentDetails = pythonExtractor.extract("valohai_yaml-0.4-py2.py3-none-any.whl", jsonObject)
        Assert.assertEquals("valohai-yaml", bdioComponentDetails.name)
        Assert.assertEquals("0.4", bdioComponentDetails.version)
        Assert.assertEquals("valohai-yaml/0.4", bdioComponentDetails.externalIdentifier.externalId)
    }

    @Test
    void testExtractingFunctools32() {
        URL url = this.getClass().getResource("/functools32-3.2.3-2.tar.gz")
        File file = new File(url.getFile())

        def mockDownloader = [download: {Map jsonObject, String artifactName -> file}] as ArtifactoryDownloader

        PythonExtractor pythonExtractor = new PythonExtractor()
        pythonExtractor.artifactoryDownloader = mockDownloader
        pythonExtractor.externalIdentifierBuilder = ExternalIdentifierBuilder.create()

        Map jsonObject = ["downloadUri":"test"]
        BdioComponentDetails bdioComponentDetails = pythonExtractor.extract("functools32-3.2.3-2.tar.gz", jsonObject)
        Assert.assertEquals("functools32", bdioComponentDetails.name)
        Assert.assertEquals("3.2.3-2", bdioComponentDetails.version)
        Assert.assertEquals("functools32/3.2.3-2", bdioComponentDetails.externalIdentifier.externalId)
    }
}
