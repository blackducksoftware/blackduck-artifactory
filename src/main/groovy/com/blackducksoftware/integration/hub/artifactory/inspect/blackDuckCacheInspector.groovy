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

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.artifactory.fs.FileLayoutInfo
import org.artifactory.fs.ItemInfo
import org.artifactory.repo.RepoPath
import org.artifactory.repo.RepoPathFactory
import org.artifactory.repo.RepositoryConfiguration
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

import com.blackducksoftware.integration.hub.artifactory.ArtifactMetaData
import com.blackducksoftware.integration.hub.artifactory.ArtifactMetaDataFromNotifications
import com.blackducksoftware.integration.hub.artifactory.ArtifactMetaDataManager
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryProperty
import com.blackducksoftware.integration.hub.artifactory.BlackDuckHubProperty
import com.blackducksoftware.integration.hub.bdio.SimpleBdioFactory
import com.blackducksoftware.integration.hub.bdio.graph.MutableDependencyGraph
import com.blackducksoftware.integration.hub.bdio.model.Forge
import com.blackducksoftware.integration.hub.bdio.model.SimpleBdioDocument
import com.blackducksoftware.integration.hub.bdio.model.dependency.Dependency
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalId
import com.blackducksoftware.integration.hub.configuration.HubServerConfig
import com.blackducksoftware.integration.hub.rest.RestConnection
import com.blackducksoftware.integration.hub.service.CodeLocationService
import com.blackducksoftware.integration.hub.service.HubService
import com.blackducksoftware.integration.hub.service.HubServicesFactory
import com.blackducksoftware.integration.hub.service.NotificationService
import com.blackducksoftware.integration.hub.service.PhoneHomeService
import com.blackducksoftware.integration.hub.service.ProjectService
import com.blackducksoftware.integration.hub.service.model.ProjectVersionWrapper
import com.blackducksoftware.integration.log.Slf4jIntLogger
import com.blackducksoftware.integration.phonehome.PhoneHomeRequestBody
import com.blackducksoftware.integration.util.IntegrationEscapeUtil
import com.google.common.collect.HashMultimap
import com.google.common.collect.SetMultimap

import groovy.transform.Field

// propertiesFilePathOverride allows you to specify an absolute path to the blackDuckCacheInspector.properties file.
// If this is empty, we will default to ${ARTIFACTORY_HOME}/etc/plugins/lib/blackduckCacheInspector.properties
@Field String propertiesFilePathOverride = ""

@Field BlackDuckArtifactoryConfig blackDuckArtifactoryConfig
@Field PackageTypePatternManager packageTypePatternManager
@Field DependencyFactory dependencyFactory
@Field ArtifactMetaDataManager artifactMetaDataManager
@Field HubServicesFactory hubServicesFactory

@Field List<String> repoKeysToInspect
@Field String dateTimePattern
@Field String blackDuckIdentifyArtifactsCron
@Field String blackDuckPopulateMetadataCron
@Field String blackDuckUpdateMetadataCron
@Field String blackDuckAddPendingArtifactsCron

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

        repoKeysToInspect.each { repoKey -> deleteInspectionProperties(repoKey) }

        log.info('...completed blackDuckDeleteInspectionProperties REST request.')
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

        identifyArtifacts()

        log.info('...completed blackDuckManuallyIdentifyArtifacts REST request.')
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

        populateMetadata()

        log.info('...completed blackDuckManuallyPopulateMetadata REST request.')
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

        updateMetadata()

        log.info('...completed blackDuckManuallyUpdateMetadata REST request.')
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

        identifyArtifacts()

        log.info('...completed blackDuckIdentifyArtifacts CRON job.')
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

        populateMetadata()

        log.info('...completed blackDuckPopulateMetadata CRON job.')
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

        updateMetadata()

        log.info('...completed blackDuckUpdateMetadata CRON job.')
    }

    /**
     * Completes the identification step of
     */
    blackDuckAddPendingArtifacts(cron: blackDuckAddPendingArtifactsCron) {
        log.info('Starting blackDuckAddPendingArtifacts CRON job...')

        resolvePendingArtifacts()

        log.info('...completed blackDuckAddPendingArtifacts CRON job.')
    }
}


