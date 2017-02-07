package com.blackducksoftware.integration.hub.artifactory.inspect

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.bdio.io.BdioWriter
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryRestClient
import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperties
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
    HubClient hubClient

    @Autowired
    ConfigurationProperties configurationProperties

    void performInspect() {
        def inspectionResults = new InspectionResults()
        def workingDirectory = new File(configurationProperties.hubArtifactoryWorkingDirectoryPath)
        workingDirectory.mkdirs()
        def outputFile = new File(workingDirectory, "${hubProjectDetails.hubProjectName}_bdio.jsonld")
        logger.info("Starting bdio creation using file: ${outputFile.canonicalPath}")
        new FileOutputStream(outputFile).withStream {
            def bdioWriter = bdioFileWriter.createBdioWriter(it, hubProjectDetails.hubProjectName, hubProjectDetails.hubProjectVersionName, outputFile.toURI())
            try {
                walkFolderStructure('', bdioWriter, inspectionResults)
            } finally {
                bdioWriter.close()
            }
        }

        logger.info("Completed bdio file: ${outputFile.canonicalPath}")

        logger.trace("Total artifacts found: ${inspectionResults.totalArtifactsFound}")
        logger.info("Total attempts to extract a component: ${inspectionResults.totalExtractAttempts}")
        logger.info("Total BDIO component nodes created: ${inspectionResults.totalBdioNodesCreated}")
        logger.info("Count of artifacts that were extracted by only one extractor: ${inspectionResults.singlesFound}")
        logger.info("Count of artifacts that were extracted by MORE than one extractor: ${inspectionResults.multiplesFound}")
        logger.trace("Count of artifacts that were NOT extracted: ${inspectionResults.artifactsNotExtracted}")
        logger.info("Count of artifacts that were skipped because they are too old: ${inspectionResults.skippedArtifacts}")

        if (hubClient.isValid()) {
            hubClient.uploadBdioToHub(outputFile)
            logger.info("Uploaded bdio to ${configurationProperties.hubUrl}")
        }
    }

    private void walkFolderStructure(String repoPath, BdioWriter bdioWriter, InspectionResults inspectionResults) {
        logger.trace("walking ${repoPath}")
        def jsonObject = artifactoryRestClient.getInfoForPath(configurationProperties.hubArtifactoryInspectRepoKey, repoPath)
        if (jsonObject.children != null) {
            jsonObject.children.each {
                walkFolderStructure(repoPath + it.uri, bdioWriter, inspectionResults)
            }
        } else {
            try {
                writeComponent(repoPath, jsonObject, bdioWriter, inspectionResults)
            } catch (Exception e) {
                logger.error("Could not write the component ${repoPath}: ${e.message}")
            }
        }
    }

    private void writeComponent(String artifactRepoPath, Map jsonObject, BdioWriter bdioWriter, InspectionResults inspectionResults) {
        inspectionResults.totalArtifactsFound++
        def artifactName = new File(new URI(artifactRepoPath).path).name
        if (!componentExtractor.shouldExtractComponent(artifactName, jsonObject)) {
            inspectionResults.skippedArtifacts++
            return
        }

        def components = componentExtractor.extract(artifactName, jsonObject, inspectionResults)
        if (components.size() == 0) {
            inspectionResults.artifactsNotExtracted++
            logger.trace("Artifact can not currently be extracted: ${artifactName}")
        } else {
            if (components.size() == 1) {
                inspectionResults.singlesFound++
            } else {
                inspectionResults.multiplesFound++
            }
            inspectionResults.totalBdioNodesCreated += components.size()
            components.each {
                bdioWriter.write(it)
                logger.info("wrote ${artifactName}")
            }
        }
    }
}