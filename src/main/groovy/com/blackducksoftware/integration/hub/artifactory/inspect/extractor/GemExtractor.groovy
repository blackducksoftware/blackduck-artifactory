package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

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
class GemExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(GemExtractor.class)

    @Autowired
    ArtifactoryDownloader artifactoryDownloader

    boolean shouldAttemptExtract(String artifactName, Map jsonObject) {
        def extension = getExtension(artifactName)
        'gem' == extension
    }

    BdioComponent extract(String artifactName, Map jsonObject) {
        def downloadUri = jsonObject.downloadUri
        def gemFile = artifactoryDownloader.download(downloadUri, artifactName)

        def tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(gemFile))
        def bdioComponent = null
        try {
            def tarArchiveEntry
            while (null != (tarArchiveEntry = tarArchiveInputStream.getNextTarEntry())) {
                if ('metadata.gz' == tarArchiveEntry.name) {
                    def entryBuffer = decompressTarContents(logger, 'metadata.gz', artifactName, tarArchiveInputStream, tarArchiveEntry)

                    def gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(entryBuffer))
                    try {
                        String metadataYaml = IOUtils.toString(gzipInputStream, StandardCharsets.UTF_8)
                        def metadataLines = metadataYaml.tokenize('\n')

                        def currentLineIndex = 0
                        def gem = null
                        def version = null
                        while (currentLineIndex < metadataLines.size() && (gem == null || version == null)) {
                            def line = metadataLines[currentLineIndex]
                            if (line.startsWith('name:')) {
                                gem = StringUtils.trimToNull(line[5..-1])
                            } else if (line.startsWith('  version:')) {
                                version = StringUtils.trimToNull(line[10..-1])
                            }
                            currentLineIndex++
                        }

                        ExternalId externalId = new NameVersionExternalId(Forge.RUBYGEMS, gem, version)
                        bdioComponent = bdioNodeFactory.createComponent(gem, version, externalId)
                        return bdioComponent
                    } finally {
                        IOUtils.closeQuietly(gzipInputStream)
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(tarArchiveInputStream)
        }
    }
}