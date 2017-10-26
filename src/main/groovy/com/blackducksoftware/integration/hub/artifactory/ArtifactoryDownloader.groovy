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

    File download(Map jsonObject, String artifactName) {
        def downloadUri = jsonObject.downloadUri
        download(downloadUri, artifactName)
    }

    File download(String downloadUri, String artifactName) {
        download(new URI(downloadUri), artifactName)
    }

    File download(URI downloadUri, String artifactName) {
        download(downloadUri.toURL(), artifactName)
    }

    File download(URL downloadUrl, String artifactName) {
        URL connectionUrl = downloadUrl
        HttpURLConnection  connection
        InputStream inputStream
        File outputFile = new File(configurationProperties.hubArtifactoryWorkingDirectoryPath)
        try {
            connection = (HttpURLConnection) downloadUrl.openConnection()
            String userCredentials = "${configurationProperties.artifactoryUsername}:${configurationProperties.artifactoryPassword}"
            String basicAuth = 'Basic ' + Base64.encoder.encodeToString(userCredentials.getBytes())
            connection.setRequestProperty ('Authorization', basicAuth)
            connection.connect()
            inputStream = connection.getInputStream()
            if (followRedirect(connection)) {
                return download(new URL(connection.getHeaderField('Location')), artifactName)
            }
            outputFile = new File(outputFile, artifactName)
            FileUtils.copyInputStreamToFile(inputStream, outputFile)
        } catch (Exception e) {
            logger.error("There was an error downloading ${artifactName} from ${downloadUrl}- ${e.message}")
        } finally {
            inputStream?.close()
            connection?.disconnect()
        }

        outputFile
    }

    boolean followRedirect(HttpURLConnection connection) {
        int connectionResponseCode = connection.getResponseCode()
        return connectionResponseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                connectionResponseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                connectionResponseCode == HttpURLConnection.HTTP_SEE_OTHER
    }
}
