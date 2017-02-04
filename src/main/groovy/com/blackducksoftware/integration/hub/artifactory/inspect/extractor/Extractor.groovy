package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import com.blackducksoftware.bdio.model.Component

interface Extractor {
    boolean shouldAttemptExtract(String artifactName, String extension, Map jsonObject)
    Component extract(String artifactName, Map jsonObject)
}
