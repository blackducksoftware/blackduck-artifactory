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
package com.synopsys.integration.blackduck.artifactory.inspect

import com.synopsys.integration.blackduck.artifactory.BlackDuckProperty
import com.synopsys.integration.blackduck.artifactory.CommonConfigurationManager
import com.synopsys.integration.blackduck.artifactory.ConfigurationProperties
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleProperty
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.fluent.Configurations
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils

import javax.annotation.PostConstruct

@Component
class InspectorConfigurationManager {
    @Autowired
    CommonConfigurationManager commonConfigurationManager

    @Autowired
    ConfigurationProperties configurationProperties

    File inspectorPropertiesFile
    PropertiesConfiguration inspectorConfig

    @PostConstruct
    void init() {
        def pluginsDirectory = new File(configurationProperties.currentUserDirectory, 'plugins')
        def libDirectory = new File(pluginsDirectory, 'lib')
        def configs = new Configurations()

        inspectorPropertiesFile = new File(libDirectory, 'blackDuckPlugin.properties')
        if (!inspectorPropertiesFile.exists()) {
            inspectorConfig = configs.properties(ResourceUtils.getFile('blackDuckPlugin.properties'))
            libDirectory.mkdirs()
            persistInspectorProperties()
        }
        inspectorConfig = configs.properties(inspectorPropertiesFile)
    }

