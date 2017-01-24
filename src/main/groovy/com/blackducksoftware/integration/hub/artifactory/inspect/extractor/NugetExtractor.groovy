package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import java.util.zip.ZipFile

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.bdio.model.ExternalIdentifierBuilder
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader

@Component
class NugetExtractor implements Extractor {
    @Autowired
    ExternalIdentifierBuilder externalIdentifierBuilder

    @Autowired
    ArtifactoryDownloader artifactoryDownloader

    com.blackducksoftware.bdio.model.Component extract(String artifactName, Map jsonObject) {
        def downloadUri = jsonObject.downloadUri
        def file = artifactoryDownloader.download(downloadUri, artifactName)

        def nupkgFile = new ZipFile(file)
        def nuspecZipEntry = nupkgFile.entries().find {
            it.name.endsWith('.nuspec')
        }

        def nuspecPackage = new XmlSlurper().parse(nupkgFile.getInputStream(nuspecZipEntry))
        def packageName = nuspecPackage.metadata.id.toString()
        def version = nuspecPackage.metadata.version.toString()

        def component = new com.blackducksoftware.bdio.model.Component()
        component.id = downloadUri
        component.name = packageName
        component.version = version
        component.addExternalIdentifier(externalIdentifierBuilder.nuget(packageName, version).build().get())

        component
    }
}
