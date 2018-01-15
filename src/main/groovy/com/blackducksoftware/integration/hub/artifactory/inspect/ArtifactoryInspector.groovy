/*
 * hub-artifactory
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.artifactory.inspect

import java.time.LocalDateTime

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.artifactory.ArtifactoryRestClient
import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperties
import com.blackducksoftware.integration.hub.artifactory.inspect.extractor.ComponentExtractor
import com.blackducksoftware.integration.hub.bdio.BdioNodeFactory
import com.blackducksoftware.integration.hub.bdio.BdioPropertyHelper
import com.blackducksoftware.integration.hub.bdio.BdioWriter
import com.blackducksoftware.integration.hub.bdio.model.BdioProject
import com.google.gson.Gson

@Component
class ArtifactoryInspector {
    static final String PROJECT_VERSION_UI_URL_PROPERTY='blackduck.uiUrl'
    static final String POLICY_STATUS_PROPERTY='blackduck.policyStatus'
    static final String OVERALL_POLICY_STATUS_PROPERTY='blackduck.overallPolicyStatus'
    static final String INSPECTION_TIME_PROPERTY='blackduck.inspectionTime'
    static final String INSPECTION_STATUS_PROPERTY='blackduck.inspectionStatus'
    private final Logger logger = LoggerFactory.getLogger(ArtifactoryInspector.class)

    @Autowired
    BdioPropertyHelper bdioPropertyHelper

    @Autowired
    BdioNodeFactory bdioNodeFactory

    @Autowired
    ArtifactoryRestClient artifactoryRestClient

    @Autowired
    ComponentExtractor componentExtractor

    @Autowired
    HubProjectDetails hubProjectDetails

    @Autowired
    HubClient hubClient

    @Autowired
    ConfigurationProperties configurationProperties

    void performInspect() {
        hubClient.phoneHome()
        String repoKey = configurationProperties.hubArtifactoryInspectRepoKey
        try {
            def projectName = hubProjectDetails.hubProjectName
            def projectVersionName = hubProjectDetails.hubProjectVersionName
            def inspectionResults = new InspectionResults()
            def workingDirectory = new File(configurationProperties.hubArtifactoryWorkingDirectoryPath)
            def outputFile = new File(workingDirectory, "${projectName}_bdio.jsonld")
            logger.info("Starting bdio creation using file: ${outputFile.canonicalPath}")
            new BdioWriter(new Gson(), new FileOutputStream(outputFile)).withCloseable {
                def bdioBillOfMaterialsNode = bdioNodeFactory.createBillOfMaterials(null, projectName, projectVersionName)
                it.writeBdioNode(bdioBillOfMaterialsNode)

                def bdioProjectNode = new BdioProject()
                bdioProjectNode.id = outputFile.toURI().toString()
                bdioProjectNode.name = projectName
                bdioProjectNode.version = projectVersionName
                it.writeBdioNode(bdioProjectNode)

                walkFolderStructure('', it, inspectionResults)
            }

            logger.info("Completed BDIO file: ${outputFile.canonicalPath}")

            logger.trace("Total artifacts found: ${inspectionResults.totalArtifactsFound}")
            logger.info("Total attempts to extract a component: ${inspectionResults.totalExtractAttempts}")
            logger.info("Total BDIO component nodes created: ${inspectionResults.totalBdioNodesCreated}")
            logger.info("Count of artifacts that were extracted by only one extractor: ${inspectionResults.singlesFound}")
            logger.info("Count of artifacts that were extracted by MORE than one extractor: ${inspectionResults.multiplesFound}")
            logger.trace("Count of artifacts that were NOT extracted: ${inspectionResults.artifactsNotExtracted}")
            logger.info("Count of artifacts that were skipped because they are too old: ${inspectionResults.skippedArtifacts}")

            if (hubClient.isValid()) {
                hubClient.uploadBdioToHub(outputFile)
                logger.info("Uploaded BDIO to ${configurationProperties.hubUrl}")
                LocalDateTime dateTime = LocalDateTime.now()
                artifactoryRestClient.setPropertiesForPath(repoKey,  '', ["${INSPECTION_TIME_PROPERTY}": dateTime.toString()], false)
                logger.info('Inspection complete')
                logger.info("${repoKey} inspection timestamp updated (Now ${dateTime.toString()})")
                if(!Boolean.valueOf(configurationProperties.hubArtifactoryInspectSkipBomCalculation)){
                    logger.info('Waiting for BOM calculation to populate the properties for the corresponding Hub project in artifactory (this may take a while)...')
                    hubClient.waitForBomCalculation(projectName, projectVersionName)
                    logger.info('BOM calculation complete.')
                    String overallPolicyStatus = hubProjectDetails.getHubProjectOverallPolicyStatus(projectName, projectVersionName).toString()
                    logger.info("Hub Overall Policy Status: ${overallPolicyStatus}")
                    String policyStatus = hubProjectDetails.getHubProjectPolicyStatus(projectName, projectVersionName)
                    logger.info("Hub Policy Status: ${policyStatus}")
                    String uiUrl = hubProjectDetails.getHubProjectVersionUIUrl(projectName, projectVersionName)
                    logger.info("Hub UI URL: ${uiUrl}")
                    Map properties = [ "${PROJECT_VERSION_UI_URL_PROPERTY}" : uiUrl,
                        "${POLICY_STATUS_PROPERTY}" : policyStatus.replaceAll(',', '\\\\,'),
                        "${OVERALL_POLICY_STATUS_PROPERTY}" : overallPolicyStatus
                    ]
                    artifactoryRestClient.setPropertiesForPath(repoKey, '', properties, false)
                    logger.info("Updated Hub data for artifactory repository ${repoKey}")
                }
            }
            artifactoryRestClient.setPropertiesForPath(repoKey, '', ["${INSPECTION_STATUS_PROPERTY}": 'SUCCESS'], false)
        } catch(Exception e) {
            logger.error("Please investigate the inspection logs for details - the Black Duck Inspection did not complete successfully: ${e.message}", e)
            artifactoryRestClient.setPropertiesForPath(repoKey, '', ["${INSPECTION_STATUS_PROPERTY}": 'FAILURE'], false)
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
                bdioWriter.writeBdioNode(it)
                logger.info("wrote ${artifactName}")
            }
        }
    }
}