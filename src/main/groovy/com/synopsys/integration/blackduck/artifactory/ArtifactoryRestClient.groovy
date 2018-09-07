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
package com.synopsys.integration.blackduck.artifactory

import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException

@Component
class ArtifactoryRestClient {
    private final Logger logger = LoggerFactory.getLogger(ArtifactoryRestClient.class);

    @Autowired
    RestTemplateContainer restTemplate

    @Autowired
    ConfigurationProperties configurationProperties

    String checkSystem() {
        def apiUrl = "${configurationProperties.artifactoryUrl}/api/system/ping"
        restTemplate.getForObject(apiUrl, String.class)
    }

    List searchForArtifactTerm(List<String> reposToSearch, String artifactTerm) {
        def apiUrl = "${configurationProperties.artifactoryUrl}/api/search/artifact?name=${artifactTerm}&repos=${reposToSearch.join(',')}"
        getJsonResponse(apiUrl).results.collect { it.uri }
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
