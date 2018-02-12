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

import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.io.FileHandler
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

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
        return (StringUtils.isBlank(configurationProperties.hubUrl)
                || StringUtils.isBlank(configurationProperties.hubApiKey)
                || StringUtils.isBlank(configurationProperties.hubTimeout)
                || StringUtils.isBlank(configurationProperties.hubAlwaysTrustCerts))
    }

    void updateBaseConfigValues(PropertiesConfiguration config, File outputFile, Console console, PrintStream out) {
        out.println('Updating Config - just hit enter to make no change to a value:')
        configurationProperties.hubUrl = setValueFromInput(console, out, 'Hub Server Url', config, PluginProperty.HUB_URL)
        configurationProperties.hubApiKey = setValueFromInput(console, out, 'Hub Server API Key', config, PluginProperty.HUB_API_KEY)
        configurationProperties.hubTimeout = setValueFromInput(console, out, 'Hub Server Timeout', config, PluginProperty.HUB_TIMEOUT)
        configurationProperties.hubAlwaysTrustCerts = setValueFromInput(console, out, 'Always Trust Server Certificates', config, PluginProperty.HUB_ALWAYS_TRUST_CERT)

        out.println('If you wish to set up proxy details, enter \'y\'. Otherwise, just press <enter> to continue.')
        String userValue = StringUtils.trimToEmpty(console.readLine())
        if ('y' == userValue) {
            configurationProperties.hubProxyHost = setValueFromInput(console, out, 'Proxy Host', config, PluginProperty.HUB_PROXY_HOST)
            configurationProperties.hubProxyPort = setValueFromInput(console, out, 'Proxy Port', config, PluginProperty.HUB_PROXY_PORT)
            configurationProperties.hubProxyUsername = setValueFromInput(console, out, 'Proxy Username', config, PluginProperty.HUB_PROXY_USERNAME)
            configurationProperties.hubProxyPassword = setValueFromInput(console, out, 'Proxy Password', config, PluginProperty.HUB_PROXY_PASSWORD)
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
                updateBaseConfigValues(config, console, out)
            }
        }
    }

    boolean updateArtifactoryConnectionValues(Console console, PrintStream out) {
        configurationProperties.artifactoryUrl = setValueFromInput(console, out, 'Url to Artifactory Instance', configurationProperties.artifactoryUrl)
        configurationProperties.artifactoryUsername = setValueFromInput(console, out, 'Artifactory Username', configurationProperties.artifactoryUsername)
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
        config.setProperty(PluginProperty.HUB_URL.getKey(), configurationProperties.hubUrl)
        config.setProperty(PluginProperty.HUB_TIMEOUT.getKey(), configurationProperties.hubTimeout)
        config.setProperty(PluginProperty.HUB_API_KEY.getKey(), configurationProperties.hubApiKey)
        config.setProperty(PluginProperty.HUB_ALWAYS_TRUST_CERT.getKey(), configurationProperties.hubAlwaysTrustCerts)
        config.setProperty(PluginProperty.HUB_PROXY_HOST.getKey(), configurationProperties.hubProxyHost)
        config.setProperty(PluginProperty.HUB_PROXY_PORT.getKey(), configurationProperties.hubProxyPort)
        config.setProperty(PluginProperty.HUB_PROXY_USERNAME.getKey(), configurationProperties.hubProxyUsername)
        config.setProperty(PluginProperty.HUB_PROXY_PASSWORD.getKey(), configurationProperties.hubProxyPassword)

        persistConfigToFile(config, outputFile)
    }

    String setValueFromInput(Console console, PrintStream out, String propertyDescription, PropertiesConfiguration config, PluginProperty property) {
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
    }

    void persistConfigToFile(PropertiesConfiguration config, File outputFile) {
        FileHandler handler = new FileHandler(config);
        handler.save(outputFile);
    }
}