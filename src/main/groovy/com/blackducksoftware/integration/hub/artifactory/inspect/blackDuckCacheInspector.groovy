package com.blackducksoftware.integration.hub.artifactory.inspect

import org.apache.commons.lang3.StringUtils
import org.artifactory.fs.FileLayoutInfo
import org.artifactory.md.Properties
import org.artifactory.repo.RepoPath
import org.artifactory.repo.RepoPathFactory
import org.artifactory.repo.RepositoryConfiguration

import com.blackducksoftware.integration.hub.api.bom.BomImportRequestService
import com.blackducksoftware.integration.hub.api.item.MetaService
import com.blackducksoftware.integration.hub.bdio.SimpleBdioFactory
import com.blackducksoftware.integration.hub.bdio.graph.MutableDependencyGraph
import com.blackducksoftware.integration.hub.bdio.model.Forge
import com.blackducksoftware.integration.hub.bdio.model.SimpleBdioDocument
import com.blackducksoftware.integration.hub.bdio.model.dependency.Dependency
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalId
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder
import com.blackducksoftware.integration.hub.dataservice.project.ProjectDataService
import com.blackducksoftware.integration.hub.dataservice.project.ProjectVersionWrapper
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.model.view.VersionBomComponentView
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection
import com.blackducksoftware.integration.hub.service.HubResponseService
import com.blackducksoftware.integration.hub.service.HubServicesFactory
import com.blackducksoftware.integration.log.Slf4jIntLogger
import com.blackducksoftware.integration.util.IntegrationEscapeUtil
import com.google.common.collect.HashMultimap
import com.google.common.collect.SetMultimap

import groovy.transform.Field

@Field final String HUB_URL=""
@Field final int HUB_TIMEOUT=120
@Field final String HUB_USERNAME=""
@Field final String HUB_PASSWORD=""

@Field final String HUB_PROXY_HOST=""
@Field final int HUB_PROXY_PORT=0
@Field final String HUB_PROXY_USERNAME=""
@Field final String HUB_PROXY_PASSWORD=""

@Field final boolean HUB_ALWAYS_TRUST_CERTS=false

executions {
    inspectRepository(httpMethod: 'POST') { params ->
        if (!params['repoKey'][0] && !params['patterns'][0]) {
            message = 'You must provide a repoKey and comma-separated list of patterns'
            return
        }

        createHubProject(params['repoKey'][0], params['patterns'][0])
    }

    updateInspectedRepository(httpMethod: 'POST') { params ->
        def repoKey = params['repoKey'][0]
        def projectName = getDefaultProjectName(repoKey)
        def projectVersionName = getDefaultProjectVersionName(repoKey)

        if (params.containsKey('projectName')) {
            projectName = params['projectName'][0]
        }
        if (params.containsKey('projectVersionName')) {
            projectVersionName = params['projectVersionName'][0]
        }

        updateFromHubProject(repoKey, projectName, projectVersionName);
    }
}

private String getDefaultProjectName(String repoKey) {
    RepoPath repoPath = RepoPathFactory.create(repoKey)
    String projectNameProperty = repositories.getProperty(repoPath, 'blackduck.hubProjectName')
    if (StringUtils.isNotBlank(projectNameProperty)) {
        return projectNameProperty
    }
    return repoKey
}

private String getDefaultProjectVersionName(String repoKey) {
    RepoPath repoPath = RepoPathFactory.create(repoKey)
    String projectVersionNameProperty = repositories.getProperty(repoPath, 'blackduck.hubProjectVersionName')
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
            repositories.setProperty(repoPath, 'blackduck.hubOriginId', hubOriginId)
            mutableDependencyGraph.addChildToRoot(repoPathDependency);
        }
    }

    Forge artifactoryForge = new Forge('/', '/', 'artifactory');
    String projectName = getDefaultProjectName(repoKey);
    String projectVersionName = getDefaultProjectVersionName(repoKey);
    String codeLocationName = "${projectName}/${projectVersionName}/${packageType}"
    ExternalId projectExternalId = simpleBdioFactory.createNameVersionExternalId(artifactoryForge, projectName, projectVersionName)
    SimpleBdioDocument simpleBdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, projectName, projectVersionName, projectExternalId, mutableDependencyGraph)

    IntegrationEscapeUtil integrationEscapeUtil = new IntegrationEscapeUtil()
    File bdioFile = new File("/tmp/${integrationEscapeUtil.escapeForUri(codeLocationName)}");
    bdioFile.delete()
    simpleBdioFactory.writeSimpleBdioDocumentToFile(bdioFile, simpleBdioDocument);

    BomImportRequestService bomImportRequestService = hubServicesFactory.createBomImportRequestService();
    bomImportRequestService.importBomFile(bdioFile);
}

private Dependency createDependency(SimpleBdioFactory simpleBdioFactory, RepoPath repoPath, String packageType) {
    if ('nuget'.equals(packageType)) {
        Properties properties = repositories.getProperties(repoPath)
        String name = properties['nuget.id'][0]
        String version = properties['nuget.version'][0]
        ExternalId externalId = simpleBdioFactory.createNameVersionExternalId(Forge.NUGET, name, version)
        return new Dependency(name, version, externalId)
    }

    if ('npm'.equals(packageType)) {
        Properties properties = repositories.getProperties(repoPath)
        String name = properties['npm.name'][0]
        String version = properties['npm.version'][0]
        ExternalId externalId = simpleBdioFactory.createNameVersionExternalId(Forge.NPM, name, version)
        return new Dependency(name, version, externalId)
    }

    if ('maven'.equals(packageType) || 'gradle'.equals(packageType)) {
        FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath)
        String name = fileLayoutInfo.module
        String version = fileLayoutInfo.baseRevision
        String group = fileLayoutInfo.organization
        ExternalId externalId = simpleBdioFactory.createMavenExternalId(group, name, version)
        return new Dependency(name, version, externalId)
    }

    if ('gems'.equals(packageType)) {
        FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath)
        String name = fileLayoutInfo.module
        String version = fileLayoutInfo.baseRevision
        ExternalId externalId = simpleBdioFactory.createNameVersionExternalId(Forge.RUBYGEMS, name, version)
        return new Dependency(name, version, externalId)
    }

    return null
}

private void updateFromHubProject(String repoKey, String projectName, String projectVersionName) {
    HubServicesFactory hubServicesFactory = createHubServicesFactory();
    HubResponseService hubResponseService = hubServicesFactory.createHubResponseService();
    ProjectDataService projectDataService = hubServicesFactory.createProjectDataService();

    ProjectVersionWrapper projectVersionWrapper = projectDataService.getProjectVersion(projectName, projectVersionName);
    if (hubResponseService.hasLink(projectVersionWrapper.getProjectVersionView(), MetaService.COMPONENTS_LINK)) {
        String componentsLink = hubResponseService.getFirstLink(projectVersionWrapper.getProjectVersionView(), MetaService.COMPONENTS_LINK);
        List<VersionBomComponentView> versionBomComponents = hubResponseService.getAllItems(componentsLink, VersionBomComponentView.class);

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
        setMultimap.put('blackduck.hubOriginId', key);
        List<RepoPath> artifactsWithOriginId = searches.itemsByProperties(setMultimap, repoKey)
        artifactsWithOriginId.each { repoPath ->
            repositories.setProperty(repoPath, 'componentVersionLink', value)
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
