package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import java.util.zip.ZipFile

import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.bdio.model.ExternalIdentifierBuilder
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader
import com.blackducksoftware.integration.hub.artifactory.inspect.BdioComponentDetails

@Component
class NugetExtractor extends Extractor {
    @Autowired
    ExternalIdentifierBuilder externalIdentifierBuilder

    @Autowired
    ArtifactoryDownloader artifactoryDownloader

    boolean shouldAttemptExtract(String artifactName, Map jsonObject) {
        def extension = getExtension(artifactName)
        'nupkg' == extension
    }

    BdioComponentDetails extract(String artifactName, Map jsonObject) {
        def file = artifactoryDownloader.download(jsonObject, artifactName)

        def details = null
        def nupkgFile = new ZipFile(file)
        try {
            def nuspecZipEntry = nupkgFile.entries().find {
                it.name.endsWith('.nuspec')
            }

            def nuspecPackage = new XmlSlurper().parse(nupkgFile.getInputStream(nuspecZipEntry))
            def packageName = nuspecPackage.metadata.id.toString()
            def version = nuspecPackage.metadata.version.toString()

            def externalIdentifier = externalIdentifierBuilder.nuget(packageName, version).build().get()
            details = new BdioComponentDetails(name: packageName, version: version, externalIdentifier: externalIdentifier)
            return details
        } finally {
            IOUtils.closeQuietly(nupkgFile)
        }
    }
}
