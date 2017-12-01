package com.blackducksoftware.integration.hub.artifactory.inspect

import org.artifactory.fs.FileLayoutInfo
import org.artifactory.md.Properties
import org.artifactory.repo.RepoPath

import com.blackducksoftware.integration.hub.bdio.model.Forge
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalId
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalIdFactory


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
