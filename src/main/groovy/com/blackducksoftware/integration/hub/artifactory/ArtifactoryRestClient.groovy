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
package com.blackducksoftware.integration.hub.artifactory

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException

import groovy.json.JsonSlurper

@Component
class ArtifactoryRestClient {
    private final Logger logger = LoggerFactory.getLogger(ArtifactoryRestClient.class);

    public static final String ARTIFACTORY_VERSION_KEY = "version";
    public static final String VERSION_UNKNOWN = "???";

    @Autowired
    RestTemplateContainer restTemplate

    @Autowired
    ConfigurationProperties configurationProperties

    String getVersionInfoForArtifactory() {
        def apiUrl = "${configurationProperties.artifactoryUrl}/api/system/version"
        try {
            Map response = getJsonResponse(apiUrl)
            if (response.containsKey(ARTIFACTORY_VERSION_KEY)) {
                return response.get(ARTIFACTORY_VERSION_KEY)
            }
        } catch (Exception e) {
            logger.debug("Error getting artifactory version: ${e.message}")
        }
        return VERSION_UNKNOWN
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
