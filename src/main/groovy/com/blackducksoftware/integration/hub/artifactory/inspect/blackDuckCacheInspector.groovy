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

import static com.blackducksoftware.integration.hub.artifactory.SupportedPackageType.*

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

import com.blackducksoftware.integration.exception.IntegrationException
import com.blackducksoftware.integration.hub.api.bom.BomImportService
import com.blackducksoftware.integration.hub.api.notification.NotificationService
import com.blackducksoftware.integration.hub.artifactory.ArtifactMetaData
import com.blackducksoftware.integration.hub.artifactory.ArtifactMetaDataManager
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig
import com.blackducksoftware.integration.hub.artifactory.BlackDuckProperty
import com.blackducksoftware.integration.hub.artifactory.DependencyFactory
import com.blackducksoftware.integration.hub.artifactory.PackageTypePatternManager
import com.blackducksoftware.integration.hub.artifactory.PluginProperty
import com.blackducksoftware.integration.hub.bdio.SimpleBdioFactory
import com.blackducksoftware.integration.hub.bdio.graph.MutableDependencyGraph
import com.blackducksoftware.integration.hub.bdio.model.Forge
import com.blackducksoftware.integration.hub.bdio.model.SimpleBdioDocument
import com.blackducksoftware.integration.hub.bdio.model.dependency.Dependency
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalId
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder
import com.blackducksoftware.integration.hub.dataservice.component.ComponentDataService
import com.blackducksoftware.integration.hub.dataservice.project.ProjectDataService
import com.blackducksoftware.integration.hub.dataservice.project.ProjectVersionWrapper
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.rest.ApiKeyRestConnection
import com.blackducksoftware.integration.hub.service.HubService
import com.blackducksoftware.integration.hub.service.HubServicesFactory
import com.blackducksoftware.integration.log.Slf4jIntLogger
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
@Field RepoPathFactory repoPathFactory

@Field List<String> repoKeysToInspect
@Field String dateTimePattern

initialize()

executions {
    /**
     * This will attempt to reload the properties file and initialize the inspector with the new values.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckReloadInspector"
     */
    blackDuckReloadInspector(httpMethod: 'POST') { params ->
        log.info('Starting blackDuckReloadInspector REST request...')

        initialize()

        log.info('...completed blackDuckReloadInspector REST request.')
    }


    blackDuckIdentifyArtifatsInRepository(httpMethod: 'POST') { params ->
        if (!params.containsKey('repoKey') || !params['repoKey'][0]) {
            message = 'You must provide a repoKey'
            return
        }
        HubServerConfigBuilder
        String repoKey = params['repoKey'][0]
        String patterns = packageTypePatternManager.getPattern(repositories.getRepositoryConfiguration(repoKey).getPackageType())

        createHubProject(repoKey, patterns)
    }

    blackDuckPopulateMetadataInRepository(httpMethod: 'POST') { params ->
        def repoKey = params['repoKey'][0]
        def projectName = getRepoProjectName(repoKey)
        def projectVersionName = getRepoProjectVersionName(repoKey)

        if (params.containsKey('projectName')) {
            projectName = params['projectName'][0]
        }
        if (params.containsKey('projectVersionName')) {
            projectVersionName = params['projectVersionName'][0]
        }

        updateFromHubProject(repoKey, projectName, projectVersionName);
    }

    blackDuckUpdateMetadataInRepository(httpMethod: 'POST') { params ->
        String today = new Date().format('yyyy-MM-dd')

        def repoKey = params['repoKey'][0]
        def projectName = getRepoProjectName(repoKey)
        def projectVersionName = getRepoProjectVersionName(repoKey)
        def startDateString = today
        def endDateString = today

        if (params.containsKey('projectName')) {
            projectName = params['projectName'][0]
        }
        if (params.containsKey('projectVersionName')) {
            projectVersionName = params['projectVersionName'][0]
        }
        if (params.containsKey('startDate')) {
            startDateString = params['startDate'][0]
        }
        if (params.containsKey('endDate')) {
            endDateString = params['endDate'][0]
        }

        Date startDate = getDateFromString("${startDateString}T00:00:00.000Z")
        Date endDate = getDateFromString("${endDateString}T23:59:59.999Z")

        updateFromHubProjectNotifications(repoKey, projectName, projectVersionName, startDate, endDate)
    }

    blackDuckDeleteInspectionPropertiesFromRepository(httpMethod: 'POST') { params ->
        log.info('Starting blackDuckDeleteInspectionPropertiesFromRepository REST request...')

        def repoKey = params['repoKey'][0]
        deleteInspectionProperties(repoKey)

        log.info('...completed blackDuckDeleteInspectionPropertiesFromRepository REST request.')
    }

    blackDuckDeleteInspectionProperties(httpMethod: 'POST') { params ->
        log.info('Starting blackDuckDeleteInspectionProperties REST request...')

        repoKeysToInspect.each { repoKey ->
            deleteInspectionProperties(repoKey)
        }

        log.info('...completed blackDuckDeleteInspectionProperties REST request.')
    }
}

