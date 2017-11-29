package com.blackducksoftware.integration.hub.artifactory.inspect

import org.artifactory.fs.FileLayoutInfo
import org.artifactory.md.Properties
import org.artifactory.repo.RepoPath


executions {
    inspectRepository(httpMethod: 'GET', params:[:]) { params ->
        if(!params['repos'][0] && !params['patterns'][0]) {
            message = 'You must provide a comma separated list of repos and patterns'
        } else {
            Set<RepoPath> repoPaths = searchForRepoPaths(params['repos'][0], params['patterns'][0])
            message = ''
            for(RepoPath repoPath: repoPaths) {
                message += getForgeInformation(repoPath) + '\n'
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

private String getForgeInformation(RepoPath repoPath) {
    FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath)
    Properties properties = repositories.getProperties(repoPath)
    String name;
    String version;
    String forge = repositories.getRepositoryConfiguration(repoPath.getRepoKey()).getPackageType();
    switch (forge) {
        case 'maven':
            name = fileLayoutInfo.module
            version = fileLayoutInfo.baseRevision
            break;
        case 'npm':
            name = properties['npm.name'][0]
            version = properties['npm.version'][0]
            break;
        default:
            return "${forge}:${properties.dump()}"
            break;
    }
    return "${forge}:${name}:${version}"
}