storage {
    afterCreate { ItemInfo item ->
        try {
            String repoKey = item.getRepoKey()
            String packageType = repositories.getRepositoryConfiguration(repoKey).getPackageType()

            RepoPath repoPath = item.getRepoPath()
            String projectName = getRepoProjectName(repoKey)
            String projectVersionName = getRepoProjectVersionName(repoKey)

            String pattern = packageTypePatternManager.getPattern(packageType)
            String path = repoPath.toPath()
            if (FilenameUtils.wildcardMatch(path, pattern)) {
                FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath)
                org.artifactory.md.Properties properties = repositories.getProperties(repoPath)
                Optional<Dependency> optionalDependency = dependencyFactory.createDependency(log, packageType, fileLayoutInfo, properties);
                if (optionalDependency.isPresent()) {
                    Dependency constructedDependency = optionalDependency.get()
                    addDependencyProperties(repoPath, constructedDependency)
                    setInspectionStatus(repoPath, 'PENDING')
                }
            }
        } catch (Exception e) {
            log.debug("The blackDuckCacheInspector encountered an unexpected exception", e)
        }
    }
}

void identifyArtifacts() {
    repoKeysToInspect.each { repoKey ->
        String patterns = packageTypePatternManager.getPattern(repositories.getRepositoryConfiguration(repoKey).getPackageType())
        RepoPath repoKeyPath = RepoPathFactory.create(repoKey)
        String inspectionStatus = repositories.getProperty(repoKeyPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS.getName())

        if (StringUtils.isBlank(inspectionStatus)) {
            try {
                createHubProject(repoKey, patterns)
                setInspectionStatus(repoKeyPath, 'PENDING')
            } catch (Exception e) {
                setInspectionStatus(repoKeyPath, 'FAILURE')
                log.error("The blackDuckCacheInspector encountered an exception while identifying artifacts in repository ${repoKey}", e)
            }
        }
    }
}

void populateMetadata() {
    repoKeysToInspect.each { repoKey ->
        RepoPath repoKeyPath = RepoPathFactory.create(repoKey)
        String inspectionStatus = repositories.getProperty(repoKeyPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS.getName())

        if ('PENDING'.equals(inspectionStatus)) {
            try {
                String projectName = getRepoProjectName(repoKey)
                String projectVersionName = getRepoProjectVersionName(repoKey)

                populateFromHubProject(repoKey, projectName, projectVersionName)

                setInspectionStatus(repoKeyPath, 'SUCCESS')
            } catch (Exception e) {
                log.error("The blackDuckCacheInspector encountered an exception while populating artifact metadata in repository ${repoKey}", e)
                setInspectionStatus(repoKeyPath, 'FAILURE')
            }
        }
    }
}

void updateMetadata() {
    repoKeysToInspect.each { repoKey ->
        RepoPath repoKeyPath = RepoPathFactory.create(repoKey)
        String inspectionStatus = repositories.getProperty(repoKeyPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS.getName())

        if ('SUCCESS'.equals(inspectionStatus)) {
            try {
                Date now = new Date()
                Date dateToCheck
                if (StringUtils.isNotBlank(repositories.getProperty(repoKeyPath, BlackDuckArtifactoryProperty.LAST_UPDATE.getName()))) {
                    dateToCheck = getDateFromProperty(repoKeyPath, BlackDuckArtifactoryProperty.LAST_UPDATE.getName())
                } else {
                    dateToCheck = getDateFromProperty(repoKeyPath, BlackDuckArtifactoryProperty.LAST_INSPECTION.getName())
                }
                String projectName = getRepoProjectName(repoKey)
                String projectVersionName = getRepoProjectVersionName(repoKey)

                Date lastNotificationDate = updateFromHubProjectNotifications(repoKey, projectName, projectVersionName, dateToCheck, now)
                repositories.setProperty(repoKeyPath, BlackDuckArtifactoryProperty.UPDATE_STATUS.getName(), 'UP TO DATE')
                repositories.setProperty(repoKeyPath, BlackDuckArtifactoryProperty.LAST_UPDATE.getName(), getStringFromDate(lastNotificationDate))
            } catch (Exception e) {
                log.error("The blackDuckCacheInspector encountered an exception while updating artifact metadata from Hub notifications in repository ${repoKey}:", e)
                repositories.setProperty(repoKeyPath, BlackDuckArtifactoryProperty.UPDATE_STATUS.getName(), 'OUT OF DATE')
            }
        }
    }
}

