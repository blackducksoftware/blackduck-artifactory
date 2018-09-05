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

import com.synopsys.integration.blackduck.artifactory.inspect.InspectorConfigurationManager
import com.synopsys.integration.blackduck.artifactory.scan.ScannerConfigurationManager
import embedded.org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

import javax.annotation.PostConstruct

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

        configurePlugin()
    }

    void configurePlugin() {
        if ('configure-inspector' == mode && (null != System.console() && null != System.out)) {
            System.out.println('Updating ./plugins/lib/blackDuckCacheInspector.properties - just hit enter to make no change to a value:')
            inspectorConfigurationManager.configure(System.console(), System.out)
            if (inspectorConfigurationManager.needsUpdate()) {
                System.out.println('The inspector was not completely configured. Would you like to restart configuration? Enter \'y\' to re-configure the inspector, or press <enter> to exit configuration.')
                def userValue = StringUtils.trimToEmpty(System.console().readLine())
                if ('y' == userValue) {
                    configurePlugin()
                } else {
                    System.out.println('Exiting configuration. You can finish configuring the inspector manually by editing the properties file located at \'./plugins/lib/blackDuckCacheInspector.properties\'')
                }
            } else {
                System.out.println('The inspector has been configured successfully, the properties file has been generated in \'./plugins/lib/blackDuckCacheInspector.properties\'')
            }
        }

        if ('configure-scanner' == mode && (null != System.console() && null != System.out)) {
            System.out.println('Updating ./plugins/lib/blackDuckScan.properties - just hit enter to make no change to a value:')
            scannerConfigurationManager.configure(System.console(), System.out)
            if (scannerConfigurationManager.needsUpdate()) {
                System.out.println('The scanner was not completely configured. Would you like to restart configuration? Enter \'y\' to re-configure the scanner, or press <enter> to exit configuration.')
                def userValue = StringUtils.trimToEmpty(System.console().readLine())
                if ('y' == userValue) {
                    configurePlugin()
                } else {
                    System.out.println('Exiting configuration. You can finish configuring the scanner manually by editing the properties file located at \'./plugins/lib/blackDuckScan.properties\'')
                }
            } else {
                System.out.println('The scanner has been configured successfully, the properties file has been generated in \'./plugins/lib/blackDuckScan.properties\'')
            }
        }
    }
}
