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
package com.synopsys.integration.blackduck.artifactory.scan

import com.synopsys.integration.blackduck.artifactory.CommonConfigurationManager
import com.synopsys.integration.blackduck.artifactory.ConfigurationProperties
import embedded.org.apache.commons.lang3.StringUtils
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.fluent.Configurations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils

import javax.annotation.PostConstruct

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
        def pluginsDirectory = new File(configurationProperties.currentUserDirectory, 'plugins')
        def libDirectory = new File(pluginsDirectory, 'lib')
        def configs = new Configurations()

        scannerPropertiesFile = new File(libDirectory, 'blackDuckPlugin.properties')
        if (!scannerPropertiesFile.exists()) {
            scannerConfig = configs.properties(ResourceUtils.getFile('blackDuckPlugin.properties'))
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
            || StringUtils.isBlank(configurationProperties.hubArtifactoryScanCron)
            || StringUtils.isBlank(configurationProperties.hubArtifactoryScanAddPolicyStatusCron)
            || StringUtils.isBlank(configurationProperties.hubArtifactoryScanRepoPathCodelocation)
            || commonConfigurationManager.needsBaseConfigUpdate())
    }

    void configure(Console console, PrintStream out) {
        updateValues(console, out)
        persistScannerProperties()
        visualValidation(console, out)
        volumeTest(console, out)
    }

    void updateValues(Console console, PrintStream out) {
        commonConfigurationManager.updateBaseConfigValues(scannerConfig, scannerPropertiesFile, console, out)

        configurationProperties.hubArtifactoryScanBinariesDirectoryPath = setValueFromInput(console, out, 'Plugin Scan Binaries Directory', ScanModuleProperty.BINARIES_DIRECTORY_PATH)
        configurationProperties.hubArtifactoryScanMemory = setValueFromInput(console, out, 'Scan Memory Allocation', ScanModuleProperty.MEMORY)
        configurationProperties.hubArtifactoryScanDryRun = setValueFromInput(console, out, 'Scan Dry Run', ScanModuleProperty.DRY_RUN)
        configurationProperties.hubArtifactoryScanDateTimePattern = setValueFromInput(console, out, 'Scan Date Time Pattern', ScanModuleProperty.DATE_TIME_PATTERN)
        configurationProperties.hubArtifactoryScanCutoffDate = setValueFromInput(console, out, 'Scan Cutoff Date', ScanModuleProperty.CUTOFF_DATE)
        configurationProperties.hubArtifactoryScanNamePatterns = setValueFromInput(console, out, 'Scan Artifact Patterns', ScanModuleProperty.NAME_PATTERNS)
        configurationProperties.hubArtifactoryScanRepoPathCodelocation = setValueFromInput(console, out, 'Use Repo Path as Codelocation Name', ScanModuleProperty.REPO_PATH_CODELOCATION)

        String reposToSearch = configurationProperties.hubArtifactoryScanRepositoriesList
        out.println('The artifactory scanner can be configured to either read a list of repositories to scan, or a file containing a comma separated list of repositories.')
        out.println('If you would like to provide a path to a file, enter \'y\' now. Otherwise, just press <enter> to manually add a list of repositories.')
        String userValue = StringUtils.trimToEmpty(console.readLine())
        if ('y' == userValue) {
            configurationProperties.hubArtifactoryScanRepositoriesCsvPath = setValueFromInput(console, out, 'Path to File of Artifactory Repositories to Scan', ScanModuleProperty.REPOS_CSV_PATH)
        } else {
            configurationProperties.hubArtifactoryScanRepositoriesList = setValueFromInput(console, out, 'Artifactory Repositories to Scan', ScanModuleProperty.REPOS)
            configurationProperties.hubArtifactoryScanRepositoriesCsvPath = ''
        }

        configurationProperties.hubArtifactoryScanCron = setCronFromInput(console, out, 'blackDuckScan CRON Expression', ScanModuleProperty.SCAN_CRON)
        configurationProperties.hubArtifactoryScanAddPolicyStatusCron = setCronFromInput(console, out, 'blackDuckAddPolicyStatus CRON Expression', ScanModuleProperty.ADD_POLICY_STATUS_CRON)
    }

    void persistScannerProperties() {
        scannerConfig.setProperty(ScanModuleProperty.ADD_POLICY_STATUS_CRON.getKey(), configurationProperties.hubArtifactoryScanAddPolicyStatusCron)
        scannerConfig.setProperty(ScanModuleProperty.BINARIES_DIRECTORY_PATH.getKey(), configurationProperties.hubArtifactoryScanBinariesDirectoryPath)
        scannerConfig.setProperty(ScanModuleProperty.CUTOFF_DATE.getKey(), configurationProperties.hubArtifactoryScanCutoffDate)
        scannerConfig.setProperty(ScanModuleProperty.DATE_TIME_PATTERN.getKey(), configurationProperties.hubArtifactoryScanDateTimePattern)
        scannerConfig.setProperty(ScanModuleProperty.DRY_RUN.getKey(), configurationProperties.hubArtifactoryScanDryRun)
        scannerConfig.setProperty(ScanModuleProperty.MEMORY.getKey(), configurationProperties.hubArtifactoryScanMemory)
        scannerConfig.setProperty(ScanModuleProperty.NAME_PATTERNS.getKey(), configurationProperties.hubArtifactoryScanNamePatterns)
        scannerConfig.setProperty(ScanModuleProperty.REPOS.getKey(), configurationProperties.hubArtifactoryScanRepositoriesList)
        scannerConfig.setProperty(ScanModuleProperty.REPOS_CSV_PATH.getKey(), configurationProperties.hubArtifactoryScanRepositoriesCsvPath)
        scannerConfig.setProperty(ScanModuleProperty.REPO_PATH_CODELOCATION.getKey(), configurationProperties.hubArtifactoryScanRepoPathCodelocation)
        scannerConfig.setProperty(ScanModuleProperty.SCAN_CRON.getKey(), configurationProperties.hubArtifactoryScanCron)

        commonConfigurationManager.persistConfigToFile(scannerConfig, scannerPropertiesFile)
    }

    void visualValidation(Console console, PrintStream out) {
        String repositoriesString
        if (StringUtils.isNotBlank(configurationProperties.hubArtifactoryScanRepositoriesCsvPath)) {
            repositoriesString = "the repositories listed in \'${configurationProperties.hubArtifactoryScanRepositoriesCsvPath}\'"
        } else {
            repositoriesString = "the repositories \'${configurationProperties.hubArtifactoryScanRepositoriesList}\'"
        }
        out.println("The Artifactory Scanner will search ${repositoriesString} for artifacts matching \'${configurationProperties.hubArtifactoryScanNamePatterns}\', then scan them.")
        commonConfigurationManager.printVisualValidationOfCron(out, 'blackDuckScan', configurationProperties.hubArtifactoryScanCron)
        commonConfigurationManager.printVisualValidationOfCron(out, 'blackDuckAddPolicyStatus', configurationProperties.hubArtifactoryScanAddPolicyStatusCron)
        out.println('If this is incorrect, enter \'n\' to enter new values, if this is correct, just press <enter>.')
        String userValue = StringUtils.trimToEmpty(console.readLine())
        if ('n' == userValue) {
            configure(console, out)
        }
    }

    void volumeTest(Console console, PrintStream out) {
        String lengthWarning = ''
        def repositories = StringUtils.isNotBlank(configurationProperties.hubArtifactoryScanRepositoriesList) ? configurationProperties.hubArtifactoryScanRepositoriesList.tokenize(',') : []
        if (StringUtils.isNotBlank(configurationProperties.hubArtifactoryScanRepositoriesCsvPath) || repositories.size() > 10) {
            lengthWarning = ' (this may take a while)'
        }

        out.println("If you would like to volume test your configuration, enter \'y\'${lengthWarning}. Otherwise, just press <enter> to skip testing.")
        String userValue = StringUtils.trimToEmpty(console.readLine())
        if ('y' == userValue) {
            if (commonConfigurationManager.updateArtifactoryConnectionValues(console, out)) {
                def repositoriesToSearch = StringUtils.isNotBlank(configurationProperties.hubArtifactoryScanRepositoriesList) ? configurationProperties.hubArtifactoryScanRepositoriesList.tokenize(',') : []
                def patternsToSearch = StringUtils.isNotBlank(configurationProperties.hubArtifactoryScanNamePatterns) ? configurationProperties.hubArtifactoryScanNamePatterns.tokenize(',') : []
                int matches = commonConfigurationManager.getArtifactCount(repositoriesToSearch, patternsToSearch)
                out.println("Found ${matches} artifacts in ${configurationProperties.hubArtifactoryScanRepositoriesList} matching the patterns ${configurationProperties.hubArtifactoryScanNamePatterns}")
            }
        }
    }

    String setValueFromInput(Console console, PrintStream out, String propertyDescription, ScanModuleProperty property) {
        return commonConfigurationManager.setValueFromInput(console, out, propertyDescription, scannerConfig, property)
    }

    String setCronFromInput(Console console, PrintStream out, String propertyDescription, ScanModuleProperty property) {
        return commonConfigurationManager.setCronFromInput(console, out, propertyDescription, scannerConfig, property)
    }
}
