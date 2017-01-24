package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import com.blackducksoftware.bdio.model.Component

interface Extractor {
    Component extract(String artifactName, Map jsonObject)
}