jobs {
    blackDuckIdentifyArtifacts(cron: "0 0/1 * 1/1 * ?") {
        log.info('Starting blackDuckIdentifyArtifacts CRON job...')

        repoKeysToInspect.each { repoKey ->
            String patterns = packageTypePatternManager.getPattern(repositories.getRepositoryConfiguration(repoKey).getPackageType())

            createHubProject(repoKey, patterns)
        }

        log.info('...completed blackDuckIdentifyArtifacts CRON job.')
    }

    blackDuckPopulateMetadata(cron: "0 0/1 * 1/1 * ?") {
        log.info('Starting blackDuckPopulateMetadata CRON job...')

        repoKeysToInspect.each { repoKey ->
            String projectName = getRepoProjectName(repoKey)
            String projectVersionName = getRepoProjectVersionName(repoKey)

            updateFromHubProject(repoKey, projectName, projectVersionName)
        }

        log.info('...completed blackDuckPopulateMetadata CRON job.')
    }

    blackDuckUpdateMetadata(cron: "0 0/1 * 1/1 * ?") {
        log.info('Starting blackDuckUpdateMetadata CRON job...')

        repoKeysToInspect.each { repoKey ->
            RepoPath repoKeyPath = repoPathFactory.create(repoKey)
            String lastInspectedString = repositories.getProperty(repoKeyPath, BlackDuckProperty.INSPECTION_TIME.getName())

            if (lastInspectedString) {
                Date now = new Date()
                Date lastInspected = getDateFromString(lastInspectedString)
                String projectName = getRepoProjectName(repoKey)
                String projectVersionName = getRepoProjectVersionName(repoKey)

                updateFromHubProjectNotifications(repoKey, projectName, projectVersionName, lastInspected, now)
            }
        }

        log.info('...completed blackDuckUpdateMetadata CRON job.')
    }
}

storage {
    afterCreate { ItemInfo item ->
        String repoKey = item.getRepoKey()
        String packageType = repositories.getRepositoryConfiguration(repoKey).getPackageType()
        SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory()

        RepoPath repoPath = item.getRepoPath()
        String projectName = getRepoProjectName(repoKey)
        String projectVersionName = getRepoProjectVersionName(repoKey)

        try {
            String pattern = packageTypePatternManager.getPattern(packageType)
            String path = repoPath.toPath()
            if (FilenameUtils.wildcardMatch(path, pattern)) {
                Dependency dependency = createDependency(simpleBdioFactory, repoPath, packageType)
                if (null != dependency) {
                    addDependencyToProjectVersion(dependency, projectName, projectVersionName)
                    addDependencyProperties(repoPath, dependency)
                }
            }
        } catch (IntegrationException e) {
            log.error("Failed to add ${repoPath} to the project/version ${projectName}/${projectVersionName} in the hub", e)
        } catch (Exception e) {
            log.debug("Unexpected exception", e)
        }
    }
}

