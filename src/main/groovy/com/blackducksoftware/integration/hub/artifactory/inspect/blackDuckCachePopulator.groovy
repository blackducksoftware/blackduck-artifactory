package com.blackducksoftware.integration.hub.artifactory.inspect

import org.artifactory.repo.RepoPath

import com.blackducksoftware.integration.hub.api.item.MetaService
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

executions {
    populateRepository(httpMethod: 'GET') { params ->
        def repoName = params['repoName'][0]
        def projectName = params['projectName'][0]
        def projectVersionName = params['projectVersionName'][0]
        message = "${repoName}--${projectName}--${projectVersionName}"

        Map<String, String> externalIdToComponentVersionLink = new HashMap<>();
        HubServicesFactory hubServicesFactory = createHubServicesFactory();
        MetaService metaService = hubServicesFactory.createMetaService();
        HubResponseService hubResponseService = hubServicesFactory.createHubResponseService();
        ProjectDataService projectDataService = hubServicesFactory.createProjectDataService();

        ProjectVersionWrapper projectVersionWrapper = projectDataService.getProjectVersion(projectName, projectVersionName);
        if (metaService.hasLink(projectVersionWrapper.getProjectVersionView(), MetaService.COMPONENTS_LINK)) {
            String componentsLink = metaService.getFirstLink(projectVersionWrapper.getProjectVersionView(), MetaService.COMPONENTS_LINK);
            List<VersionBomComponentView> versionBomComponents = hubResponseService.getAllItems(componentsLink, VersionBomComponentView.class);
            message += "Found ${versionBomComponents.size()} components in the hub"
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
            externalIdToComponentVersionLink.each { key, value ->
                message += "checking for ${key}"
                SetMultimap<String,String> setMultimap = new HashMultimap<>();
                setMultimap.put('hubOriginId', key);
                List<RepoPath> artifactsWithOriginId = searches.itemsByProperties(setMultimap, repoName)
                artifactsWithOriginId.each { repoPath ->
                    repositories.setProperty(repoPath, 'componentVersionLink', value)
                }
            }
        }
    }
}

private HubServerConfig createHubServerConfig() {
    HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder()
    hubServerConfigBuilder.setHubUrl('https://int-hub04.dc1.lan')
    hubServerConfigBuilder.setUsername('sysadmin')
    hubServerConfigBuilder.setPassword('blackduck')
    hubServerConfigBuilder.setAlwaysTrustServerCertificate(true)

    return hubServerConfigBuilder.build()
}

private HubServicesFactory createHubServicesFactory() {
    HubServerConfig hubServerConfig = createHubServerConfig()
    CredentialsRestConnection credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(log))
    new HubServicesFactory(credentialsRestConnection)
}
