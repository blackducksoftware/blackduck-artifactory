package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.bdio.model.ExternalIdentifierBuilder
import com.blackducksoftware.integration.hub.artifactory.inspect.BdioComponentDetails

@Component
class JarExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(JarExtractor.class)

    @Autowired
    ExternalIdentifierBuilder externalIdentifierBuilder

    boolean shouldAttemptExtract(String artifactName, Map jsonObject) {
        def extension = getExtension(artifactName)
        'jar' == extension
    }

    BdioComponentDetails extract(String artifactName, Map jsonObject) {
        def jarPath = jsonObject.path
        if (jarPath.endsWith('-javadoc.jar') || jarPath.endsWith('-sources.jar')) {
            return null
        }

        def gavPieces = parseGav(jarPath)
        if (gavPieces == null) {
            return null
        }

        def externalIdentifier = externalIdentifierBuilder.maven(gavPieces[0], gavPieces[1], gavPieces[2]).build().get()
        def details = new BdioComponentDetails(name: gavPieces[1], version: gavPieces[2], externalIdentifier: externalIdentifier)
        details
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
