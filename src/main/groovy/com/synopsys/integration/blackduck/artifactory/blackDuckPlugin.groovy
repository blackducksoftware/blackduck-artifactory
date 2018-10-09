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
package com.synopsys.integration.blackduck.artifactory

import com.synopsys.integration.blackduck.artifactory.inspect.InspectionModule
import com.synopsys.integration.blackduck.artifactory.scan.ScanModule
import groovy.transform.Field
import org.artifactory.fs.ItemInfo

// propertiesFilePathOverride allows you to specify an absolute path to the blackDuckPlugin.properties file.
// If this is empty, we will default to ${ARTIFACTORY_HOME}/etc/plugins/lib/blackDuckPlugin.properties
@Field String propertiesFilePathOverride = ""
@Field PluginService pluginService
@Field ScanModule scanModule
@Field InspectionModule inspectionModule

initialize(TriggerType.STARTUP)

executions {
    //////////////////////////////////////////////// PLUGIN EXECUTIONS ////////////////////////////////////////////////

    /**
     * This will attempt to reload the properties file and initialize the plugin with the new values.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckReload"
     **/
    blackDuckReload(httpMethod: 'POST') { params ->
        initialize(TriggerType.REST_REQUEST)
    }

    /**
     * This will delete, then recreate, the blackducksoftware directory which includes the cli, the cron job log, as well as all the cli logs.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckReloadDirectory"
     **/
    blackDuckReloadDirectory() { params ->
        pluginService.reloadBlackDuckDirectory(TriggerType.REST_REQUEST)
    }

    //////////////////////////////////////////////// SCAN EXECUTIONS ////////////////////////////////////////////////

    /**
     * This will return a current status of the scan module's configuration configuration to verify things are setup properly.
     *
     * This can be triggered with the following curl command:
     * curl -X GET -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckTestConfig"
     **/
    blackDuckTestConfig(httpMethod: 'GET') { params ->
        message = scanModule.getStatusCheckMessage(TriggerType.REST_REQUEST)
    }

    /**
     * This will search your artifactory ARTIFACTORY_REPOS_TO_SEARCH repositories for the filename patterns designated in ARTIFACT_NAME_PATTERNS_TO_SCAN.
     * For example:
     *
     * ARTIFACTORY_REPOS_TO_SEARCH="my-releases,my-snapshots"
     * ARTIFACT_NAME_PATTERNS_TO_SCAN="*.war,*.zip"
     *
     * then this REST call will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files, scan them, and publish the BOM to the provided Hub server.
     *
     * The scanning process will add several properties to your artifacts in artifactory. Namely:
     *
     * blackDuckScanResult - SUCCESS or FAILURE, depending on the result of the scan
     * blackDuckScanTime - the last time a SUCCESS scan was completed
     * blackDuckScanCodeLocationUrl - the url for the code location created in the Hub
     *
     * The same functionality is provided via the scanForHub cron job to enable scheduled scans to run consistently.
     *
     * This can be triggered with the following curl command:
     * curl -X GET -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckScan"
     **/
    blackDuckScan(httpMethod: 'GET') { params ->
        scanModule.triggerScan(TriggerType.REST_REQUEST)
    }

    /**
     * This will search your artifactory ARTIFACTORY_REPOS_TO_SEARCH repositories for the filename patterns designated in ARTIFACT_NAME_PATTERNS_TO_SCAN.
     * For example:
     *
     * ARTIFACTORY_REPOS_TO_SEARCH="my-releases,my-snapshots"
     * ARTIFACT_NAME_PATTERNS_TO_SCAN="*.war,*.zip"
     *
     * then this REST call will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files and delete all the properties that the plugin sets.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteScanProperties"
     **/
    blackDuckDeleteScanProperties() { params ->
        scanModule.deleteScanProperties(TriggerType.REST_REQUEST)
    }

    /**
     * This will search your artifactory ARTIFACTORY_REPOS_TO_SEARCH repositories for the filename patterns designated in ARTIFACT_NAME_PATTERNS_TO_SCAN.
     * For example:
     *
     * ARTIFACTORY_REPOS_TO_SEARCH="my-releases,my-snapshots"
     * ARTIFACT_NAME_PATTERNS_TO_SCAN="*.war,*.zip"
     *
     * then this REST call will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files and checks for the 'blackduck.scanResult' property
     * if that property indicates a scan failure, it delete all the properties that the plugin set on it.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteScanPropertiesFromFailures"
     **/
    blackDuckDeleteScanPropertiesFromFailures() { params ->
        scanModule.deleteScanPropertiesFromFailures(TriggerType.REST_REQUEST)
    }

    /**
     * This will search your artifactory ARTIFACTORY_REPOS_TO_SEARCH repositories for the filename patterns designated in ARTIFACT_NAME_PATTERNS_TO_SCAN.
     * For example:
     *
     * ARTIFACTORY_REPOS_TO_SEARCH="my-releases,my-snapshots"
     * ARTIFACT_NAME_PATTERNS_TO_SCAN="*.war,*.zip"
     *
     * then this REST call will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files and checks for the 'blackduck.scanResult' property
     * if that property indicates a scan failure, it delete all the properties that the plugin set on it.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteScanPropertiesFromOutOfDate"
     **/
    blackDuckDeleteScanPropertiesFromOutOfDate() { params ->
        scanModule.deleteScanPropertiesFromOutOfDate(TriggerType.REST_REQUEST)
    }

    /**
     * This will search your artifactory ARTIFACTORY_REPOS_TO_SEARCH repositories for the filename patterns designated in ARTIFACT_NAME_PATTERNS_TO_SCAN and update the deprecated properties
     * For example:
     *
     * ARTIFACTORY_REPOS_TO_SEARCH="my-releases,my-snapshots"
     * ARTIFACT_NAME_PATTERNS_TO_SCAN="*.war,*.zip"
     *
     * then this REST call will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files and update all the properties that the plugin sets.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckScanUpdateDeprecatedProperties"
     **/
    blackDuckScanUpdateDeprecatedProperties() { params ->
        scanModule.updateDeprecatedProperties(TriggerType.REST_REQUEST)
    }

    //////////////////////////////////////////////// INSPECTOR EXECUTIONS ////////////////////////////////////////////////

    /**
     * Removes all properties that were populated by the inspector plugin on the repositories and artifacts that it was configured to inspect.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteInspectionProperties"
     **/
    blackDuckDeleteInspectionProperties(httpMethod: 'POST') { params ->
        inspectionModule.deleteInspectionProperties(TriggerType.REST_REQUEST)
    }

    /**
     * Manual execution of the Identify Artifacts step of inspection on a specific repository.
     * Automatic execution is performed by the blackDuckIdentifyArtifacts CRON job below.
     *
     * Identifies artifacts in the repository and populates identifying metadata on them for use by the Populate Metadata and Update Metadata
     * steps.
     *
     * Metadata populated on artifacts:
     * blackduck.hubForge
     * blackduck.hubOriginId
     *
     * Metadata populated on repositories:
     * blackduck.inspectionTime
     * blackduck.inspectionStatus
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckManuallyIdentifyArtifacts"
     **/
    blackDuckManuallyIdentifyArtifacts(httpMethod: 'POST') { params ->
        inspectionModule.identifyArtifacts(TriggerType.REST_REQUEST)
    }

    /**
     * Manual execution of the Populate Metadata step of inspection on a specific repository.
     * Automatic execution is performed by the blackDuckPopulateMetadata CRON job below.
     *
     * For each artifact that matches the configured patterns in the configured repositories, uses the pre-populated identifying metadata
     * to look up vulnerability metadata in the Hub, then populates that vulnerability metadata on the artifact in Artifactory.
     *
     * Metadata populated:
     * blackduck.highVulnerabilities
     * blackduck.mediumVulnerabilities
     * blackduck.lowVulnerabilities
     * blackduck.policyStatus
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckManuallyPopulateMetadata"
     **/
    blackDuckManuallyPopulateMetadata(httpMethod: 'POST') { params ->
        inspectionModule.populateMetadata(TriggerType.REST_REQUEST)
    }

    /**
     * Manual execution of the Identify Artifacts step of inspection on a specific repository.
     * Automatic execution is performed by the blackDuckIdentifyArtifacts CRON job below.
     *
     * For each artifact that matches the configured patterns in the configured repositories, checks for updates to that metadata in the Hub
     * since the last time the repository was inspected.
     *
     * Metadata updated on artifacts:
     * blackduck.hubForge
     * blackduck.hubOriginId
     *
     * Metadata updated on repositories:
     * blackduck.inspectionTime
     * blackduck.inspectionStatus
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckManuallyUpdateMetadata"
     **/
    blackDuckManuallyUpdateMetadata(httpMethod: 'POST') { params ->
        inspectionModule.updateMetadata(TriggerType.REST_REQUEST)
    }

    /**
     * Rename all deprecated properties that were populated by the inspector plugin on the repositories and artifacts that it was configured to inspect.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckUpdateDeprecatedProperties"
     **/
    blackDuckInspectionUpdateDeprecatedProperties(httpMethod: 'POST') { params ->
        inspectionModule.updateDeprecatedProperties(TriggerType.REST_REQUEST)
    }
}

