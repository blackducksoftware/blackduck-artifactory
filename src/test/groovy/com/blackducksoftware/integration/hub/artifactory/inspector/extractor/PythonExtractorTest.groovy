package com.blackducksoftware.integration.hub.artifactory.inspector.extractor

import org.junit.Assert
import org.junit.Test

import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader
import com.blackducksoftware.integration.hub.artifactory.inspect.extractor.PythonExtractor
import com.blackducksoftware.integration.hub.bdio.simple.BdioNodeFactory
import com.blackducksoftware.integration.hub.bdio.simple.BdioPropertyHelper
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent

class PythonExtractorTest {
    @Test
    void testExtractingValohaiYaml() {
        URL url = this.getClass().getResource("/valohai_yaml-0.4-py2.py3-none-any.whl")
        File file = new File(url.getFile())

        def mockDownloader = [download: {Map jsonObject, String artifactName -> file}] as ArtifactoryDownloader

        PythonExtractor pythonExtractor = new PythonExtractor()
        pythonExtractor.artifactoryDownloader = mockDownloader
        pythonExtractor.bdioPropertyHelper = new BdioPropertyHelper()
        pythonExtractor.bdioNodeFactory = new BdioNodeFactory(pythonExtractor.bdioPropertyHelper)

        Map jsonObject = ["downloadUri":"test"]
        BdioComponent bdioComponentDetails = pythonExtractor.extract("valohai_yaml-0.4-py2.py3-none-any.whl", jsonObject)
        Assert.assertEquals("valohai-yaml", bdioComponentDetails.name)
        Assert.assertEquals("0.4", bdioComponentDetails.version)
        Assert.assertEquals("valohai-yaml/0.4", bdioComponentDetails.bdioExternalIdentifier.externalId)
    }

    @Test
    void testExtractingFunctools32() {
        URL url = this.getClass().getResource("/functools32-3.2.3-2.tar.gz")
        File file = new File(url.getFile())

        def mockDownloader = [download: {Map jsonObject, String artifactName -> file}] as ArtifactoryDownloader

        PythonExtractor pythonExtractor = new PythonExtractor()
        pythonExtractor.artifactoryDownloader = mockDownloader
        pythonExtractor.bdioPropertyHelper = new BdioPropertyHelper()
        pythonExtractor.bdioNodeFactory = new BdioNodeFactory(pythonExtractor.bdioPropertyHelper)

        Map jsonObject = ["downloadUri":"test"]
        BdioComponent bdioComponentDetails = pythonExtractor.extract("functools32-3.2.3-2.tar.gz", jsonObject)
        Assert.assertEquals("functools32", bdioComponentDetails.name)
        Assert.assertEquals("3.2.3-2", bdioComponentDetails.version)
        Assert.assertEquals("functools32/3.2.3-2", bdioComponentDetails.bdioExternalIdentifier.externalId)
    }
}
