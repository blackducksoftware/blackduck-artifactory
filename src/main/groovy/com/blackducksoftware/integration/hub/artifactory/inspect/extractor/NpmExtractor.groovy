package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import groovy.json.JsonSlurper

import java.util.zip.GZIPInputStream

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.bdio.model.ExternalIdentifierBuilder
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader

@Component
class NpmExtractor implements Extractor {
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
                def entryBuffer = new byte[tarArchiveEntry.size]
                int entryBytes = tarArchiveInputStream.read(entryBuffer, 0, entryBuffer.length)
                def entryContent = new String(entryBuffer, 0, entryBytes)

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
