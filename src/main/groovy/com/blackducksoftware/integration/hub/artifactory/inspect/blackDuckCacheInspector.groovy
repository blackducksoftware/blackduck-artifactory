package com.blackducksoftware.integration.hub.artifactory.inspect

import org.apache.commons.lang.StringUtils
import org.artifactory.addon.pypi.*
import org.artifactory.fs.FileLayoutInfo
import org.artifactory.fs.ItemInfo
import org.artifactory.md.Properties
import org.artifactory.repo.RepoPath
import org.artifactory.repo.RepoPathFactory
import org.artifactory.repo.RepositoryConfiguration
import org.springframework.context.ApplicationContext

import com.blackducksoftware.integration.hub.api.bom.BomImportService
import com.blackducksoftware.integration.hub.api.view.MetaHandler
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
import com.blackducksoftware.integration.hub.model.view.VersionBomComponentView
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection
import com.blackducksoftware.integration.hub.service.HubService
import com.blackducksoftware.integration.hub.service.HubServicesFactory
import com.blackducksoftware.integration.log.Slf4jIntLogger
import com.blackducksoftware.integration.util.IntegrationEscapeUtil
import com.google.common.collect.HashMultimap
import com.google.common.collect.SetMultimap

import groovy.transform.Field

@Field final String HUB_URL="h"
@Field final int HUB_TIMEOUT=120
@Field final String HUB_USERNAME=""
@Field final String HUB_PASSWORD=""

@Field final String HUB_PROXY_HOST=""
@Field final int HUB_PROXY_PORT=0
@Field final String HUB_PROXY_USERNAME=""
@Field final String HUB_PROXY_PASSWORD=""

@Field final boolean HUB_ALWAYS_TRUST_CERTS=true

@Field final String RUBYGEMS_PACKAGE = 'gems'
@Field final String MAVEN_PACKAGE = 'maven'
@Field final String GRADLE_PACKAGE = 'gradle'
@Field final String PYPI_PACKAGE = 'pypi'
@Field final String NUGET_PACKAGE = 'nuget'
@Field final String NPM_PACKAGE = 'npm'

@Field final String RUBYGEMS_PATTERNS = '*.gem'
@Field final String MAVEN_PATTERNS = '*.jar'
@Field final String GRADLE_PATTERNS = '*.jar'
@Field final String PYPI_PATTERNS = '*.whl,*.tar.gz,*.zip,*.egg'
@Field final String NUGET_PATTERNS = '*.nupkg'
@Field final String NPM_PATTERNS = '*.tgz'

@Field final String HUB_ORIGIN_ID_PROPERTY_NAME = 'blackduck.hubOriginId'
@Field final String HUB_PROJECT_NAME_PROPERTY_NAME = 'blackduck.hubProjectName'
@Field final String HUB_PROJECT_VERSION_NAME_PROPERTY_NAME = 'blackduck.hubProjectVersionName'
@Field final String HUB_COMPONENT_VERSION_LINK_PROPERTY_NAME = 'blackduck.hubComponentVersionLink'
@Field final String HIGH_VULNERABILITIES_PROPERTY_NAME = 'blackduck.highVulnerabilities'
@Field final String MEDIUM_VULNERABILITIES_PROPERTY_NAME = 'blackduck.mediumVulnerabilities'
@Field final String LOW_VULNERABILITIES_PROPERTY_NAME = 'blackduck.lowVulnerabilities'