    boolean needsUpdate() {
        return ((StringUtils.isBlank(configurationProperties.hubArtifactoryInspectRepositoriesList) && StringUtils.isBlank(configurationProperties.hubArtifactoryInspectRepositoriesCsvPath))
            || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectPatternsRubygems)
            || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectPatternsMaven)
            || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectPatternsGradle)
            || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectPatternsPypi)
            || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectPatternsNuget)
            || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectPatternsNpm)
            || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectDateTimePattern)
            || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectIdentifyArtifactsCron)
            || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectPopulateMetadataCron)
            || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectUpdateMetadataCron)
            || StringUtils.isBlank(configurationProperties.hubArtifactoryInspectAddPendingArtifactsCron)
            || commonConfigurationManager.needsBaseConfigUpdate())
    }

    void configure(Console console, PrintStream out) {
        updateValues(console, out)
        persistInspectorProperties()
        visualValidation(console, out)
        volumeTest(console, out)
    }

    void updateValues(Console console, PrintStream out) {
        commonConfigurationManager.updateBaseConfigValues(inspectorConfig, inspectorPropertiesFile, console, out)

        configurationProperties.hubArtifactoryInspectPatternsRubygems = setValueFromInput(console, out, 'Rubygems Artifact Patterns', InspectPluginProperty.PATTERNS_RUBYGEMS)
        configurationProperties.hubArtifactoryInspectPatternsMaven = setValueFromInput(console, out, 'Maven Artifact Patterns', InspectPluginProperty.PATTERNS_MAVEN)
        configurationProperties.hubArtifactoryInspectPatternsGradle = setValueFromInput(console, out, 'Gradle Artifact Patterns', InspectPluginProperty.PATTERNS_GRADLE)
        configurationProperties.hubArtifactoryInspectPatternsPypi = setValueFromInput(console, out, 'Pypi Artifact Patterns', InspectPluginProperty.PATTERNS_PYPI)
        configurationProperties.hubArtifactoryInspectPatternsNuget = setValueFromInput(console, out, 'Nuget Artifact Patterns', InspectPluginProperty.PATTERNS_NUGET)
        configurationProperties.hubArtifactoryInspectPatternsNpm = setValueFromInput(console, out, 'NPM Artifact Patterns', InspectPluginProperty.PATTERNS_NPM)
        configurationProperties.hubArtifactoryInspectDateTimePattern = setValueFromInput(console, out, 'Inspection Date Time Pattern', InspectPluginProperty.DATE_TIME_PATTERN)

        out.println('The artifactory inspector can be configured to either read a list of repositories to inspct, or a file containing a comma separated list of repositories.')
        out.println('If you would like to provide a path to a file, enter \'y\' now. Otherwise, just press <enter> to manually add a list of repositories.')
        String userValue = StringUtils.trimToEmpty(console.readLine())
        if ('y' == userValue) {
            configurationProperties.hubArtifactoryInspectRepositoriesCsvPath = setValueFromInput(console, out, 'Path to File of Artifactory Repositories to Inspect', InspectPluginProperty.REPOS_CSV_PATH)
        } else {
            configurationProperties.hubArtifactoryInspectRepositoriesList = setValueFromInput(console, out, 'Artifactory Repositories to Inspect', InspectPluginProperty.REPOS)
            configurationProperties.hubArtifactoryInspectRepositoriesCsvPath = ''
        }

        configurationProperties.hubArtifactoryInspectIdentifyArtifactsCron = setCronFromInput(console, out, 'blackDuckIdentifyArtifacts CRON Expression', InspectPluginProperty.IDENTIFY_ARTIFACTS_CRON)
        configurationProperties.hubArtifactoryInspectPopulateMetadataCron = setCronFromInput(console, out, 'blackDuckPopulateMetadata CRON Expression', InspectPluginProperty.POPULATE_METADATA_CRON)
        configurationProperties.hubArtifactoryInspectUpdateMetadataCron = setCronFromInput(console, out, 'blackDuckUpdateMetadata CRON Expression', InspectPluginProperty.UPDATE_METADATA_CRON)
        configurationProperties.hubArtifactoryInspectAddPendingArtifactsCron = setCronFromInput(console, out, 'blackDuckAddPendingArtifacts CRON Expression', InspectPluginProperty.ADD_PENDING_ARTIFACTS_CRON)
    }

    void persistInspectorProperties() {
        inspectorConfig.setProperty(InspectionModuleProperty.REPOS.getKey(), configurationProperties.hubArtifactoryInspectRepositoriesList)
        inspectorConfig.setProperty(InspectionModuleProperty.REPOS_CSV_PATH.getKey(), configurationProperties.hubArtifactoryInspectRepositoriesCsvPath)
        inspectorConfig.setProperty(InspectionModuleProperty.PATTERNS_RUBYGEMS.getKey(), configurationProperties.hubArtifactoryInspectPatternsRubygems)
        inspectorConfig.setProperty(InspectionModuleProperty.PATTERNS_MAVEN.getKey(), configurationProperties.hubArtifactoryInspectPatternsMaven)
        inspectorConfig.setProperty(InspectionModuleProperty.PATTERNS_GRADLE.getKey(), configurationProperties.hubArtifactoryInspectPatternsGradle)
        inspectorConfig.setProperty(InspectionModuleProperty.PATTERNS_PYPI.getKey(), configurationProperties.hubArtifactoryInspectPatternsPypi)
        inspectorConfig.setProperty(InspectionModuleProperty.PATTERNS_NUGET.getKey(), configurationProperties.hubArtifactoryInspectPatternsNuget)
        inspectorConfig.setProperty(InspectionModuleProperty.PATTERNS_NPM.getKey(), configurationProperties.hubArtifactoryInspectPatternsNpm)
        inspectorConfig.setProperty(BlackDuckProperty.DATE_TIME_PATTERN.getKey(), configurationProperties.hubArtifactoryInspectDateTimePattern)

        commonConfigurationManager.persistConfigToFile(inspectorConfig, inspectorPropertiesFile)
    }

    void visualValidation(Console console, PrintStream out) {
        String repositoriesString
        if (StringUtils.isNotBlank(configurationProperties.hubArtifactoryInspectRepositoriesCsvPath)) {
            repositoriesString = "The repositories listed in \'${configurationProperties.hubArtifactoryInspectRepositoriesCsvPath}\'"
        } else {
            repositoriesString = "The repositories \'${configurationProperties.hubArtifactoryInspectRepositoriesList}\'"
        }
        out.println("${repositoriesString} will be searched for artifacts.")
        out.println("Artifacts in Rubygems repositories will be identified for inspection by the following patterns: \'${configurationProperties.hubArtifactoryInspectPatternsRubygems}\'")
        out.println("Artifacts in Maven repositories will be identified for inspection by the following patterns: \'${configurationProperties.hubArtifactoryInspectPatternsMaven}\'")
        out.println("Artifacts in Gradle repositories will be identified for inspection by the following patterns: \'${configurationProperties.hubArtifactoryInspectPatternsGradle}\'")
        out.println("Artifacts in Pypi repositories will be identified for inspection by the following patterns: \'${configurationProperties.hubArtifactoryInspectPatternsPypi}\'")
        out.println("Artifacts in Nuget repositories will be identified for inspection by the following patterns: \'${configurationProperties.hubArtifactoryInspectPatternsNuget}\'")
        out.println("Artifacts in NPM repositories will be identified for inspection by the following patterns: \'${configurationProperties.hubArtifactoryInspectPatternsNpm}\'")
        commonConfigurationManager.printVisualValidationOfCron(out, 'blackDuckIdentifyArtifacts', configurationProperties.hubArtifactoryInspectIdentifyArtifactsCron)
        commonConfigurationManager.printVisualValidationOfCron(out, 'blackDuckPopulateMetadata', configurationProperties.hubArtifactoryInspectPopulateMetadataCron)
        commonConfigurationManager.printVisualValidationOfCron(out, 'blackDuckUpdateMetadata', configurationProperties.hubArtifactoryInspectUpdateMetadataCron)
        commonConfigurationManager.printVisualValidationOfCron(out, 'blackDuckAddPendingArtifacts', configurationProperties.hubArtifactoryInspectAddPendingArtifactsCron)
        out.println('If this is incorrect, enter \'n\' to enter new values, if this is correct, just press <enter>.')
        String userValue = StringUtils.trimToEmpty(console.readLine())
        if ('n' == userValue) {
            configure(console, out)
        }
    }

    void volumeTest(Console console, PrintStream out) {
        String lengthWarning = ''
        def repositories = StringUtils.isNotBlank(configurationProperties.hubArtifactoryInspectRepositoriesList) ? configurationProperties.hubArtifactoryInspectRepositoriesList.tokenize(',') : []
        if (StringUtils.isNotBlank(configurationProperties.hubArtifactoryInspectRepositoriesCsvPath) || repositories.size() > 10) {
            lengthWarning = ' (this may take a while)'
        }

        out.println("If you would like to volume test your configuration, enter \'y\'${lengthWarning}. Otherwise, just press <enter> to skip testing.")
        String userValue = StringUtils.trimToEmpty(console.readLine())
        if ('y' == userValue) {
            if (commonConfigurationManager.updateArtifactoryConnectionValues(console, out)) {
                def repositoriesToSearch = commonConfigurationManager.getRepositoriesToSearch(configurationProperties.hubArtifactoryInspectRepositoriesCsvPath, configurationProperties.hubArtifactoryInspectRepositoriesList)
                def patternsToSearch = []
                patternsToSearch.addAll(StringUtils.isNotBlank(configurationProperties.hubArtifactoryInspectPatternsRubygems) ? configurationProperties.hubArtifactoryInspectPatternsRubygems.tokenize(',') : [])
                patternsToSearch.addAll(StringUtils.isNotBlank(configurationProperties.hubArtifactoryInspectPatternsMaven) ? configurationProperties.hubArtifactoryInspectPatternsMaven.tokenize(',') : [])
                patternsToSearch.addAll(StringUtils.isNotBlank(configurationProperties.hubArtifactoryInspectPatternsGradle) ? configurationProperties.hubArtifactoryInspectPatternsGradle.tokenize(',') : [])
                patternsToSearch.addAll(StringUtils.isNotBlank(configurationProperties.hubArtifactoryInspectPatternsPypi) ? configurationProperties.hubArtifactoryInspectPatternsPypi.tokenize(',') : [])
                patternsToSearch.addAll(StringUtils.isNotBlank(configurationProperties.hubArtifactoryInspectPatternsNuget) ? configurationProperties.hubArtifactoryInspectPatternsNuget.tokenize(',') : [])
                patternsToSearch.addAll(StringUtils.isNotBlank(configurationProperties.hubArtifactoryInspectPatternsNpm) ? configurationProperties.hubArtifactoryInspectPatternsNpm.tokenize(',') : [])
                int matches = commonConfigurationManager.getArtifactCount(repositoriesToSearch, patternsToSearch)
                out.println("Found ${matches} artifacts in ${configurationProperties.hubArtifactoryInspectRepositoriesList} matching the configured artifact patterns. This number may be inflated by false positives.")
            }
        }
    }

    String setValueFromInput(Console console, PrintStream out, String propertyDescription, InspectionModuleProperty property) {
        return commonConfigurationManager.setValueFromInput(console, out, propertyDescription, inspectorConfig, property)
    }

    String setCronFromInput(Console console, PrintStream out, String propertyDescription, InspectionModuleProperty property) {
        return commonConfigurationManager.setCronFromInput(console, out, propertyDescription, inspectorConfig, property)
    }
}
