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

import javax.annotation.PostConstruct

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean

import com.blackducksoftware.integration.hub.bdio.BdioNodeFactory
import com.blackducksoftware.integration.hub.bdio.BdioPropertyHelper
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalIdFactory

@SpringBootApplication
class Application {
    private final Logger logger = LoggerFactory.getLogger(Application.class)

    @Autowired
    BdioPropertyHelper bdioPropertyHelper

    @Autowired
    ConfigurationManager configurationManager

    @Value('${mode}')
    String mode

    static void main(final String[] args) {
        new SpringApplicationBuilder(Application.class).logStartupInfo(false).run(args)
    }

    @PostConstruct
    void init() {
        if (StringUtils.isBlank(mode)) {
            logger.error('You are running without specifying a mode. Please add \'--mode=(configure-inspector|configure-scanner)\' to your command.')
            return
        }

        if (null != System.console() && null != System.out) {
            logger.info('You are running in an interactive mode - if configuration is needed, you should be prompted to provide it.')
            if (mode.contains('configure') || configurationManager.needsBaseConfigUpdate()) {
                configurationManager.updateBaseConfigValues(System.console(), System.out)
            }

            if ('configure-inspector' == mode) {
                configurationManager.updateArtifactoryInspectValues(System.console(), System.out)
            } else if ('configure-scanner' == mode) {
                configurationManager.updateArtifactoryScanValues(System.console(), System.out)
            }
        } else {
            logger.info('You are NOT running in an interactive mode - if configuration is needed, and error will occur.')
        }

        if (configurationManager.needsBaseConfigUpdate()) {
            logger.error('You have not provided enough configuration to run either an inspection or a scan - please edit the \'config/application.properties\' file directly, or run from a command line to configure the properties.')
        } else if ('configure-inspector' == mode && configurationManager.needsArtifactoryInspectUpdate()) {
            logger.error('You have not provided enough configuration to configure the inspector plugin - please edit the \'config/blackDuckCacheInspector.properties\' file directly, or run from a command line to configure the properties.')
        } else if ('configure-scanner' == mode && configurationManager.needsArtifactoryScanUpdate()) {
            logger.error('You have not provided enough configuration to configure the scan plugin - please edit the \'config/blackDuckScanForHub.properties\' file directly, or run from a command line to configure the properties.')
        }
    }

    @Bean
    BdioNodeFactory bdioNodeFactory() {
        new BdioNodeFactory(bdioPropertyHelper)
    }

    @Bean
    BdioPropertyHelper bdioPropertyHelper() {
        new BdioPropertyHelper()
    }

    @Bean
    ExternalIdFactory externalIdFactory() {
        new ExternalIdFactory()
    }
}
