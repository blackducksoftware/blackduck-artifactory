/*
 * blackduck-artifactory
 *
 * Copyright (c) 2020 Synopsys, Inc.
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

import com.synopsys.integration.blackduck.artifactory.configuration.DirectoryConfig
import com.synopsys.integration.blackduck.artifactory.modules.PluginAPI
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsModule
import groovy.transform.Field
import org.artifactory.fs.ItemInfo
import org.artifactory.repo.RepoPath
import org.artifactory.request.Request

// propertiesFilePathOverride allows you to specify an absolute path to the blackDuckPlugin.properties file.
// If this is empty, we will default to ${ARTIFACTORY_HOME}/etc/plugins/lib/blackDuckPlugin.properties
@Field String propertiesFilePathOverride = ""
@Field PluginService pluginService
@Field PluginAPI pluginAPI

initialize(TriggerType.STARTUP)

executions {
    //////////////////////////////////////////////// PLUGIN EXECUTIONS ////////////////////////////////////////////////

    /**
     * This will attempt to reload the properties file and initialize the plugin with the new values.
     * Does not apply to CRON expressions. An Artifactory restart is required for changing cron expressions.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckReload"
     **/
    blackDuckReload(httpMethod: 'POST') { params -> initialize(TriggerType.REST_REQUEST)
    }

    /**
     * This will delete, then recreate, the blackducksoftware directory which includes the cli, the cron job log, as well as all the cli logs.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckReloadDirectory"
     **/
    blackDuckReloadDirectory() { params -> pluginService.reloadBlackDuckDirectory(TriggerType.REST_REQUEST)
    }

    /**
     * This will enabled or disable a particular module within the plugin and then reinitialize the plugin.
     * This endpoint requires parameters to be set. Each module has an enabled state of 'true' or 'false'
     * The names for the propertyReports available are:
     *      ScanModule
     *      InspectionModule
     *      PolicyModule
     *      AnalyticsModule
     *
     * NOTE: Enabling or disabling an endpoint via this endpoint is only persistent as long as the plugin is loaded.
     *       Set the state in the blackDuckPlugin.properties file to allow the state to survive reinitialization.
     *       All permanent changes made to configuration must be made in the blackDuckPlugin.properties file
     *
     * This can be triggered with the following curl command for setting the state of a module
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckSetModuleState?params=<moduleName>=<enabledState>"
     *
     * This can be triggered with the following curl command for enabling the InspectionModule:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckSetModuleState?params=InspectionModule=true"
     *
     * This can be triggered with the following curl command for disabling multiple propertyReports (ScanModule and the PolicyModule):
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckSetModuleState?params=ScanModule=false|PolicyModule=false"
     **/
    blackDuckSetModuleState() { params -> pluginAPI.setModuleState(TriggerType.REST_REQUEST, (Map<String, List<String>>) params)
    }

    /**
     * This will return a current status of the scan module's configuration configuration to verify things are setup properly.
     *
     * This can be triggered with the following curl command:
     * curl -X GET -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckTestConfig"
     **/
    blackDuckTestConfig(httpMethod: 'GET') { params -> message = pluginService.logStatusCheckMessage(TriggerType.REST_REQUEST)
    }

    //////////////////////////////////////////////// SCAN EXECUTIONS ////////////////////////////////////////////////

    /**
     * This will search your artifactory repositories defined with the "blackduck.artifactory.scan.repos" property for the filename patterns designated in the "blackduck.artifactory.scan.name.patterns" property
     * For example:
     *
     * blackduck.artifactory.scan.repos="my-releases,my-snapshots"
     * blackduck.artifactory.scan.name.patterns="*.war,*.zip"
     *
     * Then this REST call will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files, scan them, and publish the BOM to the provided Black Duck server.
     *
     * The scanning process will add several properties to your artifacts in Artifactory. Namely:
     *
     * blackduck.scanTime - the last time a SUCCESS scan was completed
     * blackduck.scanResult - SUCCESS or FAILURE, depending on the result of the scan
     * blackduck.projectName - the name of the project in Black Duck
     * blackduck.projectVersionName - the name of the project version in Black Duck
     *
     * The same functionality is provided via the blackDuckScan cron job to enable scheduled scans to run consistently.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckScan"
     **/
    blackDuckScan(httpMethod: 'POST') { params -> pluginAPI.triggerScan(TriggerType.REST_REQUEST)
    }

    /**
     * This will search your artifactory repositories defined with the "blackduck.artifactory.scan.repos" property for the filename patterns designated in the "blackduck.artifactory.scan.name.patterns" property
     * For example:
     *
     * blackduck.artifactory.scan.repos="my-releases,my-snapshots"
     * blackduck.artifactory.scan.name.patterns="*.war,*.zip"
     *
     * Then this REST call will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files, scan them, and publish the BOM to the provided Black Duck server.
     *
     * The addScanPolicyStatus process will add several properties to your artifacts in Artifactory. Namely:
     *
     * blackduck.uiUrl - the blackDuckUrl for the project version created in Black Duck
     * blackduck.policyStatusView - a short description of the policy status from Black Duck
     * blackduck.overallPolicyStatus - the overall policy status of artifact in Black Duck (ex. NOT_IN_VIOLATION)
     * blackduck.updateStatus - the status of policy updates (ex. OUT_OF_DATE implies the policyStatusView on this artifact is out of date)
     * blackduck.lastUpdate - the last time policy status was updated on this artifact
     *
     * The same functionality is provided via the blackDuckAddPolicyStatus cron job to enable scheduled policy status checks to run consistently.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckAddScanPolicyStatus"
     **/
    blackDuckAddScanPolicyStatus(httpMethod: 'POST') { params -> pluginAPI.addScanPolicyStatus(TriggerType.REST_REQUEST)
    }

    blackDuckPerformPostScanActions(httpMethod: 'POST') { params -> pluginAPI.performPostScanActions(TriggerType.REST_REQUEST)
    }

    /**
     * This will search your Artifactory repositories defined with the "blackduck.artifactory.scan.repos" property for the filename patterns designated in the "blackduck.artifactory.scan.name.patterns" property
     * It will then remove any and all blackduck properties from the artifact.
     * For example:
     *
     * blackduck.artifactory.scan.repos="my-releases,my-snapshots"
     * blackduck.artifactory.scan.name.patterns="*.war,*.zip"
     *
     * then this REST call will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files and delete all the properties that the plugin sets.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteScanProperties"
     *
     * To delete properties with property exclusions use the following curl command (the properties "blackduck.projectName" and "blackduck.projectVersionName" will not be removed from the artifact)
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteScanProperties?params=properties=blackduck.projectName,blackduck.projectVersionName"
     **/
    blackDuckDeleteScanProperties() { params -> pluginAPI.deleteScanProperties(TriggerType.REST_REQUEST, (Map<String, List<String>>) params)
    }

    /**
     * This will search your Artifactory repositories defined with the "blackduck.artifactory.scan.repos" property for the filename patterns designated in the "blackduck.artifactory.scan.name.patterns" property
     * For example:
     *
     * blackduck.artifactory.scan.repos="my-releases,my-snapshots"
     * blackduck.artifactory.scan.name.patterns="*.war,*.zip"
     *
     * then this REST call will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files and checks for the 'blackduck.scanResult' property
     * if that property indicates a scan failure, it delete all the properties that the plugin set on it.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteScanPropertiesFromFailures"
     *
     * To delete properties with property exclusions use the following curl command (the properties "blackduck.projectName" and "blackduck.projectVersionName" will not be removed from the artifact)
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteScanPropertiesFromFailures?params=properties=blackduck.projectName,blackduck.projectVersionName"
     **/
    blackDuckDeleteScanPropertiesFromFailures() { params -> pluginAPI.deleteScanPropertiesFromFailures(TriggerType.REST_REQUEST, (Map<String, List<String>>) params)
    }

    /**
     * This will search your Artifactory repositories defined with the "blackduck.artifactory.scan.repos" property for the filename patterns designated in the "blackduck.artifactory.scan.name.patterns" property
     * For example:
     *
     * blackduck.artifactory.scan.repos="my-releases,my-snapshots"
     * blackduck.artifactory.scan.name.patterns="*.war,*.zip"
     *
     * then this REST call will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files and checks for the 'blackduck.scanResult' property
     * if that property indicates the policy information is out of date, it delete all the properties that the plugin set on it.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteScanPropertiesFromOutOfDate"
     *
     * To delete properties with property exclusions use the following curl command (the properties "blackduck.projectName" and "blackduck.projectVersionName" will not be removed from the artifact)
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteScanPropertiesFromOutOfDate?params=properties=blackduck.projectName,blackduck.projectVersionName"
     **/
    blackDuckDeleteScanPropertiesFromOutOfDate() { params -> pluginAPI.deleteScanPropertiesFromOutOfDate(TriggerType.REST_REQUEST, (Map<String, List<String>>) params)
    }

    //////////////////////////////////////////////// INSPECTOR EXECUTIONS ////////////////////////////////////////////////
    /**
     * Manual execution of the Repository Initialization step of inspection.
     * Automatic execution is performed by the blackDuckInitializeRepos CRON job below.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckInitializeRepositories"
     **/
    blackDuckInitializeRepositories() { params -> pluginAPI.initializeRepositories(TriggerType.REST_REQUEST)
    }

    /**
     * Removes all properties that were populated by the inspector plugin on the repositories and artifacts that it was configured to inspect.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteInspectionProperties"
     *
     * To delete properties with property exclusions use the following curl command (the property "blackduck.inspectionStatus" will not be removed from the artifact)
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteInspectionProperties?params=properties=blackduck.inspectionStatus"
     **/
    blackDuckDeleteInspectionProperties(httpMethod: 'POST') { params -> pluginAPI.deleteInspectionProperties(TriggerType.REST_REQUEST, (Map<String, List<String>>) params)
    }

    /**
     * Removes all properties that were populated by the inspector plugin on the repositories and artifacts that it was configured to inspect where the blackduck.updateStatus=OUT_OF_DATE.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteInspectionPropertiesFromOutOfDate"
     *
     * To delete properties with property exclusions use the following curl command (the property "blackduck.inspectionStatus" will not be removed from the artifact)
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteInspectionPropertiesFromOutOfDate?params=properties=blackduck.inspectionStatus"
     **/
    blackDuckDeleteInspectionPropertiesFromOutOfDate() { params -> pluginAPI.deleteInspectionPropertiesFromOutOfDate(TriggerType.REST_REQUEST, (Map<String, List<String>>) params)
    }

    /**
     * Removes all properties that were populated by the inspector plugin on the repositories and artifacts that it was configured to inspect that have the property 'blackduck.inspectionStatus' set to 'FAILURE'.
     * Additionally attempts to re-inspect the artifacts it removed properties from
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckReinspectFromFailures"
     *
     * To delete properties with property exclusions use the following curl command (the property "blackduck.originId" will not be removed from the artifact)
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckReinspectFromFailures?params=properties=blackduck.projectName,blackduck.projectVersionName"
     **/
    blackDuckReinspectFromFailures() { params -> pluginAPI.reinspectFromFailures(TriggerType.REST_REQUEST, (Map<String, List<String>>) params)
    }

    /**
     * Manual execution of the Identify Artifacts step of inspection on a specific repository.
     * Automatic execution is performed by the blackDuckInspectAllUnknownArtifacts CRON job below.
     *
     * Identifies artifacts in the repository and populates identifying notifications on them for use by the Populate Metadata and Update Metadata
     * steps.
     *
     * Metadata populated on artifacts:
     * blackduck.forge
     * blackduck.originId
     * blackduck.highVulnerabilities
     * blackduck.mediumVulnerabilities
     * blackduck.lowVulnerabilities
     * blackduck.componentVersionUrl
     * blackduck.policyStatus
     *
     * Metadata populated on repositories:
     * blackduck.inspectionTime
     * blackduck.inspectionStatus
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckManuallyInspectAllUnknownArtifacts"
     **/
    blackDuckManuallyInspectAllUnknownArtifacts(httpMethod: 'POST') { params -> pluginAPI.inspectAllUnknownArtifacts(TriggerType.REST_REQUEST)
    }

    /**
     * Manual execution of the Update Metadata step of inspection on a specific repository.
     * Automatic execution is performed by the blackDuckInspectAllUnknownArtifacts CRON job below.
     *
     * For each artifact that matches the configured patterns in the configured repositories, checks for updates to that notifications in Black Duck
     * since the last time the repository was inspected.
     *
     * Metadata updated on artifacts:
     * blackduck.forge
     * blackduck.originId

     *
     * Metadata updated on repositories:
     * blackduck.inspectionTime
     * blackduck.inspectionStatus
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckManuallyUpdateMetadata"
     **/
    blackDuckManuallyUpdateMetadata(httpMethod: 'POST') { params -> pluginAPI.updateMetadata(TriggerType.REST_REQUEST)
    }

    /**
     * Manual execution of the Update PerformPolicySeverityUpdate cron job.
     * Will update the blackduck.policySeverityTypes property on all artifacts within inspected repos with the following properties:
     *
     * blackduck.inspectionStatus = SUCCESS
     * blackduck.policyStatus = IN_VIOLATION
     * OR
     * blackduck.inspectionStatus = SUCCESS
     * blackduck.policyStatus = IN_VIOLATION_OVERRIDDEN
     *
     * Note: The blackduck.policySeverityTypes property can fall out of date if the severity of a policy is changed.
     * There is a cron job which runs at the same interval as the blackduck.artifactory.inspect.reinspect.cron cron config property.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckManuallyPerformPolicySeverityUpdate"
     **/
    blackDuckManuallyPerformPolicySeverityUpdate(httpMethod: 'POST') { params -> pluginAPI.performPolicySeverityUpdate(TriggerType.REST_REQUEST)
    }

    //////////////////////////////////////////////// ANALYTICS EXECUTIONS ////////////////////////////////////////////////

    /**
     * Submits usage analytics. This endpoint is intended for developer use only
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckSubmitAnalytics"*/
    blackDuckSubmitAnalytics(httpMethod: 'POST') { params -> message = pluginAPI.submitAnalytics(TriggerType.REST_REQUEST)
    }
}

