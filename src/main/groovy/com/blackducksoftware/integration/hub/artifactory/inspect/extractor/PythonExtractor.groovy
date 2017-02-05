package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.bdio.model.ExternalIdentifier
import com.blackducksoftware.bdio.model.ExternalIdentifierBuilder
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader
import com.blackducksoftware.integration.hub.artifactory.inspect.BdioComponentDetails

@Component
class PythonExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(PythonExtractor.class)

    @Autowired
    ExternalIdentifierBuilder externalIdentifierBuilder

    @Autowired
    ArtifactoryDownloader artifactoryDownloader

    boolean shouldAttemptExtract(String artifactName, Map jsonObject) {
        def extension = getExtension(artifactName)
        'whl' == extension || artifactName.endsWith('.tar.gz') || 'tgz' == extension
    }

    BdioComponentDetails extract(String artifactName, Map jsonObject) {
        def pythonArchive = artifactoryDownloader.download(jsonObject, artifactName)

        def extension = getExtension(artifactName)
        def details = null
        if ('whl' == extension) {
            def wheelFile = new ZipFile(pythonArchive)
            try {
                def metadataEntry = wheelFile.entries().find { it.name.endsWith('/METADATA') }
                String metadata = IOUtils.toString(wheelFile.getInputStream(metadataEntry), StandardCharsets.UTF_8)
                details = extractDetails(metadata)
                return details
            } finally {
                IOUtils.closeQuietly(wheelFile)
            }
        } else if (artifactName.endsWith('tar.gz') || 'tgz' == extension) {
            def tarArchiveInputStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(pythonArchive)))
            try {
                def tarArchiveEntry
                while (null != (tarArchiveEntry = tarArchiveInputStream.getNextTarEntry())) {
                    if (tarArchiveEntry.name.endsWith('/PKG-INFO')) {
                        byte[] entryBuffer = decompressTarContents(logger, 'PKG-INFO', artifactName, tarArchiveInputStream, tarArchiveEntry)
                        String metadata = new String(entryBuffer, StandardCharsets.UTF_8)
                        details = extractDetails(metadata)
                        return details
                    }
                }
            } finally {
                IOUtils.closeQuietly(tarArchiveInputStream)
            }
        }
    }

    private BdioComponentDetails extractDetails(String metadata) {
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

        def externalIdentifier = new ExternalIdentifier();
        externalIdentifier.setExternalSystemTypeId('pypi');
        externalIdentifier.setExternalId("${name}/${version}");

        new BdioComponentDetails(name: name, version: version, externalIdentifier: externalIdentifier)
    }
}
