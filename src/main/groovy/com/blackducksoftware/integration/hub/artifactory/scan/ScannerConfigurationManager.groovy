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
package com.blackducksoftware.integration.hub.artifactory.scan

import javax.annotation.PostConstruct

import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.fluent.Configurations
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils

import com.blackducksoftware.integration.hub.artifactory.CommonConfigurationManager
import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperties
import com.blackducksoftware.integration.hub.artifactory.PluginProperty

@Component
class ScannerConfigurationManager {
    @Autowired
    CommonConfigurationManager commonConfigurationManager

    @Autowired
    ConfigurationProperties configurationProperties

    File scannerPropertiesFile
    PropertiesConfiguration scannerConfig

    @PostConstruct
    void init() {
        def libDirectory = new File (configurationProperties.currentUserDirectory, 'lib')
        def configs = new Configurations()

        scannerPropertiesFile = new File(libDirectory, 'blackDuckScanForHub.properties')
        if (!scannerPropertiesFile.exists()) {
            scannerConfig = configs.properties(ResourceUtils.getFile('blackDuckScanForHub.properties'))
            libDirectory.mkdirs()
            persistScannerProperties()
        }
        scannerConfig = configs.properties(scannerPropertiesFile)
    }

    boolean needsUpdate() {
        return ((StringUtils.isBlank(configurationProperties.hubArtifactoryScanRepositoriesList) && StringUtils.isBlank(configurationProperties.hubArtifactoryScanRepositoriesCsvPath))
                || StringUtils.isBlank(configurationProperties.hubArtifactoryScanNamePatterns)
                || StringUtils.isBlank(configurationProperties.hubArtifactoryScanBinariesDirectoryPath)
                || StringUtils.isBlank(configurationProperties.hubArtifactoryScanMemory)
                || StringUtils.isBlank(configurationProperties.hubArtifactoryScanDryRun)
                || StringUtils.isBlank(configurationProperties.hubArtifactoryScanDateTimePattern)
                || StringUtils.isBlank(configurationProperties.hubArtifactoryScanCutoffDate)
                || commonConfigurationManager.needsBaseConfigUpdate())
    }