jobs {
    //////////////////////////////////////////////// SCAN JOBS ////////////////////////////////////////////////

    /**
     * The functionality is described above the blackDuckScan execution
     **/
    blackDuckScan(cron: pluginAPI.getScanCron()) {
        pluginAPI.triggerScan(TriggerType.CRON_JOB)
    }

    blackDuckAddScanPolicyStatus(cron: pluginAPI.getScanCron()) {
        pluginAPI.addScanPolicyStatus(TriggerType.CRON_JOB)
    }

    blackDuckPerformPostScanActions(cron: pluginAPI.getScanCron()) {
        pluginAPI.performPostScanActions(TriggerType.CRON_JOB)
    }

    //////////////////////////////////////////////// INSPECTION JOBS ////////////////////////////////////////////////

    blackDuckInitializeRepos(cron: pluginAPI.getInspectionCron()) {
        pluginAPI.initializeRepositories(TriggerType.CRON_JOB)
    }

    /**
     * The functionality is described above the blackDuckManuallyInspectAllUnknownArtifacts execution
     **/
    blackDuckInspectAllUnknownArtifacts(cron: pluginAPI.getInspectionCron()) {
        pluginAPI.inspectAllUnknownArtifacts(TriggerType.CRON_JOB)
    }

    /**
     * The functionality is described above the blackDuckManuallyUpdateMetadata execution
     **/
    blackDuckUpdateMetadata(cron: pluginAPI.getInspectionCron()) {
        pluginAPI.updateMetadata(TriggerType.CRON_JOB)
    }

    /**
     * The functionality is described above the blackDuckReinspectFromFailures execution*/
    blackDuckReinspectFromFailures(cron: pluginAPI.getReinspectCron()) {
        pluginAPI.reinspectFromFailures(TriggerType.CRON_JOB)
    }

    /**
     * The functionality is described above the blackDuckManuallyPerformPolicySeverityUpdate execution*/
    blackDuckPerformPolicySeverityUpdate(cron: pluginAPI.getReinspectCron()) {
        pluginAPI.performPolicySeverityUpdate(TriggerType.CRON_JOB)
    }

    //////////////////////////////////////////////// ANALYTICS JOBS ////////////////////////////////////////////////

    /**
     * Submits usage analytics. For developer use only
     **/
    blackDuckSubmitAnalytics(cron: AnalyticsModule.SUBMIT_ANALYTICS_CRON) {
        pluginAPI.submitAnalytics(TriggerType.CRON_JOB)
    }
}

