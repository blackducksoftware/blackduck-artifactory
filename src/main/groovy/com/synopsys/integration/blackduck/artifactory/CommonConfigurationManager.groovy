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

import com.synopsys.integration.blackduck.artifactory.ConfigurationProperty
import embedded.org.apache.commons.lang3.StringUtils
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.io.FileHandler
import org.quartz.CronExpression
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.text.ParseException

@Component
class CommonConfigurationManager {
    @Autowired
    HubClient hubClient

    @Autowired
    RestTemplateContainer restTemplateContainer

    @Autowired
    ArtifactoryRestClient artifactoryRestClient

    @Autowired
    ConfigurationProperties configurationProperties

    boolean needsBaseConfigUpdate() {
        return (StringUtils.isBlank(configurationProperties.blackduckHubUrl)
        || StringUtils.isBlank(configurationProperties.blackduckHubApiToken)
        || StringUtils.isBlank(configurationProperties.blackduckHubTimeout)
        || StringUtils.isBlank(configurationProperties.blackduckHubTrustCert))
    }

    void updateBaseConfigValues(PropertiesConfiguration config, File outputFile, Console console, PrintStream out) {
        configurationProperties.blackduckHubUrl = setValueFromInput(console, out, 'Hub Server Url', config, BlackDuckHubProperty.URL)
        configurationProperties.blackduckHubApiToken = setValueFromInput(console, out, 'Hub Server API Token', config, BlackDuckHubProperty.API_TOKEN)
        if (StringUtils.isBlank(configurationProperties.blackduckHubApiToken)) {
            configurationProperties.blackduckHubUsername = setValueFromInput(console, out, 'Hub Server Username', config, BlackDuckHubProperty.USERNAME)
            configurationProperties.blackduckHubPassword = setValueFromInput(console, out, 'Hub Server Password', config, BlackDuckHubProperty.PASSWORD)
        }
        configurationProperties.blackduckHubTimeout = setValueFromInput(console, out, 'Hub Server Timeout', config, BlackDuckHubProperty.TIMEOUT)
        configurationProperties.blackduckHubTrustCert = setValueFromInput(console, out, 'Always Trust Server Certificates', config, BlackDuckHubProperty.TRUST_CERT)

        out.println('If you wish to set up proxy details, enter \'y\'. Otherwise, just press <enter> to continue.')
        String userValue = StringUtils.trimToEmpty(console.readLine())
        if ('y' == userValue) {
            configurationProperties.blackduckHubProxyHost = setValueFromInput(console, out, 'Proxy Host', config, BlackDuckHubProperty.PROXY_HOST)
            configurationProperties.blackduckHubProxyPort = setValueFromInput(console, out, 'Proxy Port', config, BlackDuckHubProperty.PROXY_PORT)
            configurationProperties.blackduckHubProxyUsername = setValueFromInput(console, out, 'Proxy Username', config, BlackDuckHubProperty.PROXY_USERNAME)
            configurationProperties.blackduckHubProxyPassword = setValueFromInput(console, out, 'Proxy Password', config, BlackDuckHubProperty.PROXY_PASSWORD)
        }
        persistCommonProperties(config, outputFile)

        try {
            hubClient.testHubConnection()
            out.println 'Your Hub configuration is valid and a successful connection to the Hub was established.'
        } catch (Exception e) {
            out.println("Your Hub configuration is not valid: ${e.message}")
            out.println('If you wish to re-enter the base configuration, enter \'y\', otherwise, just press <enter> to continue.')
            userValue = StringUtils.trimToEmpty(console.readLine())
            if ('y' == userValue) {
                updateBaseConfigValues(config, outputFile, console, out)
            }
        }
    }

    boolean updateArtifactoryConnectionValues(Console console, PrintStream out) {
        configurationProperties.artifactoryUrl = setValueFromInput(console, out, 'Url to Artifactory Instance', configurationProperties.artifactoryUrl)
        configurationProperties.artifactoryApiKey = setValueFromInput(console, out, 'Artifactory API Key', configurationProperties.artifactoryApiKey)
        boolean ok = false;
        try {
            restTemplateContainer.init()
            String connectionStatus = artifactoryRestClient.checkSystem();
            out.println("Connection status: ${connectionStatus}")
            ok = 'OK'.equalsIgnoreCase(connectionStatus)
        } catch (Exception e) {
            out.println("An error occurred when establishing your connection to Artifactory: ${e.message}")
        }
        if (!ok) {
            out.println('If you would like to reconfigure your connection, enter \'y\'. Otherwise, just press <enter> to skip volume testing.')
            def userValue = StringUtils.trimToEmpty(console.readLine())
            if ('y' == userValue) {
                updateArtifactoryConnectionValues(console, out)
            }
        }
        return ok
    }

