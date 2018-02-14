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

import com.blackducksoftware.integration.hub.artifactory.inspect.InspectorConfigurationManager
import com.blackducksoftware.integration.hub.artifactory.scan.ScannerConfigurationManager

@SpringBootApplication
class Application {
    private final Logger logger = LoggerFactory.getLogger(Application.class)

    @Autowired
    InspectorConfigurationManager inspectorConfigurationManager

    @Autowired
    ScannerConfigurationManager scannerConfigurationManager

    @Value('${mode}')
    String mode

    static void main(final String[] args) {
        new SpringApplicationBuilder(Application.class).logStartupInfo(false).run(args)
    }

    @PostConstruct
    void init() {
        if (StringUtils.isBlank(mode) || ('configure-inspector' != mode && 'configure-scanner' != mode)) {
            logger.error('You are running without specifying a valid mode. Please add \'--mode=(configure-inspector|configure-scanner)\' to your command.')
            return
        }

        if (null != System.console() && null != System.out) {
            if ('configure-inspector' == mode) {
                System.out.println('Updating ./lib/blackDuckCacheInspector.properties - just hit enter to make no change to a value:')
                inspectorConfigurationManager.updateValues(System.console(), System.out)
            } else if ('configure-scanner' == mode) {
                System.out.println('Updating ./lib/blackDuckScanForHub.properties - just hit enter to make no change to a value:')
                scannerConfigurationManager.updateValues(System.console(), System.out)
            }
        }

        if ('configure-inspector' == mode && inspectorConfigurationManager.needsUpdate()) {
            logger.error('The inspector was not completely configured - please edit the \'./lib/blackDuckCacheInspector.properties\' file directly, or run from a command line to configure the properties.')
        } else if ('configure-scanner' == mode && scannerConfigurationManager.needsUpdate()) {
            logger.error('The scanner was not completely configured - please edit the \'./lib/blackDuckScanForHub.properties\' file directly, or run from a command line to configure the properties.')
        }
    }
}
