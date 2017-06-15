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

import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioExternalIdentifier
import com.blackducksoftware.integration.hub.bdio.simple.model.Forge
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.ExternalId
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.NameVersionExternalId

@Component
class PythonExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(PythonExtractor.class)

    @Autowired
    ArtifactoryDownloader artifactoryDownloader

    boolean shouldAttemptExtract(String artifactName, Map jsonObject) {
        def extension = getExtension(artifactName)
        'whl' == extension || artifactName.endsWith('.tar.gz') || 'tgz' == extension
    }

    BdioComponent extract(String artifactName, Map jsonObject) {
        def pythonArchive = artifactoryDownloader.download(jsonObject, artifactName)

        def extension = getExtension(artifactName)
        BdioComponent bdioComponent = null
        if ('whl' == extension) {
            def wheelFile = new ZipFile(pythonArchive)
            try {
                def metadataEntry = wheelFile.entries().find { it.name.endsWith('/METADATA') }
                String metadata = IOUtils.toString(wheelFile.getInputStream(metadataEntry), StandardCharsets.UTF_8)
                bdioComponent = extractComponent(metadata)
                return bdioComponent
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
                        bdioComponent = extractComponent(metadata)
                        return bdioComponent
                    }
                }
            } finally {
                IOUtils.closeQuietly(tarArchiveInputStream)
            }
        }
    }

    private BdioComponent extractComponent(String metadata) {
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
        ExternalId externalId = new NameVersionExternalId(Forge.PYPI, name, version)
        String bdioId = externalId.createDataId()
        BdioExternalIdentifier bdioExternalIdentifier = bdioPropertyHelper.createExternalIdentifier(externalId)
        BdioComponent bdioComponent = bdioNodeFactory.createComponent(name, version, bdioId, bdioExternalIdentifier)
        return bdioComponent
    }
}
