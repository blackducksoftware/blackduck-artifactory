package com.blackducksoftware.integration.hub.artifactory.inspect

import org.artifactory.fs.FileLayoutInfo
import org.artifactory.md.Properties
import org.artifactory.repo.RepoPath

import com.blackducksoftware.integration.hub.api.item.MetaService
import com.blackducksoftware.integration.hub.bdio.model.Forge
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalId
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalIdFactory
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder
import com.blackducksoftware.integration.hub.dataservice.project.ProjectDataService
import com.blackducksoftware.integration.hub.dataservice.project.ProjectVersionWrapper
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.model.view.VersionBomComponentView
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection
import com.blackducksoftware.integration.hub.service.HubResponseService
import com.blackducksoftware.integration.hub.service.HubServicesFactory
import com.blackducksoftware.integration.log.Slf4jIntLogger
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
    inspectRepository(httpMethod: 'GET', params:[:]) { params ->
        if(!params['repos'][0] && !params['patterns'][0]) {
            message = 'You must provide a comma separated list of repos and patterns'
        } else {
            Set<RepoPath> repoPaths = searchForRepoPaths(params['repos'][0], params['patterns'][0])
            message = ''
            for(RepoPath repoPath: repoPaths) {
                populateHubOriginId(repoPath)
            }
        }
    }

    populateRepository(httpMethod: 'GET') { params ->
        def repoName = params['repoName'][0]
        def projectName = params['projectName'][0]
        def projectVersionName = params['projectVersionName'][0]

        HubServicesFactory hubServicesFactory = createHubServicesFactory();
        MetaService metaService = hubServicesFactory.createMetaService();
        HubResponseService hubResponseService = hubServicesFactory.createHubResponseService();
        ProjectDataService projectDataService = hubServicesFactory.createProjectDataService();

        ProjectVersionWrapper projectVersionWrapper = projectDataService.getProjectVersion(projectName, projectVersionName);
        if (metaService.hasLink(projectVersionWrapper.getProjectVersionView(), MetaService.COMPONENTS_LINK)) {
            String componentsLink = metaService.getFirstLink(projectVersionWrapper.getProjectVersionView(), MetaService.COMPONENTS_LINK);
            List<VersionBomComponentView> versionBomComponents = hubResponseService.getAllItems(componentsLink, VersionBomComponentView.class);
            populateArtifactoryDetails(repoName, versionBomComponents);
        }
    }
}

private Set<RepoPath> searchForRepoPaths(String repos, String patterns) {
    def reposToSearch = repos.tokenize(',')
    def patternsToScan = patterns.tokenize(',')

    def repoPaths = []
    patternsToScan.each {
        repoPaths.addAll(searches.artifactsByName(it, reposToSearch.toArray(new String[reposToSearch.size])))
    }

    repoPaths.toSet()
}

private void populateHubOriginId(RepoPath repoPath) {
    ExternalId externalId = constructExternalIdFromRepoPath(repoPath)
    if (externalId) {
        String hubOriginId = externalId.createHubOriginId()
        repositories.setProperty(repoPath, 'blackduck.hubOriginId', externalId.createHubOriginId())
    }
}

private ExternalId constructExternalIdFromRepoPath(RepoPath repoPath) {
    ExternalIdFactory externalIdFactory = new ExternalIdFactory()
    String packageType = repositories.getRepositoryConfiguration(repoPath.getRepoKey()).getPackageType();
    FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath)
    Properties properties = repositories.getProperties(repoPath)

    if ('nuget'.equals(packageType)) {
        String name = properties['nuget.id'][0]
        String version = properties['nuget.version'][0]
        return externalIdFactory.createNameVersionExternalId(Forge.NUGET, name, version)
    }

    if ('npm'.equals(packageType)) {
        String name = properties['npm.name'][0]
        String version = properties['npm.version'][0]
        return externalIdFactory.createNameVersionExternalId(Forge.NPM, name, version)
    }

    if ('maven'.equals(packageType) || 'gradle'.equals(packageType)) {
        String name = fileLayoutInfo.module
        String version = fileLayoutInfo.baseRevision
        String group = fileLayoutInfo.organization
        return externalIdFactory.createMavenExternalId(group, name, version)
    }


    if ('gems'.equals(packageType)) {
        String name = fileLayoutInfo.module
        String version = fileLayoutInfo.baseRevision
        return externalIdFactory.createNameVersionExternalId(Forge.RUBYGEMS, name, version)
    }

    return null
}

private void populateArtifactoryDetails(String repoName, List<VersionBomComponentView> versionBomComponents) {
    Map<String, String> externalIdToComponentVersionLink = transformVersionBomComponentViews(versionBomComponents);

    addOriginIdProperties(repoName, externalIdToComponentVersionLink);
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

private void addOriginIdProperties(String repoName, Map<String, String> externalIdToComponentVersionLink) {
    externalIdToComponentVersionLink.each { key, value ->
        SetMultimap<String,String> setMultimap = new HashMultimap<>();
        setMultimap.put('hubOriginId', key);
        List<RepoPath> artifactsWithOriginId = searches.itemsByProperties(setMultimap, repoName)
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
