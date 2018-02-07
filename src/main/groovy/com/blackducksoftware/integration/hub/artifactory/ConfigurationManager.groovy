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

import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.fluent.Configurations
import org.apache.commons.configuration2.io.FileHandler
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils

@Component
class ConfigurationManager {
    @Autowired
    HubClient hubClient

    @Autowired
    RestTemplateContainer restTemplateContainer

    @Autowired
    ArtifactoryRestClient artifactoryRestClient

    @Autowired
    ConfigurationProperties configurationProperties

    File userSpecifiedProperties
    File inspectorPropertiesFile
    File scannerPropertiesFile
    PropertiesConfiguration inspectorConfig
    PropertiesConfiguration scannerConfig

    @PostConstruct
    void init() {
        def libDirectory = new File (configurationProperties.currentUserDirectory, 'lib')
        def configs = new Configurations()

        inspectorPropertiesFile = new File(libDirectory, 'blackDuckCacheInspector.properties')
        if (!inspectorPropertiesFile.exists()) {
            inspectorConfig = configs.properties(ResourceUtils.getFile('blackDuckCacheInspector.properties'))
            libDirectory.mkdirs()
            persistInspectorProperties()
        }
        inspectorConfig = configs.properties(inspectorPropertiesFile)

        scannerPropertiesFile = new File(libDirectory, 'blackDuckScanForHub.properties')
        if (!scannerPropertiesFile.exists()) {
            scannerConfig = configs.properties(ResourceUtils.getFile('blackDuckScanForHub.properties'))
            libDirectory.mkdirs()
            persistScannerProperties()
        }
        scannerConfig = configs.properties(scannerPropertiesFile)
    }

    boolean needsBaseConfigUpdate() {
        return (StringUtils.isBlank(configurationProperties.hubUrl)
                || StringUtils.isBlank(configurationProperties.hubApiKey)
                || StringUtils.isBlank(configurationProperties.hubTimeout)
                || StringUtils.isBlank(configurationProperties.hubAlwaysTrustCerts))
    }