jobs {
    //////////////////////////////////////////////// SCAN JOBS ////////////////////////////////////////////////

    /**
     * This will search your artifactory ARTIFACTORY_REPOS_TO_SEARCH repositories for the filename patterns designated in ARTIFACT_NAME_PATTERNS_TO_SCAN.
     * For example:
     *
     * ARTIFACTORY_REPOS_TO_SEARCH="my-releases,my-snapshots"
     * ARTIFACT_NAME_PATTERNS_TO_SCAN="*.war,*.zip"
     *
     * then this cron job will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files, scan them, and publish the BOM to the provided Hub server.
     *
     * The scanning process will add several properties to your artifacts in artifactory. Namely:
     *
     * blackduck.scanResult - SUCCESS or FAILURE, depending on the result of the scan
     * blackduck.scanTime - the last time a SUCCESS scan was completed
     * blackduck.uiUrl - the url directly to the scanned BOM in the Hub
     * blackduck.apiUrl - the api url for your project which is needed for further Hub REST calls.
     *
     * The same functionality is provided via the scanForHub execution to enable a one-time scan triggered via a REST call.
     **/
    blackDuckScan(cron: scanModule.getScanModuleConfig().getBlackDuckScanCron()) {
        scanModule.triggerScan(TriggerType.CRON_JOB)
    }

    blackDuckAddPolicyStatus(cron: scanModule.getScanModuleConfig().getBlackDuckAddPolicyStatusCron()) {
        scanModule.addPolicyStatus(TriggerType.CRON_JOB)
    }

    //////////////////////////////////////////////// INSPECTION JOBS ////////////////////////////////////////////////

    /**
     * Identifies artifacts in the repository and populates identifying metadata on them for use by the Populate Metadata and Update Metadata
     * steps.
     *
     * Metadata populated on artifacts:
     * blackduck.hubForge
     * blackduck.hubOriginId
     *
     * Metadata populated on repositories:
     * blackduck.inspectionTime
     * blackduck.inspectionStatus
     **/
    blackDuckIdentifyArtifacts(cron: inspectionModule.getInspectionModuleConfig().getBlackDuckIdentifyArtifactsCron()) {
        inspectionModule.identifyArtifacts(TriggerType.CRON_JOB)
    }

    /**
     * For each artifact that matches the configured patterns in the configured repositories, uses the pre-populated identifying metadata
     * to look up vulnerability metadata in the Hub, then populates that vulnerability metadata on the artifact in Artifactory.
     *
     * Metadata populated:
     * blackduck.highVulnerabilities
     * blackduck.mediumVulnerabilities
     * blackduck.lowVulnerabilities
     * blackduck.policyStatus
     **/
    blackDuckPopulateMetadata(cron: inspectionModule.getInspectionModuleConfig().getBlackDuckPopulateMetadataCron()) {
        inspectionModule.populateMetadata(TriggerType.CRON_JOB)
    }

    /**
     * For each artifact that matches the configured patterns in the configured repositories, checks for updates to that metadata in the Hub
     * since the last time the repository was inspected.
     *
     * Metadata updated on artifacts:
     * blackduck.hubForge
     * blackduck.hubOriginId
     *
     * Metadata updated on repositories:
     * blackduck.inspectionTime
     * blackduck.inspectionStatus
     **/
    blackDuckUpdateMetadata(cron: inspectionModule.getInspectionModuleConfig().getBlackDuckUpdateMetadataCron()) {
        inspectionModule.updateMetadata(TriggerType.CRON_JOB)
    }
}

