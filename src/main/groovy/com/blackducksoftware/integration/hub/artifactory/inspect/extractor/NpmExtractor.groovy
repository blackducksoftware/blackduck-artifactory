package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import groovy.json.JsonSlurper

import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.bdio.model.ExternalIdentifierBuilder
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader
import com.blackducksoftware.integration.hub.artifactory.inspect.BdioComponentDetails

@Component
class NpmExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(NpmExtractor.class)

    @Autowired
    ExternalIdentifierBuilder externalIdentifierBuilder

    @Autowired
    ArtifactoryDownloader artifactoryDownloader

    boolean shouldAttemptExtract(String artifactName, Map jsonObject) {
        def extension = getExtension(artifactName)
        'tgz' == extension || 'tar.gz' == extension
    }

    BdioComponentDetails extract(String artifactName, Map jsonObject) {
        def tgzFile = artifactoryDownloader.download(jsonObject, artifactName)

        def tarArchiveInputStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(tgzFile)))
        def details = null
        try {
            def tarArchiveEntry
            while (null != (tarArchiveEntry = tarArchiveInputStream.getNextTarEntry())) {
                if ('package/package.json' == tarArchiveEntry.name) {
                    byte[] entryBuffer = decompressTarContents(logger, 'package/package.json', artifactName, tarArchiveInputStream, tarArchiveEntry)
                    String entryContent = new String(entryBuffer, StandardCharsets.UTF_8)

                    def npmPackageJson = new JsonSlurper().parseText(entryContent)
                    def packageName = npmPackageJson.name
                    def version = npmPackageJson.version

                    def externalIdentifier = externalIdentifierBuilder.npm(packageName, version).build().get()
                    details = new BdioComponentDetails(name: packageName, version: version, externalIdentifier: externalIdentifier)
                    return details
                }
            }
        } finally {
            IOUtils.closeQuietly(tarArchiveInputStream)
        }
    }
}