private void createHubProject(String repoKey, String patterns) {
    RepoPath repoKeyPath = repoPathFactory.create(repoKey)
    String inspectionStatus = repositories.getProperty(repoKeyPath, BlackDuckProperty.INSPECTION_STATUS.getName())

    if (!'SUCCESS'.equals(inspectionStatus)) {
        try {
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
                Dependency repoPathDependency = createDependency(repoPath, packageType);
                if (repoPathDependency != null) {
                    addDependencyProperties(repoPath, repoPathDependency)
                    mutableDependencyGraph.addChildToRoot(repoPathDependency)
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

            BomImportService bomImportService = hubServicesFactory.createBomImportService();
            bomImportService.importBomFile(bdioFile);

            repositories.setProperty(repoKeyPath, BlackDuckProperty.INSPECTION_STATUS.getName(), 'SUCCESS')
        } catch (Exception e) {
            repositories.setProperty(repoKeyPath, BlackDuckProperty.INSPECTION_STATUS.getName(), 'FAILURE')
            log.error("The blackDuckCacheInspector encountered an exception while identifying artifacts in repository ${repoKey}", e)
        }
    }
}

private void updateFromHubProject(String repoKey, String projectName, String projectVersionName) {
    RepoPath repoKeyPath = repoPathFactory.create(repoKey)
    String inspectionStatus = repositories.getProperty(repoKeyPath, BlackDuckProperty.INSPECTION_STATUS.getName())

    if ('SUCCESS'.equals(inspectionStatus)) {
        try {
            HubService hubService = hubServicesFactory.createHubService();
            ProjectDataService projectDataService = hubServicesFactory.createProjectDataService();

            ProjectVersionWrapper projectVersionWrapper = projectDataService.getProjectVersion(projectName, projectVersionName);
            List<ArtifactMetaData> artifactMetaDataList = artifactMetaDataManager.getMetaData(repoKey, hubService, projectVersionWrapper.getProjectVersionView());
            addOriginIdProperties(repoKey, artifactMetaDataList);

            String lastInspectedString = repositories.getProperty(repoKeyPath, BlackDuckProperty.INSPECTION_TIME.getName())
            if (!lastInspectedString) {
                String nowString = getNowString()
                repositories.setProperty(repoKeyPath, BlackDuckProperty.INSPECTION_TIME.getName(), nowString)
            }
        } catch (Exception e) {
            log.error("The blackDuckCacheInspector encountered an exception while populating artifact metadata in repository ${repoKey}", e)
        }
    }
}

private void updateFromHubProjectNotifications(String repoKey, String projectName, String projectVersionName, Date startDate, Date endDate) {
    RepoPath repoKeyPath = repoPathFactory.create(repoKey)
    String inspectionStatus = repositories.getProperty(repoKeyPath, BlackDuckProperty.INSPECTION_STATUS.getName())

    if ('SUCCESS'.equals(inspectionStatus)) {
        try {
            NotificationService notificationService = hubServicesFactory.createNotificationService()
            ProjectDataService projectDataService = hubServicesFactory.createProjectDataService();

            ProjectVersionWrapper projectVersionWrapper = projectDataService.getProjectVersion(projectName, projectVersionName);
            List<ArtifactMetaData> artifactMetaDataList = artifactMetaDataManager.getMetaDataFromNotifications(repoKey, notificationService, projectVersionWrapper.getProjectVersionView(), startDate, endDate)
            addOriginIdProperties(repoKey, artifactMetaDataList);

            String endString = getNowString()
            repositories.setProperty(repoKeyPath, BlackDuckProperty.INSPECTION_TIME.getName(), endString)
        } catch (Exception e) {
            log.error("The blackDuckCacheInspector encountered an exception while updating artifact metadata from Hub notifications in repository ${repoKey}", e)
        }
    }

}

private void deleteInspectionProperties(String repoKey) {
    BlackDuckProperty.values().each { blackDuckProperty ->
        SetMultimap<String,String> setMultimap = new HashMultimap<>();
        setMultimap.put(blackDuckProperty.getName(), '*');
        List<RepoPath> repoPathsWithProperty = searches.itemsByProperties(setMultimap, repoKey)
        repoPathsWithProperty.each { repoPath ->
            repositories.deleteProperty(repoPath, blackDuckProperty.getName())
        }
    }
}

private String getRepoProjectName(String repoKey) {
    RepoPath repoPath = RepoPathFactory.create(repoKey)
    String projectNameProperty = repositories.getProperty(repoPath, BlackDuckProperty.PROJECT_NAME.getName())
    if (StringUtils.isNotBlank(projectNameProperty)) {
        return projectNameProperty
    }
    return repoKey
}

private String getRepoProjectVersionName(String repoKey) {
    RepoPath repoPath = RepoPathFactory.create(repoKey)
    String projectVersionNameProperty = repositories.getProperty(repoPath, BlackDuckProperty.HUB_PROJECT_VERSION_NAME.getName())
    if (StringUtils.isNotBlank(projectVersionNameProperty)) {
        return projectVersionNameProperty
    }
    return InetAddress.getLocalHost().getHostName();
}

private void addDependencyToProjectVersion(Dependency dependency, String projectName, String projectVersionName) {
    HubServicesFactory hubServicesFactory = createHubServicesFactory();
    ComponentDataService componentDataService = hubServicesFactory.createComponentDataService();
    componentDataService.addComponentToProjectVersion(dependency.externalId, projectName, projectVersionName);
}

private void addDependencyProperties(RepoPath repoPath, Dependency dependency) {
    String hubOriginId = dependency.externalId.createHubOriginId()
    repositories.setProperty(repoPath, BlackDuckProperty.HUB_ORIGIN_ID.getName(), hubOriginId)
    String hubForge = dependency.externalId.forge.getName()
    repositories.setProperty(repoPath, BlackDuckProperty.HUB_FORGE.getName(), hubForge)
}

private void addOriginIdProperties(String repoKey, List<ArtifactMetaData> artifactMetaDataList) {
    artifactMetaDataList.each { artifactMetaData ->
        SetMultimap<String,String> setMultimap = new HashMultimap<>();
        setMultimap.put(BlackDuckProperty.HUB_ORIGIN_ID.getName(), artifactMetaData.originId);
        setMultimap.put(BlackDuckProperty.HUB_FORGE.getName(), artifactMetaData.forge);
        List<RepoPath> artifactsWithOriginId = searches.itemsByProperties(setMultimap, repoKey)
        artifactsWithOriginId.each { repoPath ->
            repositories.setProperty(repoPath, BlackDuckProperty.HIGH_VULNERABILITIES.getName(), Integer.toString(artifactMetaData.highSeverityCount))
            repositories.setProperty(repoPath, BlackDuckProperty.MEDIUM_VULNERABILITIES.getName(), Integer.toString(artifactMetaData.mediumSeverityCount))
            repositories.setProperty(repoPath, BlackDuckProperty.LOW_VULNERABILITIES.getName(), Integer.toString(artifactMetaData.lowSeverityCount))
            repositories.setProperty(repoPath, BlackDuckProperty.POLICY_STATUS.getName(), artifactMetaData.policyStatus)
        }
    }
}

private Dependency createDependency(RepoPath repoPath, String packageType) {
    try {
        FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath);
        org.artifactory.md.Properties properties = repositories.getProperties(repoPath);

        if (nuget.name().equals(packageType)) {
            return dependencyFactory.createNugetDependency(fileLayoutInfo, properties);
        }

        if (npm.name().equals(packageType)) {
            return dependencyFactory.createNpmDependency(fileLayoutInfo, properties);
        }

        if (pypi.name().equals(packageType)) {
            return dependencyFactory.createPyPiDependency(fileLayoutInfo, properties);
        }

        if (gems.name().equals(packageType)) {
            return dependencyFactory.createRubygemsDependency(fileLayoutInfo, properties);
        }

        if (maven.name().equals(packageType) || gradle.name().equals(packageType)) {
            return dependencyFactory.createMavenDependency(fileLayoutInfo, properties);
        }
    } catch (Exception e) {
        log.error("Could not resolve dependency:", e);
    }

    return null
}

