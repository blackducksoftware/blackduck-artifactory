package com.blackducksoftware.integration.hub.artifactory.inspector.extractor

import org.junit.Assert
import org.junit.Test

import com.blackducksoftware.bdio.model.Component
import com.blackducksoftware.bdio.model.ExternalIdentifierBuilder
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader
import com.blackducksoftware.integration.hub.artifactory.inspect.extractor.PythonExtractor

class PythonExtractorTest {
    @Test
    void testExtractingValohaiYaml() {
        URL url = this.getClass().getResource("/valohai_yaml-0.4-py2.py3-none-any.whl")
        File file = new File(url.getFile())

        def mockDownloader = [download: {String downloadUri, String artifactName -> file}] as ArtifactoryDownloader

        PythonExtractor pythonExtractor = new PythonExtractor()
        pythonExtractor.artifactoryDownloader = mockDownloader
        pythonExtractor.externalIdentifierBuilder = ExternalIdentifierBuilder.create()

        Map jsonObject = ["downloadUri":"test"]
        Component component = pythonExtractor.extract("valohai_yaml-0.4-py2.py3-none-any.whl", jsonObject)
        Assert.assertEquals("valohai-yaml", component.name)
        Assert.assertEquals("0.4", component.version)
        Assert.assertEquals("valohai-yaml/0.4", component.externalIdentifiers.first().externalId)
    }
}
