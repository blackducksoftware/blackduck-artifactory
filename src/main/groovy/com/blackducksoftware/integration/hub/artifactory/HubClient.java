/**
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
package com.blackducksoftware.integration.hub.artifactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.global.HubServerConfig;
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection;
import com.blackducksoftware.integration.log.Slf4jIntLogger;

@Component
public class HubClient {
    private final Logger logger = LoggerFactory.getLogger(HubClient.class);

    @Autowired
    private ConfigurationProperties configurationProperties;

    public boolean isValid() {
        return createBuilder().isValid();
    }

    public void assertValid() throws IllegalStateException {
        createBuilder().build();
    }

    public void testHubConnection() throws IntegrationException {
        final HubServerConfig hubServerConfig = createBuilder().build();
        final CredentialsRestConnection credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(logger));
        credentialsRestConnection.connect();
        logger.info("Successful connection to the Hub!");
    }

    private HubServerConfigBuilder createBuilder() {
        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        hubServerConfigBuilder.setHubUrl(configurationProperties.getHubUrl());
        hubServerConfigBuilder.setApiKey(configurationProperties.getHubApiKey());

        hubServerConfigBuilder.setTimeout(configurationProperties.getHubTimeout());
        hubServerConfigBuilder.setProxyHost(configurationProperties.getHubProxyHost());
        hubServerConfigBuilder.setProxyPort(configurationProperties.getHubProxyPort());
        hubServerConfigBuilder.setProxyUsername(configurationProperties.getHubProxyUsername());
        hubServerConfigBuilder.setProxyPassword(configurationProperties.getHubProxyPassword());

        hubServerConfigBuilder.setAlwaysTrustServerCertificate(Boolean.parseBoolean(configurationProperties.getHubAlwaysTrustCerts()));

        return hubServerConfigBuilder;
    }

}