void resolvePendingArtifacts() {
    repoKeysToInspect.each { repoKey ->
        RepoPath repoKeyPath = RepoPathFactory.create(repoKey)
        String repoInspectionStatus = repositories.getProperty(repoKeyPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS.getName())

        if ('SUCCESS'.equals(repoInspectionStatus)) {
            Set repoPaths = new HashSet<>()
            String patterns = packageTypePatternManager.getPattern(repositories.getRepositoryConfiguration(repoKey).getPackageType())
            def patternsToFind = patterns.tokenize(',')
            patternsToFind.each {
                List<RepoPath> searchResults = searches.artifactsByName(it, repoKey)
                repoPaths.addAll(searchResults)
            }

            RepositoryConfiguration repositoryConfiguration = repositories.getRepositoryConfiguration(repoKey);
            String packageType = repositoryConfiguration.getPackageType();
            String projectName = getRepoProjectName(repoKey)
            String projectVersionName = getRepoProjectVersionName(repoKey)

            repoPaths.each { repoPath ->
                String inspectionStatus = repositories.getProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS.getName())
                if (!'SUCCESS'.equals(inspectionStatus)) {
                    try {
                        FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath);
                        org.artifactory.md.Properties properties = repositories.getProperties(repoPath);
                        //TODO: we only *really* need the ExternalId here, but right now we are creating a whole Dependency
                        Optional<Dependency> optionalDependency = dependencyFactory.createDependency(log, packageType, fileLayoutInfo, properties);
                        if (optionalDependency.isPresent()) {
                            Dependency constructedDependency = optionalDependency.get()
                            String hubOriginId = repositories.getProperty(repoPath, BlackDuckArtifactoryProperty.HUB_ORIGIN_ID.getName());
                            String hubForge = repositories.getProperty(repoPath, BlackDuckArtifactoryProperty.HUB_FORGE.getName());
                            if (StringUtils.isBlank(hubOriginId) || StringUtils.isBlank(hubForge)) {
                                addDependencyProperties(repoPath, constructedDependency)
                                setInspectionStatus(repoPath, 'PENDING')
                            }
                            addDependencyToProjectVersion(constructedDependency, projectName, projectVersionName)
                            setInspectionStatus(repoPath, 'SUCCESS')
                        }
                    } catch (Exception e) {
                        setInspectionStatus(repoPath, 'FAILURE')
                        log.debug("The blackDuckCacheInspector could not successfully inspect ${repoPath}:", e)
                    }
                }
            }
        }
    }
}

private void createHubProject(String repoKey, String patterns) {
    Set repoPaths = new HashSet<>()
    def patternsToFind = patterns.tokenize(',')
    patternsToFind.each {
        List<RepoPath> searchResults = searches.artifactsByName(it, repoKey)
        repoPaths.addAll(searchResults)
    }

    RepositoryConfiguration repositoryConfiguration = repositories.getRepositoryConfiguration(repoKey);
    String packageType = repositoryConfiguration.getPackageType();
    SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
    MutableDependencyGraph mutableDependencyGraph = simpleBdioFactory.createMutableDependencyGraph();

    repoPaths.each { repoPath ->
        FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath);
        org.artifactory.md.Properties properties = repositories.getProperties(repoPath);
        Optional<Dependency> optionalDependency = dependencyFactory.createDependency(log, packageType, fileLayoutInfo, properties);
        if (optionalDependency.isPresent()) {
            Dependency constructedDependency = optionalDependency.get()
            addDependencyProperties(repoPath, constructedDependency)
            mutableDependencyGraph.addChildToRoot(constructedDependency)
        }
    }

    Forge artifactoryForge = new Forge('/', '/', 'artifactory');
    String projectName = getRepoProjectName(repoKey);
    String projectVersionName = getRepoProjectVersionName(repoKey);
    String codeLocationName = "${projectName}/${projectVersionName}/${packageType}"
    ExternalId projectExternalId = simpleBdioFactory.createNameVersionExternalId(artifactoryForge, projectName, projectVersionName)
    SimpleBdioDocument simpleBdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, projectName, projectVersionName, projectExternalId, mutableDependencyGraph)

    IntegrationEscapeUtil integrationEscapeUtil = new IntegrationEscapeUtil()
    File bdioFile = new File("/tmp/${integrationEscapeUtil.escapeForUri(codeLocationName)}");
    bdioFile.delete()
    simpleBdioFactory.writeSimpleBdioDocumentToFile(bdioFile, simpleBdioDocument);

    CodeLocationService codeLocationService = hubServicesFactory.createCodeLocationService();
    codeLocationService.importBomFile(bdioFile);

    repoPaths.each { repoPath ->
        setInspectionStatus(repoPath, 'SUCCESS')
    }

    phoneHome()
}

