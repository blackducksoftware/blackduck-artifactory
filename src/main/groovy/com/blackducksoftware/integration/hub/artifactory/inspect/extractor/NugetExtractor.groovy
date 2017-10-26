package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import java.util.zip.ZipFile

import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader
import com.blackducksoftware.integration.hub.bdio.model.BdioComponent
import com.blackducksoftware.integration.hub.bdio.model.Forge
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalId

@Component
class NugetExtractor extends Extractor {
    @Autowired
    ArtifactoryDownloader artifactoryDownloader

    boolean shouldAttemptExtract(String artifactName, Map jsonObject) {
        def extension = getExtension(artifactName)
        'nupkg' == extension
    }

    BdioComponent extract(String artifactName, Map jsonObject) {
        def file = artifactoryDownloader.download(jsonObject, artifactName)

        BdioComponent bdioComponent = null
        def nupkgFile = new ZipFile(file)
        try {
            def nuspecZipEntry = nupkgFile.entries().find {
                it.name.endsWith('.nuspec')
            }

            def nuspecPackage = new XmlSlurper().parse(nupkgFile.getInputStream(nuspecZipEntry))
            def packageName = nuspecPackage.metadata.id.toString()
            def version = nuspecPackage.metadata.version.toString()

            ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.NUGET, packageName, version)
            bdioComponent = bdioNodeFactory.createComponent(packageName, version, externalId)
            return bdioComponent
        } finally {
            IOUtils.closeQuietly(nupkgFile)
        }
    }
}