storage {
    //////////////////////////////////////////////// INSPECTION STORAGE ////////////////////////////////////////////////

    /**
     * Handle after create events.
     *
     * Closure parameters:
     * item (org.artifactory.fs.ItemInfo) - the original item being created.*/
    afterCreate { ItemInfo item -> pluginAPI.handleAfterCreateEvent(item, TriggerType.STORAGE_AFTER_CREATE)
    }

    /**
     * Handle after copy events.
     *
     * Closure parameters:
     * item (org.artifactory.fs.ItemInfo) - the source item copied.
     * targetRepoPath (org.artifactory.repo.RepoPath) - the target repoPath for the copy.*/
    afterCopy { ItemInfo item, RepoPath targetRepoPath, properties -> pluginAPI.handleAfterCopyEvent(targetRepoPath, TriggerType.STORAGE_AFTER_COPY)
    }

    /**
     * Handle after move events.
     *
     * Closure parameters:
     * item (org.artifactory.fs.ItemInfo) - the source item moved.
     * targetRepoPath (org.artifactory.repo.RepoPath) - the target repoPath for the move.*/
    afterMove { ItemInfo item, RepoPath targetRepoPath, properties -> pluginAPI.handleAfterMoveEvent(targetRepoPath, TriggerType.STORAGE_AFTER_MOVE)
    }
}