    boolean needsArtifactoryInspectUpdate() {
        return ((StringUtils.isBlank(configurationProperties.hubArtifactoryInspectRepositoriesList) && StringUtils.isBlank(configurationProperties.hubArtifactoryInspectRepositoriesCsvPath))
                || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectPatternsRubygems)
                || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectPatternsMaven)
                || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectPatternsGradle)
                || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectPatternsPypi)
                || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectPatternsNuget)
                || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectPatternsNpm))
    }

    boolean needsArtifactoryScanUpdate() {
        return ((StringUtils.isBlank(configurationProperties.hubArtifactoryScanRepositoriesList) && StringUtils.isBlank(configurationProperties.hubArtifactoryScanRepositoriesCsvPath))
                || StringUtils.isBlank(configurationProperties.hubArtifactoryScanNamePatterns)
                || StringUtils.isBlank(configurationProperties.hubArtifactoryScanBinariesDirectoryPath)
                || StringUtils.isBlank(configurationProperties.hubArtifactoryScanMemory)
                || StringUtils.isBlank(configurationProperties.hubArtifactoryScanDryRun))
    }

    void updateBaseConfigValues(Console console, PrintStream out) {
        out.println('Updating Config - just hit enter to make no change to a value:')
        configurationProperties.hubUrl = setValueFromInput(console, out, 'Hub Server Url', configurationProperties.hubUrl)
        configurationProperties.hubApiKey = setValueFromInput(console, out, 'Hub Server API Key', configurationProperties.hubApiKey)
        configurationProperties.hubTimeout = setValueFromInput(console, out, 'Hub Server Timeout', configurationProperties.hubTimeout)
        configurationProperties.hubAlwaysTrustCerts = setValueFromInput(console, out, 'Always Trust Server Certificates', configurationProperties.hubAlwaysTrustCerts)

        boolean ok = false
        try {
            hubClient.testHubConnection()
            out.println 'Your Hub configuration is valid and a successful connection to the Hub was established.'
            ok = true
        } catch (Exception e) {
            out.println("Your Hub configuration is not valid: ${e.message}")
        }

        if (!ok) {
            out.println('You may need to manually edit the \'config/application.properties\' file to provide proxy details. If you wish to re-enter the base configuration, enter \'y\', otherwise, just press <enter> to continue.')
            String userValue = StringUtils.trimToEmpty(console.readLine())
            if ('y' == userValue) {
                updateBaseConfigValues(console, out)
            }
        }
    }

    void updateArtifactoryInspectValues(Console console, PrintStream out) {
        configurationProperties.hubArtifactoryInspectPatternsRubygems = setValueFromInput(console, out, 'Rubygems Artifact Pattern', configurationProperties.hubArtifactoryInspectPatternsRubygems)
        configurationProperties.hubArtifactoryInspectPatternsMaven = setValueFromInput(console, out, 'Maven Artifact Pattern', configurationProperties.hubArtifactoryInspectPatternsMaven)
        configurationProperties.hubArtifactoryInspectPatternsGradle = setValueFromInput(console, out, 'Gradle Artifact Pattern', configurationProperties.hubArtifactoryInspectPatternsGradle)
        configurationProperties.hubArtifactoryInspectPatternsPypi = setValueFromInput(console, out, 'Pypi Artifact Pattern', configurationProperties.hubArtifactoryInspectPatternsPypi)
        configurationProperties.hubArtifactoryInspectPatternsNuget = setValueFromInput(console, out, 'Nuget Artifact Pattern', configurationProperties.hubArtifactoryInspectPatternsNuget)
        configurationProperties.hubArtifactoryInspectPatternsNpm = setValueFromInput(console, out, 'NPM Artifact Pattern', configurationProperties.hubArtifactoryInspectPatternsNpm)

        String reposToSearch = configurationProperties.hubArtifactoryInspectRepositoriesList
        out.println('The artifactory inspector can be configured to either read a list of repositories to inspct, or a file containing a comma separated list of repositories.')
        out.println('If you would like to provide a path to a file, enter \'y\' now. Otherwise, just press <enter> to manually add a list of repositories.')
        String userValue = StringUtils.trimToEmpty(console.readLine())
        if ('y' == userValue) {
            configurationProperties.hubArtifactoryInspectRepositoriesCsvPath = setValueFromInput(console, out, 'Path to File of Artifactory Repositories to Inspect', configurationProperties.hubArtifactoryInspectRepositoriesCsvPath)
        } else {
            def repositoryNames = reposToSearch ? reposToSearch.tokenize(',') : []
            out.println("The current set of repositories to search is ${reposToSearch}. You will be prompted to add new repositories. If you would first like to clear the currently configured repositories, type 'clear'.")
            userValue = StringUtils.trimToEmpty(console.readLine())
            if ('clear' == userValue) {
                repositoryNames = []
            }

            out.println('Enter Artifactory repositories to search for artifacts to inspect, one at a time. If you are finished, just press <enter>.')
            out.print "Enter repository name (current repositories=${repositoryNames.join(',')}): "
            userValue = StringUtils.trimToEmpty(console.readLine())
            while (StringUtils.isNotBlank(userValue)) {
                repositoryNames.add(userValue)
                out.print 'Enter repository name: '
                userValue = StringUtils.trimToEmpty(console.readLine())
            }
            configurationProperties.hubArtifactoryInspectRepositoriesCsvPath = ''
            configurationProperties.hubArtifactoryInspectRepositoriesList = repositoryNames.join(',')
        }

        persistInspectorProperties()

        String repositoriesString
        if (StringUtils.isNotBlank(configurationProperties.hubArtifactoryInspectRepositoriesCsvPath)) {
            repositoriesString = "The repositories listed in \"${configurationProperties.hubArtifactoryInspectRepositoriesCsvPath}\""
            repositoriesString = "These repositories (${configurationProperties.hubArtifactoryInspectRepositoriesList})"
        }
        out.println("${repositoriesString} will be searched for artifacts.")
        out.println("Artifacts in Rubygems repositories will be identified for inspection by the following pattern: '${configurationProperties.hubArtifactoryInspectPatternsRubygems}'")
        out.println("Artifacts in Maven repositories will be identified for inspection by the following pattern: '${configurationProperties.hubArtifactoryInspectPatternsMaven}'")
        out.println("Artifacts in Gradle repositories will be identified for inspection by the following pattern: '${configurationProperties.hubArtifactoryInspectPatternsGradle}'")
        out.println("Artifacts in Pypi repositories will be identified for inspection by the following pattern: '${configurationProperties.hubArtifactoryInspectPatternsPypi}'")
        out.println("Artifacts in Nuget repositories will be identified for inspection by the following pattern: '${configurationProperties.hubArtifactoryInspectPatternsNuget}'")
        out.println("Artifacts in NPM repositories will be identified for inspection by the following pattern: '${configurationProperties.hubArtifactoryInspectPatternsNpm}'")
        out.print('If this is incorrect, enter \'n\' to enter new values, if this is correct, just press <enter>.')
        userValue = StringUtils.trimToEmpty(console.readLine())
        if ('n' == userValue) {
            updateArtifactoryInspectValues(console, out)
        }
    }

    void updateArtifactoryScanValues(Console console, PrintStream out) {
        configurationProperties.hubArtifactoryScanBinariesDirectoryPath = setValueFromInput(console, out, 'Plugin Scan Binaries Directory', configurationProperties.hubArtifactoryScanBinariesDirectoryPath)
        configurationProperties.hubArtifactoryScanMemory = setValueFromInput(console, out, 'Plugin Scan Binaries Directory', configurationProperties.hubArtifactoryScanBinariesDirectoryPath)
        configurationProperties.hubArtifactoryScanDryRun = setValueFromInput(console, out, 'Plugin Scan Binaries Directory', configurationProperties.hubArtifactoryScanBinariesDirectoryPath)
        configurationProperties.hubArtifactoryScanBinariesDirectoryPath = setValueFromInput(console, out, 'Plugin Scan Binaries Directory', configurationProperties.hubArtifactoryScanBinariesDirectoryPath)

        String reposToSearch = configurationProperties.hubArtifactoryScanRepositoriesList
        out.println('The artifactory scanner can be configured to either read a list of repositories to scan, or a file containing a comma separated list of repositories.')
        out.println('If you would like to provide a path to a file, enter \'y\' now. Otherwise, just press <enter> to manually add a list of repositories.')
        String userValue = StringUtils.trimToEmpty(console.readLine())
        if ('y' == userValue) {
            configurationProperties.hubArtifactoryScanRepositoriesCsvPath = setValueFromInput(console, out, 'Path to File of Artifactory Repositories to Scan', configurationProperties.hubArtifactoryScanRepositoriesCsvPath)
        } else {
            def repositoryNames = reposToSearch ? reposToSearch.tokenize(',') : []
            out.println("The current set of repositories to search is ${reposToSearch}. You will be prompted to add new repositories. If you would first like to clear the currently configured repositories, type 'clear'.")
            userValue = StringUtils.trimToEmpty(console.readLine())
            if ('clear' == userValue) {
                repositoryNames = []
            }

            out.println('Enter Artifactory repositories to search for artifacts to scan, one at a time. If you are finished, just press <enter>.')
            out.print "Enter repository name (current repositories=${repositoryNames.join(',')}): "
            userValue = StringUtils.trimToEmpty(console.readLine())
            while (StringUtils.isNotBlank(userValue)) {
                repositoryNames.add(userValue)
                out.print 'Enter repository name: '
                userValue = StringUtils.trimToEmpty(console.readLine())
            }
            configurationProperties.hubArtifactoryScanRepositoriesCsvPath = ''
            configurationProperties.hubArtifactoryScanRepositoriesList = repositoryNames.join(',')
        }

        String namePatternsToScan = configurationProperties.hubArtifactoryScanNamePatterns
        def namePatterns = namePatternsToScan ? namePatternsToScan.tokenize(',') : []
        out.println("The current set of patterns to scan is ${namePatternsToScan}. You will be prompted to add new patterns. If you would first like to clear the currently configured patterns, type 'clear'.")
        userValue = StringUtils.trimToEmpty(console.readLine())
        if ('clear' == userValue) {
            namePatterns = []
        }

        out.println('Enter Artifactory artifact filename patterns to scan, one at a time. If you are finished, just press <enter>.')
        out.print "Enter pattern (current patterns=${namePatterns.join(',')}): "
        userValue = StringUtils.trimToEmpty(console.readLine())
        while (StringUtils.isNotBlank(userValue)) {
            namePatterns.add(userValue)
            out.print 'Enter pattern: '
            userValue = StringUtils.trimToEmpty(console.readLine())
        }
        configurationProperties.hubArtifactoryScanNamePatterns = namePatterns.join(',')

        persistScannerProperties()

        String repositoriesString
        if (StringUtils.isNotBlank(configurationProperties.hubArtifactoryScanRepositoriesCsvPath)) {
            repositoriesString = "The repositories listed in \"${configurationProperties.hubArtifactoryScanRepositoriesCsvPath}\""
            repositoriesString = "These repositories (${configurationProperties.hubArtifactoryScanRepositoriesList})"
        }
        out.println("${repositoriesString} will be searched for these artifact name patterns (${configurationProperties.hubArtifactoryScanNamePatterns}) which will then be scanned.")
        out.print('If this is incorrect, enter \'n\' to enter new values, if this is correct, just press <enter>.')
        userValue = StringUtils.trimToEmpty(console.readLine())
        if ('n' == userValue) {
            updateArtifactoryScanValues(console, out)
        }
    }

    private setCommonProperties(PropertiesConfiguration config) {
        config.setProperty(PluginProperty.HUB_URL.getKey(), configurationProperties.hubUrl)
        config.setProperty(PluginProperty.HUB_TIMEOUT.getKey(), configurationProperties.hubTimeout)
        config.setProperty(PluginProperty.HUB_API_KEY.getKey(), configurationProperties.hubApiKey)
        config.setProperty(PluginProperty.HUB_ALWAYS_TRUST_CERT.getKey(), configurationProperties.hubAlwaysTrustCerts)
        config.setProperty(PluginProperty.HUB_PROXY_HOST.getKey(), configurationProperties.hubProxyHost)
        config.setProperty(PluginProperty.HUB_PROXY_PORT.getKey(), configurationProperties.hubProxyPort)
        config.setProperty(PluginProperty.HUB_PROXY_USERNAME.getKey(), configurationProperties.hubProxyUsername)
        config.setProperty(PluginProperty.HUB_PROXY_PASSWORD.getKey(), configurationProperties.hubProxyPassword)
    }

    private persistScannerProperties() {
        setCommonProperties(scannerConfig)
        scannerConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_NAME_PATTERNS.getKey(), configurationProperties.hubArtifactoryScanNamePatterns)
        scannerConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_MEMORY.getKey(), configurationProperties.hubArtifactoryScanMemory)
        scannerConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_DRY_RUN.getKey(), configurationProperties.hubArtifactoryScanDryRun)
        scannerConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_REPOS.getKey(), configurationProperties.hubArtifactoryScanRepositoriesList)
        scannerConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_REPOS_CSV_PATH.getKey(), configurationProperties.hubArtifactoryScanRepositoriesCsvPath)
        scannerConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_BINARIES_DIRECTORY_PATH.getKey(), configurationProperties.hubArtifactoryScanBinariesDirectoryPath)

        persistConfigToFile(scannerConfig, scannerPropertiesFile)
    }

    private persistInspectorProperties() {
        setCommonProperties(inspectorConfig)
        inspectorConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_REPOS.getKey(), configurationProperties.hubArtifactoryInspectRepositoriesList)
        inspectorConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_REPOS_CSV_PATH.getKey(), configurationProperties.hubArtifactoryInspectRepositoriesCsvPath)
        inspectorConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_PATTERNS_RUBYGEMS.getKey(), configurationProperties.hubArtifactoryInspectPatternsRubygems)
        inspectorConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_PATTERNS_MAVEN.getKey(), configurationProperties.hubArtifactoryInspectPatternsMaven)
        inspectorConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_PATTERNS_GRADLE.getKey(), configurationProperties.hubArtifactoryInspectPatternsGradle)
        inspectorConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_PATTERNS_PYPI.getKey(), configurationProperties.hubArtifactoryInspectPatternsPypi)
        inspectorConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_PATTERNS_NUGET.getKey(), configurationProperties.hubArtifactoryInspectPatternsNuget)
        inspectorConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_INSPECT_PATTERNS_NPM.getKey(), configurationProperties.hubArtifactoryInspectPatternsNpm)

        persistConfigToFile(inspectorConfig, inspectorPropertiesFile)
    }

    private persistConfigToFile(PropertiesConfiguration config, File outputFile) {
        FileHandler handler = new FileHandler(config);
        handler.save(outputFile);
    }

    private String setValueFromInput(Console console, PrintStream out, String propertyName, String oldValue) {
        out.print("Enter ${propertyName} (current value=\"${oldValue}\"): ")
        String userValue = StringUtils.trimToEmpty(console.readLine())
        if (StringUtils.isNotBlank(userValue)) {
            userValue
        } else {
            oldValue
        }
    }
}