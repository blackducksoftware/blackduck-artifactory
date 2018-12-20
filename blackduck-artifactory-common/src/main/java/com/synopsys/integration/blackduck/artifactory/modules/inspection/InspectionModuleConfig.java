/**
 * blackduck-artifactory-common
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
package com.synopsys.integration.blackduck.artifactory.modules.inspection;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.ConfigurationPropertyManager;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;
import com.synopsys.integration.util.BuilderStatus;

public class InspectionModuleConfig extends ModuleConfig {
    private final String identifyArtifactsCron;
    private final List<String> patternsRubygems;
    private final List<String> patternsMaven;
    private final List<String> patternsGradle;
    private final List<String> patternsPypi;
    private final List<String> patternsNuget;
    private final List<String> patternsNpm;
    private final String populateMetadataCron;
    private final List<String> repos;
    private final String updateMetadataCron;

    public InspectionModuleConfig(final Boolean enabled, final String blackDuckIdentifyArtifactsCron, final List<String> patternsRubygems, final List<String> patternsMaven, final List<String> patternsGradle, final List<String> patternsPypi,
        final List<String> patternsNuget, final List<String> patternsNpm, final String blackDuckPopulateMetadataCron, final String blackDuckUpdateMetadataCron, final List<String> repos) {
        super(InspectionModule.class.getSimpleName(), enabled);
        this.identifyArtifactsCron = blackDuckIdentifyArtifactsCron;
        this.patternsRubygems = patternsRubygems;
        this.patternsMaven = patternsMaven;
        this.patternsGradle = patternsGradle;
        this.patternsPypi = patternsPypi;
        this.patternsNuget = patternsNuget;
        this.patternsNpm = patternsNpm;
        this.populateMetadataCron = blackDuckPopulateMetadataCron;
        this.updateMetadataCron = blackDuckUpdateMetadataCron;
        this.repos = repos;
    }

    public static InspectionModuleConfig createFromProperties(final ConfigurationPropertyManager configurationPropertyManager, final ArtifactoryPAPIService artifactoryPAPIService) throws IOException {
        final Boolean enabled = configurationPropertyManager.getBooleanProperty(InspectionModuleProperty.ENABLED);
        final String blackDuckIdentifyArtifactsCron = configurationPropertyManager.getProperty(InspectionModuleProperty.IDENTIFY_ARTIFACTS_CRON);
        final List<String> patternsRubygems = configurationPropertyManager.getPropertyAsList(InspectionModuleProperty.PATTERNS_RUBYGEMS, configurationPropertyManager::getProperty);
        final List<String> patternsMaven = configurationPropertyManager.getPropertyAsList(InspectionModuleProperty.PATTERNS_MAVEN, configurationPropertyManager::getProperty);
        final List<String> patternsGradle = configurationPropertyManager.getPropertyAsList(InspectionModuleProperty.PATTERNS_GRADLE, configurationPropertyManager::getProperty);
        final List<String> patternsPypi = configurationPropertyManager.getPropertyAsList(InspectionModuleProperty.PATTERNS_PYPI, configurationPropertyManager::getProperty);
        final List<String> patternsNuget = configurationPropertyManager.getPropertyAsList(InspectionModuleProperty.PATTERNS_NUGET, configurationPropertyManager::getProperty);
        final List<String> patternsNpm = configurationPropertyManager.getPropertyAsList(InspectionModuleProperty.PATTERNS_NPM, configurationPropertyManager::getProperty);
        final String blackDuckPopulateMetadataCron = configurationPropertyManager.getProperty(InspectionModuleProperty.POPULATE_METADATA_CRON);
        final String blackDuckUpdateMetadataCron = configurationPropertyManager.getProperty(InspectionModuleProperty.UPDATE_METADATA_CRON);
        final List<String> repos = configurationPropertyManager.getRepositoryKeysFromProperties(InspectionModuleProperty.REPOS, InspectionModuleProperty.REPOS_CSV_PATH).stream()
                                       .filter(artifactoryPAPIService::isValidRepository)
                                       .collect(Collectors.toList());

        return new InspectionModuleConfig(enabled, blackDuckIdentifyArtifactsCron, patternsRubygems, patternsMaven, patternsGradle, patternsPypi, patternsNuget, patternsNpm, blackDuckPopulateMetadataCron, blackDuckUpdateMetadataCron,
            repos);
    }

    @Override
    public void validate(final BuilderStatus builderStatus) {
        validateBoolean(builderStatus, InspectionModuleProperty.ENABLED, isEnabledUnverified());
        validateCronExpression(builderStatus, InspectionModuleProperty.IDENTIFY_ARTIFACTS_CRON, identifyArtifactsCron);
        validateNotNull(builderStatus, InspectionModuleProperty.PATTERNS_RUBYGEMS, patternsRubygems);
        validateNotNull(builderStatus, InspectionModuleProperty.PATTERNS_MAVEN, patternsMaven);
        validateNotNull(builderStatus, InspectionModuleProperty.PATTERNS_GRADLE, patternsGradle);
        validateNotNull(builderStatus, InspectionModuleProperty.PATTERNS_PYPI, patternsPypi);
        validateNotNull(builderStatus, InspectionModuleProperty.PATTERNS_NUGET, patternsNuget);
        validateNotNull(builderStatus, InspectionModuleProperty.PATTERNS_NPM, patternsNpm);
        validateCronExpression(builderStatus, InspectionModuleProperty.POPULATE_METADATA_CRON, populateMetadataCron);
        validateList(builderStatus, InspectionModuleProperty.REPOS, repos,
            String.format("No valid repositories specified. Please set the %s or %s property with valid repositories", InspectionModuleProperty.REPOS.getKey(), InspectionModuleProperty.REPOS_CSV_PATH.getKey()));
        validateCronExpression(builderStatus, InspectionModuleProperty.UPDATE_METADATA_CRON, updateMetadataCron);
    }

    public String getIdentifyArtifactsCron() {
        return identifyArtifactsCron;
    }

    public String getPopulateMetadataCron() {
        return populateMetadataCron;
    }

    public String getUpdateMetadataCron() {
        return updateMetadataCron;
    }

    public List<String> getRepos() {
        return repos;
    }

    public List<String> getPatternsRubygems() {
        return patternsRubygems;
    }

    public List<String> getPatternsMaven() {
        return patternsMaven;
    }

    public List<String> getPatternsGradle() {
        return patternsGradle;
    }

    public List<String> getPatternsPypi() {
        return patternsPypi;
    }

    public List<String> getPatternsNuget() {
        return patternsNuget;
    }

    public List<String> getPatternsNpm() {
        return patternsNpm;
    }
}
