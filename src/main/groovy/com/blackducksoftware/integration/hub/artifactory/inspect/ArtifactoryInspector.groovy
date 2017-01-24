package com.blackducksoftware.integration.hub.artifactory.inspect

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.bdio.io.BdioWriter
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryRestClient
import com.blackducksoftware.integration.hub.artifactory.ConfigurationManager
import com.blackducksoftware.integration.hub.artifactory.inspect.extractor.ComponentExtractor

@Component
class ArtifactoryInspector {
    private final Logger logger = LoggerFactory.getLogger(ArtifactoryInspector.class)

    @Autowired
    ArtifactoryRestClient artifactoryRestClient

    @Autowired
    BdioFileWriter bdioFileWriter

    @Autowired
    ComponentExtractor componentExtractor

    @Autowired
    HubProjectDetails hubProjectDetails

    @Autowired
    HubUploader hubUploader

    @Autowired
    ConfigurationManager configurationManager

    void performInspect() {
        def workingDirectory = new File(configurationManager.hubArtifactoryWorkingDirectoryPath)
        workingDirectory.mkdirs()
        def outputFile = new File(workingDirectory, "${hubProjectDetails.hubProjectName}_bdio.jsonld")
        new FileOutputStream(outputFile).withStream {
            def bdioWriter = bdioFileWriter.createBdioWriter(it, hubProjectDetails.hubProjectName, hubProjectDetails.hubProjectVersionName, outputFile.toURI())
            try {
                walkFolderStructure('', bdioWriter)
            } finally {
                bdioWriter.close()
            }
        }

        if (hubUploader.isValid()) {
            hubUploader.uploadBdioToHub(outputFile)
        }
    }

    private void walkFolderStructure(String repoPath, BdioWriter bdioWriter) {
        logger.trace("walking ${repoPath}")
        def jsonObject = artifactoryRestClient.getInfoForPath(configurationManager.hubArtifactoryInspectRepoKey, repoPath)
        if (jsonObject.children != null) {
            jsonObject.children.each {
                walkFolderStructure(repoPath + it.uri, bdioWriter)
            }
        } else {
            try {
                writeComponent(repoPath, jsonObject, bdioWriter)
            } catch (Exception e) {
                logger.error("Could not write the component ${repoPath}: ${e.message}")
            }
        }
    }

    private void writeComponent(String artifactRepoPath, Map jsonObject, BdioWriter bdioWriter) {
        def artifactName = new File(new URI(artifactRepoPath).path).name
        if (!componentExtractor.shouldExtractComponent(artifactName, jsonObject)) {
            return
        }

        def component = componentExtractor.extract(artifactName, jsonObject)
        if (component != null) {
            bdioWriter.write(component)
            logger.info("wrote ${artifactName}")
        }
    }
}