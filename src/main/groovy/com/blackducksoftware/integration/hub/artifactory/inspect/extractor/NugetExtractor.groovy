package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import java.util.zip.ZipFile

import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioExternalIdentifier
import com.blackducksoftware.integration.hub.bdio.simple.model.Forge
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.ExternalId
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.NameVersionExternalId

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

            ExternalId externalId = new NameVersionExternalId(Forge.nuget, packageName, version)
            String bdioId = externalId.createDataId()
            BdioExternalIdentifier bdioExternalIdentifier = bdioPropertyHelper.createExternalIdentifier(externalId)
            bdioComponent = bdioNodeFactory.createComponent(packageName, version, bdioId, bdioExternalIdentifier)
            return bdioComponent
        } finally {
            IOUtils.closeQuietly(nupkgFile)
        }
    }
}
