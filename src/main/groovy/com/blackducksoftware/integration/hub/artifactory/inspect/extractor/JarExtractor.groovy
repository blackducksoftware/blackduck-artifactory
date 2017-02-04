package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.bdio.model.ExternalIdentifierBuilder

@Component
class JarExtractor implements Extractor {
    private final Logger logger = LoggerFactory.getLogger(JarExtractor.class)

    @Autowired
    ExternalIdentifierBuilder externalIdentifierBuilder

    boolean shouldAttemptExtract(String artifactName, String extension, Map jsonObject) {
        "jar" == extension
    }

    com.blackducksoftware.bdio.model.Component extract(String artifactName, Map jsonObject) {
        def jarPath = jsonObject.path
        if (jarPath.endsWith('-javadoc.jar') || jarPath.endsWith('-sources.jar')) {
            return null
        }

        def gavPieces = parseGav(jarPath)
        if (gavPieces == null) {
            return null
        }

        def component = new com.blackducksoftware.bdio.model.Component()
        component.id = jsonObject.downloadUri
        component.name = gavPieces[1]
        component.version = gavPieces[2]
        component.addExternalIdentifier(externalIdentifierBuilder.maven(gavPieces[0], gavPieces[1], gavPieces[2]).build().get())

        component
    }

    def parseGav(String path) {
        try {
            def pathPieces = path.tokenize('/')
            def version = pathPieces[-2]
            def artifact = pathPieces[-3]
            def group = pathPieces[0..-4].join('.')

            [group, artifact, version]
        } catch (Exception e) {
            logger.error("Couldn't parse the gav from ${path}")

            null
        }
    }
}