executions {
    inspectRepository(httpMethod: 'POST') { params ->
        if (!params.containsKey('repoKey') || !params['repoKey'][0]) {
            message = 'You must provide a repoKey'
            return
        }
        String repoKey = params['repoKey'][0]
        String patterns = getPatternsFromPackageType(repositories.getRepositoryConfiguration(repoKey).getPackageType())

        createHubProject(repoKey, patterns)
    }

    updateInspectedRepository(httpMethod: 'POST') { params ->
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
}

storage {
    afterCreate { ItemInfo item ->
        if (!item.isFolder()) {
            RepoPath repoPath = item.getRepoPath()
            String repoKey = item.getRepoKey()
            String packageType = repositories.getRepositoryConfiguration(repoKey).getPackageType()
            SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory()
            String projectName = getRepoProjectName(repoKey)
            String projectVersionName = getRepoProjectVersionName(repoKey)

            try {
                Dependency repoPathDependency = createDependency(simpleBdioFactory, repoPath, packageType)
                if (repoPathDependency != null) {
                    HubServicesFactory hubServicesFactory = createHubServicesFactory();
                    ComponentDataService componentDataService = hubServicesFactory.createComponentDataService();
                    componentDataService.addComponentToProjectVersion(repoPathDependency.externalId, projectName, projectVersionName);

                    String hubOriginId = repoPathDependency.externalId.createHubOriginId()
                    repositories.setProperty(repoPath, HUB_ORIGIN_ID_PROPERTY_NAME, hubOriginId)
                }
            } catch (Exception e) {
                log.debug("Failed to add ${repoPath} to the project/version ${projectName}/${projectVersionName} in the hub: ${e.getMessage()}")
            }
        }
    }
}

private String getRepoProjectName(String repoKey) {
    RepoPath repoPath = RepoPathFactory.create(repoKey)
    String projectNameProperty = repositories.getProperty(repoPath, HUB_PROJECT_NAME_PROPERTY_NAME)
    if (StringUtils.isNotBlank(projectNameProperty)) {
        return projectNameProperty
    }
    return repoKey
}

private String getRepoProjectVersionName(String repoKey) {
    RepoPath repoPath = RepoPathFactory.create(repoKey)
    String projectVersionNameProperty = repositories.getProperty(repoPath, HUB_PROJECT_VERSION_NAME_PROPERTY_NAME)
    if (StringUtils.isNotBlank(projectVersionNameProperty)) {
        return projectVersionNameProperty
    }
    return InetAddress.getLocalHost().getHostName();
}

private void createHubProject(String repoKey, String patterns) {
    Set repoPaths = new HashSet<>()
    def patternsToFind = patterns.tokenize(',')
    patternsToFind.each {
        repoPaths.addAll(searches.artifactsByName(it, repoKey))
    }

    HubServicesFactory hubServicesFactory = createHubServicesFactory();
    RepositoryConfiguration repositoryConfiguration = repositories.getRepositoryConfiguration(repoKey);
    String packageType = repositoryConfiguration.getPackageType();
    SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
    MutableDependencyGraph mutableDependencyGraph = simpleBdioFactory.createMutableDependencyGraph();

    repoPaths.each { repoPath ->
        Dependency repoPathDependency = createDependency(simpleBdioFactory, repoPath, packageType);
        if (repoPathDependency != null) {
            String hubOriginId = repoPathDependency.externalId.createHubOriginId()
            repositories.setProperty(repoPath, HUB_ORIGIN_ID_PROPERTY_NAME, hubOriginId)
            mutableDependencyGraph.addChildToRoot(repoPathDependency);
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
}

private String getPatternsFromPackageType(String packageType) {
    if (RUBYGEMS_PACKAGE.equals(packageType)) {
        return RUBYGEMS_PATTERNS
    }
    if (MAVEN_PACKAGE.equals(packageType)) {
        return MAVEN_PATTERNS
    }
    if (GRADLE_PACKAGE.equals(packageType)) {
        return GRADLE_PATTERNS
    }
    if (PYPI_PACKAGE.equals(packageType)) {
        return PYPI_PATTERNS
    }
    if (NUGET_PACKAGE.equals(packageType)) {
        return NUGET_PATTERNS
    }
    if (NPM_PACKAGE.equals(packageType)) {
        return NPM_PATTERNS
    }
}

private Dependency createDependency(SimpleBdioFactory simpleBdioFactory, RepoPath repoPath, String packageType) {
    try {
        if (NUGET_PACKAGE.equals(packageType)) {
            Properties properties = repositories.getProperties(repoPath)
            String name = properties['nuget.id'][0]
            String version = properties['nuget.version'][0]
            ExternalId externalId = simpleBdioFactory.createNameVersionExternalId(Forge.NUGET, name, version)
            if (name && version && externalId) {
                return new Dependency(name, version, externalId)
            }
        }

        if (NPM_PACKAGE.equals(packageType)) {
            FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath)
            Properties properties = repositories.getProperties(repoPath)
            Forge forge = Forge.NPM
            String name
            String version
            if (properties['npm.name'] && properties['npm.version']) {
                name = properties['npm.name'][0]
                version = properties['npm.version'][0]
            } else {
                name = fileLayoutInfo.module
                version = fileLayoutInfo.baseRevision
            }
            ExternalId externalId = simpleBdioFactory.createNameVersionExternalId(Forge.NPM, name, version)
            if (name && version && externalId) {
                return new Dependency(name, version, externalId)
            }
        }

        if (PYPI_PACKAGE.equals(packageType)) {
            FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath)
            Properties properties = repositories.getProperties(repoPath)
            Forge forge = Forge.PYPI
            String name
            String version
            if (properties['pypi.name'] && properties['pypi.version']) {
                name = properties['pypi.name'][0]
                version = properties['pypi.version'][0]
            } else {
                name = fileLayoutInfo.module
                version = fileLayoutInfo.baseRevision
            }
            if (!name || !version) {
                try {
                    def pypiService = ((ApplicationContext) ctx).getBean('pypiService')
                    def pypiMetadata = pypiService.getPypiMetadata(repoPath)
                    name = pypiMetadata.name
                    version = pypiMetadata.version
                } catch (Exception e) {
                }
            }
            ExternalId externalId = simpleBdioFactory.createNameVersionExternalId(Forge.PYPI, name, version)
            if (name && version && externalId) {
                return new Dependency(name, version, externalId)
            }
        }

        if (MAVEN_PACKAGE.equals(packageType) || GRADLE_PACKAGE.equals(packageType)) {
            FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath)
            String name = fileLayoutInfo.module
            String version = fileLayoutInfo.baseRevision
            String group = fileLayoutInfo.organization
            ExternalId externalId = simpleBdioFactory.createMavenExternalId(group, name, version)
            if (name && version && externalId) {
                return new Dependency(name, version, externalId)
            }
        }

        if (RUBYGEMS_PACKAGE.equals(packageType)) {
            FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath)
            String name = fileLayoutInfo.module
            String version = fileLayoutInfo.baseRevision
            ExternalId externalId = simpleBdioFactory.createNameVersionExternalId(Forge.RUBYGEMS, name, version)
            if (name && version && externalId) {
                return new Dependency(name, version, externalId)
            }
        }
    } catch (Exception e) {
        log.debug("Could not resolve dependency: ${e.getMessage()}")
    }

    return null
}

