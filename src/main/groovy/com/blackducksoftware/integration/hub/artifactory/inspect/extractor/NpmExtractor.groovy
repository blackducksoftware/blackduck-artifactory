package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import groovy.json.JsonSlurper

import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.bdio.model.ExternalIdentifierBuilder
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader

@Component
class NpmExtractor implements Extractor {
    private final Logger logger = LoggerFactory.getLogger(NpmExtractor.class)

    @Autowired
    ExternalIdentifierBuilder externalIdentifierBuilder

    @Autowired
    ArtifactoryDownloader artifactoryDownloader

    com.blackducksoftware.bdio.model.Component extract(String artifactName, Map jsonObject) {
        def downloadUri = jsonObject.downloadUri
        def tgzFile = artifactoryDownloader.download(downloadUri, artifactName)

        def tarArchiveInputStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(tgzFile)))
        def tarArchiveEntry
        while (null != (tarArchiveEntry = tarArchiveInputStream.getNextTarEntry())) {
            if ('package/package.json' == tarArchiveEntry.name) {
                long entrySize = tarArchiveEntry.size
                if (entrySize > Integer.MAX_VALUE) {
                    logger.warn("package.json is too large to consume for ${artifactName}")
                    return null
                }

                int fileSize = (int)entrySize
                byte[] entryBuffer = new byte[fileSize]
                int offset = 0
                while (offset < fileSize) {
                    int entryBytes = tarArchiveInputStream.read(entryBuffer, offset, fileSize - offset)
                    offset += entryBytes
                }

                String entryContent = new String(entryBuffer, StandardCharsets.UTF_8)
                def npmPackageJson = new JsonSlurper().parseText(entryContent)
                def packageName = npmPackageJson.name
                def version = npmPackageJson.version

                def component = new com.blackducksoftware.bdio.model.Component()
                component.id = downloadUri
                component.name = packageName
                component.version = version
                component.addExternalIdentifier(externalIdentifierBuilder.npm(packageName, version).build().get())

                return component
            }
        }
    }
}