    void persistCommonProperties(PropertiesConfiguration config, File outputFile) {
        config.setProperty(BlackDuckHubProperty.URL.getKey(), configurationProperties.blackduckHubUrl)
        config.setProperty(BlackDuckHubProperty.TIMEOUT.getKey(), configurationProperties.blackduckHubTimeout)
        config.setProperty(BlackDuckHubProperty.API_TOKEN.getKey(), configurationProperties.blackduckHubApiToken)
        config.setProperty(BlackDuckHubProperty.USERNAME.getKey(), configurationProperties.blackduckHubUsername)
        config.setProperty(BlackDuckHubProperty.PASSWORD.getKey(), configurationProperties.blackduckHubPassword)
        config.setProperty(BlackDuckHubProperty.TRUST_CERT.getKey(), configurationProperties.blackduckHubTrustCert)
        config.setProperty(BlackDuckHubProperty.PROXY_HOST.getKey(), configurationProperties.blackduckHubProxyHost)
        config.setProperty(BlackDuckHubProperty.PROXY_PORT.getKey(), configurationProperties.blackduckHubProxyPort)
        config.setProperty(BlackDuckHubProperty.PROXY_USERNAME.getKey(), configurationProperties.blackduckHubProxyUsername)
        config.setProperty(BlackDuckHubProperty.PROXY_PASSWORD.getKey(), configurationProperties.blackduckHubProxyPassword)

        persistConfigToFile(config, outputFile)
    }

    String setValueFromInput(Console console, PrintStream out, String propertyDescription, PropertiesConfiguration config, ConfigurationProperty property) {
        return setValueFromInput(console, out, propertyDescription, config.getString(property.getKey()))
    }

    String setValueFromInput(Console console, PrintStream out, String propertyDescription, String oldValue) {
        out.print("Enter ${propertyDescription} (current value=\'${oldValue}\'): ")
        String userValue = StringUtils.trimToEmpty(console.readLine())
        if (StringUtils.isNotBlank(userValue)) {
            userValue
        } else {
            oldValue
        }
    }

    String setCronFromInput(Console console, PrintStream out, String propertyDescription, PropertiesConfiguration config, ConfigurationProperty property) {
        String cronString = setValueFromInput(console, out, propertyDescription, config, property)
        while (!validateCron(out, cronString)) {
            out.println('Enter \'y\' to continue regardless, otherwise press <enter> to input another CRON expression.')
            String userValue = StringUtils.trimToEmpty(console.readLine())
            if ('y' == userValue) {
                break
            }
            cronString = setValueFromInput(console, out, propertyDescription, config, property)
        }
        return cronString
    }

    boolean validateCron(PrintStream out, String cronString) {
        try {
            CronExpression.validateExpression(cronString)
        } catch (ParseException e) {
            out.println("\'${cronString}\' is not a valid CRON expression: ${e.getMessage()}")
            return false
        }
        return true
    }

    List<String> getRepositoriesToSearch(String csvPath, String repositoryList) {
        if (StringUtils.isNotBlank(csvPath)) {
            def repositoriesToSearch = []
            def repositoryFile = new File(csvPath)
            repositoryFile.splitEachLine(',') { repos ->
                repositoriesToSearch.addAll(repos)
            }
            return repositoriesToSearch
        }
        return StringUtils.isNotBlank(repositoryList) ? repositoryList.tokenize(',') : []
    }

    int getArtifactCount(List<String> repositoriesToSearch, List<String> patternsToSearch) {
        int matches = 0
        patternsToSearch.each { pattern ->
            matches += artifactoryRestClient.searchForArtifactTerm(repositoriesToSearch, pattern).size()
        }

        return matches
    }

    void persistConfigToFile(PropertiesConfiguration config, File outputFile) {
        FileHandler handler = new FileHandler(config);
        handler.save(outputFile);
    }

    void printVisualValidationOfCron(PrintStream out, String cronName, String cronExpressionString) {
        String message
        CronExpression cronExpression;
        message = "The CRON job ${cronName}"
        try {
            cronExpression = new CronExpression(cronExpressionString)
            Date soonestExecutionDate = cronExpression.getNextValidTimeAfter(new Date())
            Date secondSoonestExecutionDate = cronExpression.getNextValidTimeAfter(soonestExecutionDate)
            message += " will next be executed at ${soonestExecutionDate.toString()}, then ${secondSoonestExecutionDate.toString()}"
        } catch (ParseException e) {
            message += (' is configured with an invalid CRON expression')
        }

        out.println(message)
    }
}
