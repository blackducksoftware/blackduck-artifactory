package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioExternalIdentifier

@Component
class JarExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(JarExtractor.class)

    boolean shouldAttemptExtract(String artifactName, Map jsonObject) {
        def extension = getExtension(artifactName)
        'jar' == extension
    }

    BdioComponent extract(String artifactName, Map jsonObject) {
        def jarPath = jsonObject.path
        if (jarPath.endsWith('-javadoc.jar') || jarPath.endsWith('-sources.jar')) {
            return null
        }

        def gavPieces = parseGav(jarPath)
        if (gavPieces == null) {
            return null
        }

        String group = gavPieces[0]
        String artifact = gavPieces[1]
        String version = gavPieces[2]

        String bdioId = bdioPropertyHelper.createBdioId(group, artifact, version)
        BdioExternalIdentifier bdioExternalIdentifier = bdioPropertyHelper.createMavenExternalIdentifier(group, artifact, version)
        BdioComponent bdioComponent = bdioNodeFactory.createComponent(artifact, version, bdioId, bdioExternalIdentifier)

        bdioComponent
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