    void updateValues(Console console, PrintStream out) {
        commonConfigurationManager.updateBaseConfigValues(scannerConfig, scannerPropertiesFile, console, out)

        configurationProperties.hubArtifactoryScanBinariesDirectoryPath = setValueFromInput(console, out, 'Plugin Scan Binaries Directory', PluginProperty.HUB_ARTIFACTORY_SCAN_BINARIES_DIRECTORY_PATH)
        configurationProperties.hubArtifactoryScanMemory = setValueFromInput(console, out, 'Scan Memory Allocation', PluginProperty.HUB_ARTIFACTORY_SCAN_MEMORY)
        configurationProperties.hubArtifactoryScanDryRun = setValueFromInput(console, out, 'Scan Dry Run', PluginProperty.HUB_ARTIFACTORY_SCAN_DRY_RUN)
        configurationProperties.hubArtifactoryScanDateTimePattern = setValueFromInput(console, out, 'Scan Date Time Pattern', PluginProperty.HUB_ARTIFACTORY_SCAN_DATE_TIME_PATTERN)
        configurationProperties.hubArtifactoryScanCutoffDate = setValueFromInput(console, out, 'Scan Cutoff Date', PluginProperty.HUB_ARTIFACTORY_SCAN_CUTOFF_DATE)
        configurationProperties.hubArtifactoryScanNamePatterns = setValueFromInput(console, out, 'Scan Artifact Patterns', PluginProperty.HUB_ARTIFACTORY_SCAN_NAME_PATTERNS)

        String reposToSearch = configurationProperties.hubArtifactoryScanRepositoriesList
        out.println('The artifactory scanner can be configured to either read a list of repositories to scan, or a file containing a comma separated list of repositories.')
        out.println('If you would like to provide a path to a file, enter \'y\' now. Otherwise, just press <enter> to manually add a list of repositories.')
        String userValue = StringUtils.trimToEmpty(console.readLine())
        if ('y' == userValue) {
            configurationProperties.hubArtifactoryScanRepositoriesCsvPath = setValueFromInput(console, out, 'Path to File of Artifactory Repositories to Scan',PluginProperty.HUB_ARTIFACTORY_SCAN_REPOS_CSV_PATH)
        } else {
            configurationProperties.hubArtifactoryScanRepositoriesList = setValueFromInput(console, out, 'Artifactory Repositories to Scan', PluginProperty.HUB_ARTIFACTORY_SCAN_REPOS)
            configurationProperties.hubArtifactoryScanRepositoriesCsvPath = ''
        }

        persistScannerProperties()

        String repositoriesString
        if (StringUtils.isNotBlank(configurationProperties.hubArtifactoryScanRepositoriesCsvPath)) {
            repositoriesString = "the repositories listed in \'${configurationProperties.hubArtifactoryScanRepositoriesCsvPath}\'"
        } else {
            repositoriesString = "the repositories \'${configurationProperties.hubArtifactoryScanRepositoriesList}\'"
        }
        out.println("The Artifactory Scanner will search ${repositoriesString} for artifacts matching \'${configurationProperties.hubArtifactoryScanNamePatterns}\', then scan them.")
        out.print('If this is incorrect, enter \'n\' to enter new values, if this is correct, just press <enter>.')
        userValue = StringUtils.trimToEmpty(console.readLine())
        if ('n' == userValue) {
            updateValues(console, out)
        }

        String lengthWarning = ''
        def repositories = StringUtils.isNotBlank(configurationProperties.hubArtifactoryScanRepositoriesList) ? configurationProperties.hubArtifactoryScanRepositoriesList.tokenize(',') : []
        if (StringUtils.isNotBlank(configurationProperties.hubArtifactoryScanRepositoriesCsvPath) || repositories.size() > 10) {
            lengthWarning = ' (this may take a while)'
        }

        out.println("If you would like to volume test your configuration, enter \'y\'${lengthWarning}. Otherwise, just press <enter> to skip testing.")
        userValue = StringUtils.trimToEmpty(console.readLine())
        if ('y' == userValue) {
            if (commonConfigurationManager.updateArtifactoryConnectionValues(console, out)) {
                def repositoriesToSearch = StringUtils.isNotBlank(configurationProperties.hubArtifactoryScanRepositoriesList) ? configurationProperties.hubArtifactoryScanRepositoriesList.tokenize(',') : []
                def patternsToSearch = StringUtils.isNotBlank(configurationProperties.hubArtifactoryScanNamePatterns) ? configurationProperties.hubArtifactoryScanNamePatterns.tokenize(',') : []
                int matches = commonConfigurationManager.getArtifactCount(repositoriesToSearch, patternsToSearch)
                out.println("Found ${matches} artifacts in ${configurationProperties.hubArtifactoryScanRepositoriesList} matching the patterns ${configurationProperties.hubArtifactoryScanNamePatterns}")
            }
        }
    }

    void persistScannerProperties() {
        scannerConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_NAME_PATTERNS.getKey(), configurationProperties.hubArtifactoryScanNamePatterns)
        scannerConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_MEMORY.getKey(), configurationProperties.hubArtifactoryScanMemory)
        scannerConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_DRY_RUN.getKey(), configurationProperties.hubArtifactoryScanDryRun)
        scannerConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_REPOS.getKey(), configurationProperties.hubArtifactoryScanRepositoriesList)
        scannerConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_REPOS_CSV_PATH.getKey(), configurationProperties.hubArtifactoryScanRepositoriesCsvPath)
        scannerConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_BINARIES_DIRECTORY_PATH.getKey(), configurationProperties.hubArtifactoryScanBinariesDirectoryPath)
        scannerConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_DATE_TIME_PATTERN.getKey(), configurationProperties.hubArtifactoryScanDateTimePattern)
        scannerConfig.setProperty(PluginProperty.HUB_ARTIFACTORY_SCAN_CUTOFF_DATE.getKey(), configurationProperties.hubArtifactoryScanCutoffDate)

        commonConfigurationManager.persistConfigToFile(scannerConfig, scannerPropertiesFile)
    }

    String setValueFromInput(Console console, PrintStream out, String propertyDescription, PluginProperty property) {
        return commonConfigurationManager.setValueFromInput(console, out, propertyDescription, scannerConfig, property)
    }
}
