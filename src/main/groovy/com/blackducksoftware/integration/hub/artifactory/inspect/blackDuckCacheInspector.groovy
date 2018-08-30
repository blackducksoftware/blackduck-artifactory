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

import com.blackducksoftware.integration.hub.artifactory.ArtifactoryPropertyService
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig
import com.blackducksoftware.integration.hub.artifactory.DateTimeManager
import com.blackducksoftware.integration.hub.artifactory.HubConnectionService
import com.blackducksoftware.integration.hub.artifactory.inspect.ArtifactIdentificationService.IdentifiedArtifact
import com.blackducksoftware.integration.hub.artifactory.inspect.metadata.ArtifactMetaDataService
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalIdFactory
import embedded.org.apache.commons.lang3.StringUtils
import groovy.transform.Field
import org.artifactory.fs.ItemInfo
import org.artifactory.repo.RepoPath

// propertiesFilePathOverride allows you to specify an absolute path to the blackDuckCacheInspector.properties file.
// If this is empty, we will default to ${ARTIFACTORY_HOME}/etc/plugins/lib/blackduckCacheInspector.properties
@Field String propertiesFilePathOverride = ""

@Field BlackDuckArtifactoryConfig blackDuckArtifactoryConfig
@Field HubConnectionService hubConnectionService
@Field ArtifactoryPropertyService artifactoryPropertyService
@Field ArtifactIdentificationService artifactIdentificationService
@Field MetaDataPopulationService metadataPopulationService
@Field MetaDataUpdateService metadataUpdateService

@Field List<String> repoKeysToInspect
@Field String blackDuckIdentifyArtifactsCron
@Field String blackDuckPopulateMetadataCron
@Field String blackDuckUpdateMetadataCron

initialize()