private void initialize() {
    packageTypePatternManager = new PackageTypePatternManager()
    dependencyFactory = new DependencyFactory()
    artifactMetaDataManager = new ArtifactMetaDataManager()
    repoPathFactory = new RepoPathFactory()
    blackDuckArtifactoryConfig = new BlackDuckArtifactoryConfig()
    blackDuckArtifactoryConfig.setPluginsDirectory(new File(ctx.artifactoryHome.pluginsDir))

    loadProperties()
}

private void loadProperties() {
    def propertiesFile
    if (StringUtils.isNotBlank(propertiesFilePathOverride)) {
        propertiesFile = new File(propertiesFilePathOverride);
    } else {
        propertiesFile = new File(blackDuckArtifactoryConfig.pluginsLibDirectory, "${this.getClass().getSimpleName()}.properties")
    }

    try {
        blackDuckArtifactoryConfig.loadProperties(propertiesFile)
        packageTypePatternManager.setPattern(gems, blackDuckArtifactoryConfig.getProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_PATTERNS_RUBYGEMS))
        packageTypePatternManager.setPattern(maven, blackDuckArtifactoryConfig.getProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_PATTERNS_MAVEN))
        packageTypePatternManager.setPattern(gradle, blackDuckArtifactoryConfig.getProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_PATTERNS_GRADLE))
        packageTypePatternManager.setPattern(pypi, blackDuckArtifactoryConfig.getProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_PATTERNS_PYPI))
        packageTypePatternManager.setPattern(nuget, blackDuckArtifactoryConfig.getProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_PATTERNS_NUGET))
        packageTypePatternManager.setPattern(npm, blackDuckArtifactoryConfig.getProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_PATTERNS_NPM))
        dateTimePattern = blackDuckArtifactoryConfig.getProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_DATE_TIME_PATTERN)

        createHubServicesFactory()
        loadRepositoriesToInspect()
    } catch (Exception e) {
        log.error("Black Duck Cache Inspector encountered an unexpected error when trying to load its properties file at ${propertiesFile.getAbsolutePath()}", e)
    }
}