private void populateFromHubProject(String repoKey, String projectName, String projectVersionName) {
    HubService hubService = hubServicesFactory.createHubService();
    ProjectService projectDataService = hubServicesFactory.createProjectService();

    ProjectVersionWrapper projectVersionWrapper = projectDataService.getProjectVersion(projectName, projectVersionName);
    List<ArtifactMetaData> artifactMetaDataList = artifactMetaDataManager.getMetaData(repoKey, hubService, projectVersionWrapper.getProjectVersionView());
    addOriginIdProperties(repoKey, artifactMetaDataList);
}

private Date updateFromHubProjectNotifications(String repoKey, String projectName, String projectVersionName, Date startDate, Date endDate) {
    NotificationService notificationService = hubServicesFactory.createNotificationService()
    ProjectService projectDataService = hubServicesFactory.createProjectService();
    HubService hubService = hubServicesFactory.createHubService();

    ProjectVersionWrapper projectVersionWrapper = projectDataService.getProjectVersion(projectName, projectVersionName);
    ArtifactMetaDataFromNotifications artifactMetaDataFromNotifications = artifactMetaDataManager.getMetaDataFromNotifications(repoKey, hubService, notificationService, projectVersionWrapper.getProjectVersionView(), startDate, endDate)
    addOriginIdProperties(repoKey, artifactMetaDataFromNotifications.getArtifactMetaData());

    phoneHome()

    return artifactMetaDataFromNotifications.getLastNotificationDate();
}

private void deleteInspectionProperties(String repoKey) {
    BlackDuckArtifactoryProperty.values().each { blackDuckArtifactoryProperty ->
        SetMultimap<String,String> setMultimap = new HashMultimap<>();
        setMultimap.put(blackDuckArtifactoryProperty.getName(), '*');
        List<RepoPath> repoPathsWithProperty = searches.itemsByProperties(setMultimap, repoKey)
        repoPathsWithProperty.each { repoPath ->
            repositories.deleteProperty(repoPath, blackDuckArtifactoryProperty.getName())
        }
    }
}

private String getRepoProjectName(String repoKey) {
    String projectName;
    RepoPath repoPath = RepoPathFactory.create(repoKey)
    String projectNameProperty = repositories.getProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_NAME.getName())
    if (StringUtils.isNotBlank(projectNameProperty)) {
        projectName = projectNameProperty
    } else {
        projectName = repoKey
    }
    return projectName
}

private String getRepoProjectVersionName(String repoKey) {
    String projectVersionName;
    RepoPath repoPath = RepoPathFactory.create(repoKey)
    String projectVersionNameProperty = repositories.getProperty(repoPath, BlackDuckArtifactoryProperty.HUB_PROJECT_VERSION_NAME.getName())
    if (StringUtils.isNotBlank(projectVersionNameProperty)) {
        projectVersionName = projectVersionNameProperty
    } else {
        try {
            projectVersionName = InetAddress.getLocalHost().getHostName()
        } catch (UnknownHostException e) {
            projectVersionName = 'UNKNOWN_HOST'
        }
    }
    return projectVersionName
}

private void addDependencyToProjectVersion(Dependency dependency, String projectName, String projectVersionName) {
    ProjectService projectService = hubServicesFactory.createProjectService();
    projectService.addComponentToProjectVersion(dependency.externalId, projectName, projectVersionName);
}

