package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.bdio.model.ExternalIdentifier
import com.blackducksoftware.bdio.model.ExternalIdentifierBuilder
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader

@Component
class PythonExtractor implements Extractor {
    private final Logger logger = LoggerFactory.getLogger(PythonExtractor.class)

    @Autowired
    ExternalIdentifierBuilder externalIdentifierBuilder

    @Autowired
    ArtifactoryDownloader artifactoryDownloader

    boolean shouldAttemptExtract(String artifactName, String extension, Map jsonObject) {
        "whl" == extension || "tar.gz" == extension
    }

    com.blackducksoftware.bdio.model.Component extract(String artifactName, Map jsonObject) {
        def downloadUri = jsonObject.downloadUri
        def pythonArchive = artifactoryDownloader.download(downloadUri, artifactName)

        def extension = StringUtils.trimToEmpty(FilenameUtils.getExtension(artifactName)).toLowerCase()
        if ('whl' == extension) {
            def wheelFile = new ZipFile(pythonArchive)
            def metadataEntry = wheelFile.entries().find { it.name.endsWith('/METADATA') }
            String metadata = IOUtils.toString(wheelFile.getInputStream(metadataEntry), StandardCharsets.UTF_8)
            def metadataLines = metadata.tokenize('\n')

            def currentLineIndex = 0
            def name = null
            def version = null
            while (currentLineIndex < metadataLines.size() && (name == null || version == null)) {
                def line = metadataLines[currentLineIndex]
                if (line.startsWith('Name:')) {
                    name = StringUtils.trimToNull(line[5..-1])
                } else if (line.startsWith('Version:')) {
                    version = StringUtils.trimToNull(line[8..-1])
                }
                currentLineIndex++
            }

            def component = new com.blackducksoftware.bdio.model.Component()
            component.id = downloadUri
            component.name = name
            component.version = version
            def externalIdentifier = new ExternalIdentifier();
            externalIdentifier.setExternalSystemTypeId('pypi');
            externalIdentifier.setExternalId("${name}/${version}");
            component.addExternalIdentifier(externalIdentifier)

            return component
        }
    }
}
