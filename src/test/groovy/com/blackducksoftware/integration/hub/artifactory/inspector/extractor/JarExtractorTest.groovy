package com.blackducksoftware.integration.hub.artifactory.inspector.extractor

import org.junit.Assert
import org.junit.Test

import com.blackducksoftware.integration.hub.artifactory.inspect.extractor.JarExtractor

class JarExtractorTest {
    @Test
    void testExtractingGav() {
        JarExtractor jarExtractor = new JarExtractor()
        def gav = jarExtractor.parseGav("/com/blackducksoftware/integration/integration-common/1.0.7/integration-common-1.0.7.jar")
        Assert.assertEquals([
            "com.blackducksoftware.integration",
            "integration-common",
            "1.0.7"
        ], gav)
    }

    @Test
    void testExtractingBadGav() {
        JarExtractor jarExtractor = new JarExtractor()
        def gav = jarExtractor.parseGav("1.0.7/integration-common-1.0.7.jar")
        Assert.assertNull(gav)
    }
}
