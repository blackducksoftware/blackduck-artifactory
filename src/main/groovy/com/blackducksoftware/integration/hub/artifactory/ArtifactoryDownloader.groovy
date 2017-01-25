package com.blackducksoftware.integration.hub.artifactory

import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ArtifactoryDownloader {
    private final Logger logger = LoggerFactory.getLogger(ArtifactoryDownloader.class)

    @Autowired
    ConfigurationProperties configurationProperties

    File download(String downloadUri, String artifactName) {
        download(new URI(downloadUri), artifactName)
    }

    File download(URI downloadUri, String artifactName) {
        download(downloadUri.toURL(), artifactName)
    }

    File download(URL downloadUrl, String artifactName) {
        def inputStream = downloadUrl.openStream()
        File outputFile = new File(configurationProperties.hubArtifactoryWorkingDirectoryPath)
        outputFile = new File(outputFile, artifactName)
        try {
            FileUtils.copyInputStreamToFile(inputStream, outputFile)
        } catch (IOException e) {
            logger.error("There was an error downloading ${artifactName}: ${e.message}")
        }

        outputFile
    }
}