private void createHubServicesFactory() {
    HubServerConfig hubServerConfig = blackDuckArtifactoryConfig.hubServerConfig
    ApiKeyRestConnection apiKeyRestConnection = hubServerConfig.createApiKeyRestConnection(new Slf4jIntLogger(log))
    hubServicesFactory = new HubServicesFactory(apiKeyRestConnection)
}

private void loadRepositoriesToInspect() {
    String repositoriesToInspect = blackDuckArtifactoryConfig.getProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_REPOS)
    String repositoriesToInspectFilePath = blackDuckArtifactoryConfig.getProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_REPOS_CSV_PATH)
    repoKeysToInspect = []

    if (repositoriesToInspectFilePath) {
        def repositoryFile = new File(repositoriesToInspectFilePath)
        repositoryFile.splitEachLine(',') { repos ->
            repoKeysToInspect.addAll(repos)
        }
    } else if (repositoriesToInspect) {
        repoKeysToInspect.addAll(repositoriesToInspect.split(','))
    }
}

private String getNowString() {
    DateTime.now().withZone(DateTimeZone.UTC).toString(DateTimeFormat.forPattern(dateTimePattern).withZoneUTC())
}

private Date getDateFromString(String dateTimeString) {
    DateTime.parse(dateTimeString, DateTimeFormat.forPattern(dateTimePattern).withZoneUTC()).toDate()
}