private void addDependencyProperties(RepoPath repoPath, Dependency dependency) {
    String hubOriginId = dependency.externalId.createHubOriginId()
    repositories.setProperty(repoPath, BlackDuckArtifactoryProperty.HUB_ORIGIN_ID.getName(), hubOriginId)
    String hubForge = dependency.externalId.forge.getName()
    repositories.setProperty(repoPath, BlackDuckArtifactoryProperty.HUB_FORGE.getName(), hubForge)
}

private void addOriginIdProperties(String repoKey, List<ArtifactMetaData> artifactMetaDataList) {
    artifactMetaDataList.each { artifactMetaData ->
        if (StringUtils.isNotBlank(artifactMetaData.originId) && StringUtils.isNotBlank(artifactMetaData.forge)) {
            SetMultimap<String,String> setMultimap = new HashMultimap<>();
            setMultimap.put(BlackDuckArtifactoryProperty.HUB_ORIGIN_ID.getName(), artifactMetaData.originId);
            setMultimap.put(BlackDuckArtifactoryProperty.HUB_FORGE.getName(), artifactMetaData.forge);
            List<RepoPath> artifactsWithOriginId = searches.itemsByProperties(setMultimap, repoKey)
            artifactsWithOriginId.each { repoPath ->
                repositories.setProperty(repoPath, BlackDuckArtifactoryProperty.HIGH_VULNERABILITIES.getName(), Integer.toString(artifactMetaData.highSeverityCount))
                repositories.setProperty(repoPath, BlackDuckArtifactoryProperty.MEDIUM_VULNERABILITIES.getName(), Integer.toString(artifactMetaData.mediumSeverityCount))
                repositories.setProperty(repoPath, BlackDuckArtifactoryProperty.LOW_VULNERABILITIES.getName(), Integer.toString(artifactMetaData.lowSeverityCount))
                repositories.setProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS.getName(), artifactMetaData.policyStatus.toString())
                repositories.setProperty(repoPath, BlackDuckArtifactoryProperty.COMPONENT_VERSION_URL.getName(), artifactMetaData.componentVersionLink)
            }
        }
    }
}

private void initialize() {
    packageTypePatternManager = new PackageTypePatternManager()
    dependencyFactory = new DependencyFactory()
    artifactMetaDataManager = new ArtifactMetaDataManager(new Slf4jIntLogger(log))
    blackDuckArtifactoryConfig = new BlackDuckArtifactoryConfig()
    blackDuckArtifactoryConfig.setPluginsDirectory(ctx.artifactoryHome.pluginsDir.toString())

    loadProperties()
}

private void loadProperties() {
    final File propertiesFile
    if (StringUtils.isNotBlank(propertiesFilePathOverride)) {
        propertiesFile = new File(propertiesFilePathOverride);
    } else {
        propertiesFile = new File(blackDuckArtifactoryConfig.pluginsLibDirectory, "${this.getClass().getSimpleName()}.properties")
    }

    try {
        blackDuckArtifactoryConfig.loadProperties(propertiesFile)
        packageTypePatternManager.setPattern(SupportedPackageType.gems, blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.PATTERNS_RUBYGEMS))
        packageTypePatternManager.setPattern(SupportedPackageType.maven, blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.PATTERNS_MAVEN))
        packageTypePatternManager.setPattern(SupportedPackageType.gradle, blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.PATTERNS_GRADLE))
        packageTypePatternManager.setPattern(SupportedPackageType.pypi, blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.PATTERNS_PYPI))
        packageTypePatternManager.setPattern(SupportedPackageType.nuget, blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.PATTERNS_NUGET))
        packageTypePatternManager.setPattern(SupportedPackageType.npm, blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.PATTERNS_NPM))
        dateTimePattern = blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.DATE_TIME_PATTERN)
        blackDuckIdentifyArtifactsCron = blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.IDENTIFY_ARTIFACTS_CRON)
        blackDuckPopulateMetadataCron = blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.POPULATE_METADATA_CRON)
        blackDuckUpdateMetadataCron = blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.UPDATE_METADATA_CRON)
        blackDuckAddPendingArtifactsCron = blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.ADD_PENDING_ARTIFACTS_CRON)

        createHubServicesFactory()
        loadRepositoriesToInspect()
    } catch (Exception e) {
        log.error("Black Duck Cache Inspector encountered an unexpected error when trying to load its properties file at ${propertiesFile.getAbsolutePath()}")
        throw e
    }
}