private void updateFromHubProject(String repoKey, String projectName, String projectVersionName) {
    HubServicesFactory hubServicesFactory = createHubServicesFactory();
    HubService hubService = hubServicesFactory.createHubService();
    ProjectDataService projectDataService = hubServicesFactory.createProjectDataService();

    ProjectVersionWrapper projectVersionWrapper = projectDataService.getProjectVersion(projectName, projectVersionName);
    if (hubService.hasLink(projectVersionWrapper.getProjectVersionView(), MetaHandler.COMPONENTS_LINK)) {
        String componentsLink = hubService.getFirstLink(projectVersionWrapper.getProjectVersionView(), MetaHandler.COMPONENTS_LINK);
        List<VersionBomComponentView> versionBomComponents = hubService.getAllViews(componentsLink, VersionBomComponentView.class);

        Map<String, String> externalIdToComponentVersionLink = transformVersionBomComponentViews(versionBomComponents);
        addOriginIdProperties(repoKey, externalIdToComponentVersionLink);
    }
}

private Map<String, String> transformVersionBomComponentViews(List<VersionBomComponentView> versionBomComponents) {
    Map<String, String> externalIdToComponentVersionLink = new HashMap<>();
    versionBomComponents.each { component ->
        String componentVersionLink = component.componentVersion;
        component.origins.each { origin ->
            String currentValue = externalIdToComponentVersionLink.get(origin.externalId);
            if (currentValue != null && !currentValue.equals(componentVersionLink)) {
                log.warn(String.format("The external id %s is already assigned to the component link %s so we will not assign it to %s", origin.externalId, currentValue, componentVersionLink));
            } else {
                externalIdToComponentVersionLink.put(origin.externalId, componentVersionLink);
            }
        }
    }

    return externalIdToComponentVersionLink
}

private void addOriginIdProperties(String repoKey, Map<String, String> externalIdToComponentVersionLink) {
    externalIdToComponentVersionLink.each { key, value ->
        SetMultimap<String,String> setMultimap = new HashMultimap<>();
        setMultimap.put(HUB_ORIGIN_ID_PROPERTY_NAME, key);
        List<RepoPath> artifactsWithOriginId = searches.itemsByProperties(setMultimap, repoKey)
        artifactsWithOriginId.each { repoPath ->
            repositories.setProperty(repoPath, HUB_COMPONENT_VERSION_LINK_PROPERTY_NAME, value)
        }
    }
}

private HubServerConfig createHubServerConfig() {
    HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder()
    hubServerConfigBuilder.setHubUrl(HUB_URL)
    hubServerConfigBuilder.setUsername(HUB_USERNAME)
    hubServerConfigBuilder.setPassword(HUB_PASSWORD)
    hubServerConfigBuilder.setTimeout(HUB_TIMEOUT)
    hubServerConfigBuilder.setProxyHost(HUB_PROXY_HOST)
    hubServerConfigBuilder.setProxyPort(HUB_PROXY_PORT)
    hubServerConfigBuilder.setProxyUsername(HUB_PROXY_USERNAME)
    hubServerConfigBuilder.setProxyPassword(HUB_PROXY_PASSWORD)
    hubServerConfigBuilder.setAlwaysTrustServerCertificate(HUB_ALWAYS_TRUST_CERTS)

    return hubServerConfigBuilder.build()
}

private HubServicesFactory createHubServicesFactory() {
    HubServerConfig hubServerConfig = createHubServerConfig()
    CredentialsRestConnection credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(log))
    new HubServicesFactory(credentialsRestConnection)
}