download {
    //////////////////////////////////////////////// POLICY ENFORCER ////////////////////////////////////////////////

    beforeDownload { Request request, RepoPath repoPath ->
        pluginAPI.handleBeforeDownloadEventPolicy(TriggerType.BEFORE_DOWNLOAD, repoPath)
        pluginAPI.handleBeforeDownloadEventInspection(TriggerType.BEFORE_DOWNLOAD, repoPath)
    }
}

private void initialize(final TriggerType triggerType) {
    log.info("Initializing blackDuckPlugin from ${triggerType.getLogName()}...")

    final File etcDirectory = ctx.artifactoryHome.etcDir
    final File homeDirectory = ctx.artifactoryHome.homeDir
    final File pluginsDirectory = ctx.artifactoryHome.pluginsDir
    final String thirdPartyVersion = ctx?.versionProvider?.running?.versionName?.toString()
    final DirectoryConfig pluginConfig = DirectoryConfig.createDefault(homeDirectory, etcDirectory, pluginsDirectory, thirdPartyVersion, propertiesFilePathOverride)

    pluginService = new PluginService(pluginConfig, repositories, searches)
    pluginAPI = pluginService.initializePlugin()

    log.info("... completed intialization of blackDuckPlugin from ${triggerType.getLogName()}")
}

