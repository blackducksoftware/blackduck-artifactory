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
package com.synopsys.integration.blackduck.artifactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.configuration.HubServerConfig;
import com.blackducksoftware.integration.hub.configuration.HubServerConfigBuilder;
import com.blackducksoftware.integration.log.Slf4jIntLogger;
import com.blackducksoftware.integration.rest.connection.RestConnection;
import com.blackducksoftware.integration.util.ResourceUtil;

import embedded.org.apache.commons.lang3.StringUtils;

@Component
public class HubClient {
    private final Logger logger = LoggerFactory.getLogger(HubClient.class);

    @Autowired
    private ConfigurationProperties configurationProperties;

    public boolean isValid() {
        return createBuilder().isValid();
    }

    public void assertValid() {
        createBuilder().build();
    }

    public void testHubConnection() throws IntegrationException {
        final HubServerConfig hubServerConfig = createBuilder().build();
        RestConnection restConnection = null;

        try {
            if (StringUtils.isNotBlank(configurationProperties.getBlackduckHubApiToken())) {
                restConnection = hubServerConfig.createApiTokenRestConnection(new Slf4jIntLogger(logger));
            } else {
                restConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(logger));
            }

            restConnection.connect();
        } finally {
            ResourceUtil.closeQuietly(restConnection);
        }

        logger.info("Successful connection to the Hub!");
    }

    private HubServerConfigBuilder createBuilder() {
        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        hubServerConfigBuilder.setUrl(configurationProperties.getBlackduckHubUrl());
        hubServerConfigBuilder.setApiToken(configurationProperties.getBlackduckHubApiToken());
        hubServerConfigBuilder.setUsername(configurationProperties.getBlackduckHubUsername());
        hubServerConfigBuilder.setPassword(configurationProperties.getBlackduckHubPassword());
        hubServerConfigBuilder.setTimeout(configurationProperties.getBlackduckHubTimeout());
        hubServerConfigBuilder.setProxyHost(configurationProperties.getBlackduckHubProxyHost());
        hubServerConfigBuilder.setProxyPort(configurationProperties.getBlackduckHubProxyPort());
        hubServerConfigBuilder.setProxyUsername(configurationProperties.getBlackduckHubProxyUsername());
        hubServerConfigBuilder.setProxyPassword(configurationProperties.getBlackduckHubProxyPassword());
        hubServerConfigBuilder.setTrustCert(configurationProperties.getBlackduckHubTrustCert());

        return hubServerConfigBuilder;
    }

}