//////////////////////////////////////////////// INSPECTION STORAGE ////////////////////////////////////////////////
storage {
    afterCreate { ItemInfo item ->
        inspectionModule.inspectItem(item, TriggerType.STORAGE_AFTER_CREATE)
    }
}

//////////////////////////////////////////////// POLICY ENFORCER ////////////////////////////////////////////////
//download {
//    beforeDownload { Request request, RepoPath repoPath ->
//        def policyStatus = repositories.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS.getName())
//        if (PolicySummaryStatusType.IN_VIOLATION.name().equals(policyStatus)) {
//            throw new CancelException("Black Duck Policy Enforcer has prevented the download of ${repoPath.toPath()} because it violates a policy in your Black Duck Hub.", 403)
//        }
//    }
//}

private void initialize(final TriggerType triggerType) {
    log.info("Initializing blackDuckPlugin from ${triggerType.getLogName()}...")

    final File etcDirectory = ctx.artifactoryHome.etcDir
    final File homeDirectory = ctx.artifactoryHome.homeDir
    final File pluginsDirectory = ctx.artifactoryHome.pluginsDir
    final String thirdPartyVersion = ctx?.versionProvider?.running?.versionName?.toString()

    final PluginConfig pluginConfig = new PluginConfig(homeDirectory, etcDirectory, pluginsDirectory, thirdPartyVersion, propertiesFilePathOverride)
    pluginService = new PluginService(pluginConfig, repositories, searches)
    pluginService.initializePlugin()
    scanModule = pluginService.createScanModule()
    inspectionModule = pluginService.createInspectionModule()

    log.info("... completed intialization of blackDuckPlugin from ${triggerType.getLogName()}")

    //    // INSPECTION
    //    blackDuckArtifactoryConfig = new BlackDuckArtifactoryConfig()
    //    blackDuckArtifactoryConfig.setPluginsDirectory(ctx.artifactoryHome.pluginsDir.toString())
    //    blackDuckArtifactoryConfig.setThirdPartyVersion(ctx?.versionProvider?.running?.versionName?.toString())
    //    blackDuckArtifactoryConfig.setPluginType(PluginType.INSPECTOR)
    //
    //    blackDuckArtifactoryConfig.loadProperties(propertiesFilePathOverride)
    //    blackDuckIdentifyArtifactsCron = blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.IDENTIFY_ARTIFACTS_CRON)
    //    blackDuckPopulateMetadataCron = blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.POPULATE_METADATA_CRON)
    //    blackDuckUpdateMetadataCron = blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.UPDATE_METADATA_CRON)
    //
    //    dateTimeManager = new DateTimeManager(blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.DATE_TIME_PATTERN))
    //    final ArtifactoryExternalIdFactory artifactoryExternalIdFactory = new ArtifactoryExternalIdFactory(new ExternalIdFactory())
    //    PackageTypePatternManager packageTypePatternManager = new PackageTypePatternManager()
    //    packageTypePatternManager.loadPatterns(blackDuckArtifactoryConfig)
    //    artifactoryPropertyService = new ArtifactoryPropertyService(repositories, searches, dateTimeManager)
    //    blackDuckConnectionService = new BlackDuckConnectionService(blackDuckArtifactoryConfig, artifactoryPropertyService, dateTimeManager)
    //
    //    final CacheInspectorService cacheInspectorService = new CacheInspectorService(blackDuckArtifactoryConfig, repositories, artifactoryPropertyService)
    //    final ArtifactMetaDataService artifactMetaDataService = new ArtifactMetaDataService(blackDuckConnectionService)
    //    artifactIdentificationService = new ArtifactIdentificationService(repositories, searches, packageTypePatternManager, artifactoryExternalIdFactory, artifactoryPropertyService, cacheInspectorService, blackDuckConnectionService)
    //    metadataPopulationService = new MetaDataPopulationService(artifactoryPropertyService, cacheInspectorService, artifactMetaDataService)
    //    metadataUpdateService = new MetaDataUpdateService(artifactoryPropertyService, cacheInspectorService, artifactMetaDataService, metadataPopulationService)
    //
    //    repoKeysToInspect = cacheInspectorService.getRepositoriesToInspect()
    //
    //    blackDuckConnectionService.phoneHome()
}

