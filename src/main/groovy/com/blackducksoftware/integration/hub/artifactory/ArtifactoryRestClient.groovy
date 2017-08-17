package com.blackducksoftware.integration.hub.artifactory

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException

import groovy.json.JsonSlurper

@Component
class ArtifactoryRestClient {
    @Autowired
    RestTemplateContainer restTemplate

    @Autowired
    ConfigurationProperties configurationProperties

    Map getVersionInfoForArtifactory() {
        def apiUrl = "${configurationProperties.artifactoryUrl}/api/system/version"
        getJsonResponse(apiUrl)
    }

    String checkSystem() {
        def apiUrl = "${configurationProperties.artifactoryUrl}/api/system/ping"
        restTemplate.getForObject(apiUrl, String.class)
    }

    Map getInfoForInfoUri(String uri) {
        getJsonResponse(uri)
    }

    Map getInfoForPath(String repoKey, String repoPath) {
        def apiUrl = "${configurationProperties.artifactoryUrl}/api/storage/${repoKey}/${repoPath}"
        getJsonResponse(apiUrl)
    }

    List searchForArtifactTerm(List<String> reposToSearch, String artifactTerm) {
        def apiUrl = "${configurationProperties.artifactoryUrl}/api/search/artifact?name=${artifactTerm}&repos=${reposToSearch.join(',')}"
        getJsonResponse(apiUrl).results.collect { it.uri }
    }

    Map getPropertiesForPath(String repoKey, String repoPath, List<String> propertyNames) {
        def apiUrl = "${configurationProperties.artifactoryUrl}/api/storage/${repoKey}/${repoPath}?properties=${propertyNames.join(',')}"
        getJsonResponse(apiUrl)
    }

    void setPropertiesForPath(String repoKey, String repoPath, Map properties, boolean recursive) {
        def propertiesParameter = properties.collect { key, value -> "${key}=${value}" }.join('|')
        def recursiveNum = recursive ? "1" : "0"
        def apiUrl = "${configurationProperties.artifactoryUrl}/api/storage/${repoKey}/${repoPath}?properties=${propertiesParameter}&recursive=${recursiveNum}"
        restTemplate.put(apiUrl, "")
    }

    void deletePropertiesForPath(String repoKey, String repoPath, List<String> propertyNames) {
        def apiUrl = "${configurationProperties.artifactoryUrl}/api/storage/${repoKey}/${repoPath}?properties=${propertyNames.join(',')}"
        restTemplate.delete(apiUrl)
    }

    Map getStatsForPath(String repoKey, String repoPath) {
        def apiUrl = "${configurationProperties.artifactoryUrl}/api/storage/${repoKey}/${repoPath}?stats"
        getJsonResponse(apiUrl)
    }

    private Map getJsonResponse(String apiUrl) {
        try {
            def body = restTemplate.getForObject(apiUrl, String.class)
            return new JsonSlurper().parseText(body)
        } catch (HttpStatusCodeException e) {
            if (HttpStatus.NOT_FOUND == e.statusCode) {
                return [:]
            } else {
                throw e
            }
        }
    }
}