private void createHubServicesFactory() {
    HubServerConfig hubServerConfig = blackDuckArtifactoryConfig.hubServerConfig
    final RestConnection restConnection;

    if (StringUtils.isNotBlank(blackDuckArtifactoryConfig.getProperty(BlackDuckHubProperty.API_TOKEN))) {
        restConnection = hubServerConfig.createApiTokenRestConnection(new Slf4jIntLogger(log));
    } else {
        restConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(log));
    }

    hubServicesFactory = new HubServicesFactory(restConnection)
}

private void loadRepositoriesToInspect() {
    String repositoriesToInspect = blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.REPOS)
    String repositoriesToInspectFilePath = blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.REPOS_CSV_PATH)
    repoKeysToInspect = []

    if (repositoriesToInspectFilePath) {
        def repositoryFile = new File(repositoriesToInspectFilePath)
        repositoryFile.splitEachLine(',') { repos ->
            repoKeysToInspect.addAll(repos)
        }
    } else if (repositoriesToInspect) {
        repoKeysToInspect.addAll(repositoriesToInspect.split(','))
    }

    List<String> invalidRepoKeys = []

    repoKeysToInspect.each { repoKey ->
        def repoKeyPath = RepoPathFactory.create(repoKey)
        def repositoryConfiguration = repositories.getRepositoryConfiguration(repoKey)
        if (!repositories.exists(repoKeyPath) || !repositoryConfiguration) {
            invalidRepoKeys.add(repoKey)
            log.warn("Black Duck Cache Inspector will ignore configured repository \'${repoKey}\': Repository was not found or is not a valid repository.")
        }
    }

    repoKeysToInspect.removeAll(invalidRepoKeys)
}

private String getNowString() {
    DateTime.now().withZone(DateTimeZone.UTC).toString(DateTimeFormat.forPattern(dateTimePattern).withZoneUTC())
}

private String getStringFromDate(Date date) {
    return new DateTime(date).withZone(DateTimeZone.UTC).toString(DateTimeFormat.forPattern(dateTimePattern).withZoneUTC())
}

private Date getDateFromProperty(RepoPath repoPath, String propertyName) {
    String lastInspectedString = repositories.getProperty(repoPath, BlackDuckArtifactoryProperty.LAST_INSPECTION.getName())
    return DateTime.parse(lastInspectedString, DateTimeFormat.forPattern(dateTimePattern).withZoneUTC()).toDate()
}

private Date getDateFromString(String dateTimeString) {
    DateTime.parse(dateTimeString, DateTimeFormat.forPattern(dateTimePattern).withZoneUTC()).toDate()
}

private void setInspectionStatus(RepoPath repoPath, String status) {
    repositories.setProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS.getName(), status)
    repositories.setProperty(repoPath, BlackDuckArtifactoryProperty.LAST_INSPECTION.getName(), getNowString())
}

private void phoneHome() {
    try {
        Optional<String> thirdPartyVersion = Optional.ofNullable(ctx?.versionProvider?.running?.versionName)
        Optional<String> pluginVersion = Optional.ofNullable(blackDuckArtifactoryConfig.getVersionFile()?.text)
        PhoneHomeService phoneHomeService = hubServicesFactory.createPhoneHomeService()
        PhoneHomeRequestBody.Builder phoneHomeRequestBodyBuilder = phoneHomeService.createInitialPhoneHomeRequestBodyBuilder('hub-artifactory', pluginVersion.orElse('UNKNOWN_VERSION'))
        phoneHomeRequestBodyBuilder.addToMetaData('artifactory.version', thirdPartyVersion.orElse('UNKNOWN_VERSION'))
        phoneHomeRequestBodyBuilder.addToMetaData('hub.artifactory.plugin', 'blackDuckCacheInspector')
        phoneHomeService.phoneHome(phoneHomeRequestBodyBuilder)
    } catch(Exception e) {
    }
}