executions {
    /**
     * Attempts to reload the properties file and initialize the inspector with the new values.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckReloadInspector"
     */
    blackDuckReloadInspector(httpMethod: 'POST') { params ->
        log.info('Starting blackDuckReloadInspector REST request...')

        initialize()

        log.info('...completed blackDuckReloadInspector REST request.')
    }

    /**
     * Removes all properties that were populated by the inspector plugin on the repositories and artifacts that it was configured to inspect.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteInspectionProperties"
     */
    blackDuckDeleteInspectionProperties(httpMethod: 'POST') { params ->
        log.info('Starting blackDuckDeleteInspectionProperties REST request...')

        repoKeysToInspect.each { repoKey -> artifactoryPropertyService.deleteAllBlackDuckPropertiesFrom(repoKey) }

        log.info('...completed blackDuckDeleteInspectionProperties REST request.')
        hubConnectionService.phoneHome()
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
     */
    blackDuckManuallyIdentifyArtifacts(httpMethod: 'POST') { params ->
        log.info('Starting blackDuckManuallyIdentifyArtifacts REST request...')

        repoKeysToInspect.each { repoKey -> artifactIdentificationService.identifyArtifacts(repoKey) }

        log.info('...completed blackDuckManuallyIdentifyArtifacts REST request.')
        hubConnectionService.phoneHome()
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
     */
    blackDuckManuallyPopulateMetadata(httpMethod: 'POST') { params ->
        log.info('Starting blackDuckManuallyPopulateMetadata REST request...')

        repoKeysToInspect.each { repoKey -> metadataPopulationService.populateMetadata(repoKey) }

        log.info('...completed blackDuckManuallyPopulateMetadata REST request.')
        hubConnectionService.phoneHome()
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
     */
    blackDuckManuallyUpdateMetadata(httpMethod: 'POST') { params ->
        log.info('Starting blackDuckManuallyUpdateMetadata REST request...')

        repoKeysToInspect.each { repoKey -> metadataUpdateService.updateMetadata(repoKey) }

        log.info('...completed blackDuckManuallyUpdateMetadata REST request.')
        hubConnectionService.phoneHome()
    }
}

jobs {
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
     */
    blackDuckIdentifyArtifacts(cron: blackDuckIdentifyArtifactsCron) {
        log.info('Starting blackDuckIdentifyArtifacts CRON job...')

        repoKeysToInspect.each { repoKey -> artifactIdentificationService.identifyArtifacts(repoKey) }

        log.info('...completed blackDuckIdentifyArtifacts CRON job.')
        hubConnectionService.phoneHome()
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
     */
    blackDuckPopulateMetadata(cron: blackDuckPopulateMetadataCron) {
        log.info('Starting blackDuckPopulateMetadata CRON job...')

        repoKeysToInspect.each { repoKey -> metadataPopulationService.populateMetadata(repoKey) }

        log.info('...completed blackDuckPopulateMetadata CRON job.')
        hubConnectionService.phoneHome()
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
     */
    blackDuckUpdateMetadata(cron: blackDuckUpdateMetadataCron) {
        log.info('Starting blackDuckUpdateMetadata CRON job...')

        repoKeysToInspect.each { repoKey -> metadataUpdateService.updateMetadata(repoKey) }

        log.info('...completed blackDuckUpdateMetadata CRON job.')
        hubConnectionService.phoneHome()
    }
}


storage {
    afterCreate { ItemInfo item ->
        try {
            String repoKey = item.getRepoKey()
            RepoPath repoPath = item.getRepoPath()
            String packageType = repositories.getRepositoryConfiguration(repoKey).getPackageType()

            if (repoKeysToInspect.contains(repoKey)) {
                Optional<Set<RepoPath>> identifiableArtifacts = artifactIdentificationService.getIdentifiableArtifacts(repoKey)
                if (identifiableArtifacts.isPresent() && identifiableArtifacts.get().contains(repoPath)) {
                    Optional<IdentifiedArtifact> optionalIdentifiedArtifact = artifactIdentificationService.identifyArtifact(repoPath, packageType)
                    if (optionalIdentifiedArtifact.isPresent()) {
                        artifactIdentificationService.populateIdMetadataOnIdentifiedArtifact(optionalIdentifiedArtifact.get())
                    }
                }
            }
        } catch (Exception e) {
            log.debug("The blackDuckCacheInspector encountered an unexpected exception", e)
        }
    }
}

// TODO: Utilize classes and methods created when abstracting scan logic
private void initialize() {
    blackDuckArtifactoryConfig = new BlackDuckArtifactoryConfig()
    blackDuckArtifactoryConfig.setPluginsDirectory(ctx.artifactoryHome.pluginsDir.toString())
    blackDuckArtifactoryConfig.setThirdPartyVersion(ctx?.versionProvider?.running?.versionName?.toString())
    blackDuckArtifactoryConfig.setPluginName('blackDuckCacheInspector')

    final File propertiesFile
    if (StringUtils.isNotBlank(propertiesFilePathOverride)) {
        propertiesFile = new File(propertiesFilePathOverride)
    } else {
        propertiesFile = new File(blackDuckArtifactoryConfig.pluginsLibDirectory, "${this.getClass().getSimpleName()}.properties")
    }

    try {
        blackDuckArtifactoryConfig.loadProperties(propertiesFile)
        blackDuckIdentifyArtifactsCron = blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.IDENTIFY_ARTIFACTS_CRON)
        blackDuckPopulateMetadataCron = blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.POPULATE_METADATA_CRON)
        blackDuckUpdateMetadataCron = blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.UPDATE_METADATA_CRON)


        DateTimeManager dateTimeManager = new DateTimeManager(blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.DATE_TIME_PATTERN))
        ArtifactoryExternalIdFactory artifactoryExternalIdFactory = new ArtifactoryExternalIdFactory(new ExternalIdFactory())
        PackageTypePatternManager packageTypePatternManager = new PackageTypePatternManager()
        packageTypePatternManager.loadPatterns(blackDuckArtifactoryConfig)
        artifactoryPropertyService = new ArtifactoryPropertyService(repositories, searches, dateTimeManager)
        hubConnectionService = new HubConnectionService(blackDuckArtifactoryConfig, artifactoryPropertyService, dateTimeManager)

        CacheInspectorService cacheInspectorService = new CacheInspectorService(blackDuckArtifactoryConfig, repositories, artifactoryPropertyService)
        ArtifactMetaDataService artifactMetaDataService = new ArtifactMetaDataService(hubConnectionService)
        artifactIdentificationService = new ArtifactIdentificationService(repositories, searches, packageTypePatternManager, artifactoryExternalIdFactory, artifactoryPropertyService, cacheInspectorService, hubConnectionService)
        metadataPopulationService = new MetaDataPopulationService(artifactoryPropertyService, cacheInspectorService, artifactMetaDataService)
        metadataUpdateService = new MetaDataUpdateService(artifactoryPropertyService, cacheInspectorService, artifactMetaDataService, metadataPopulationService)

        repoKeysToInspect = cacheInspectorService.getRepositoriesToInspect()
    } catch (Exception e) {
        log.error("Black Duck Cache Inspector encountered an unexpected error when trying to load its properties file at ${propertiesFile.getAbsolutePath()}")
        throw e
    }

    hubConnectionService.phoneHome()
}
