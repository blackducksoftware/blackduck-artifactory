/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationPropertyManager;
import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyGroupReport;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;

public class InspectionModuleConfig extends ModuleConfig {
    private final String inspectionCron;
    private final String reinspectCron;
    private final Boolean metadataBlockEnabled;
    private final Map<SupportedPackageType, List<String>> patternMap;
    private final List<String> repos;
    private final Integer retryCount;

    public InspectionModuleConfig(Boolean enabled, String blackDuckIdentifyArtifactsCron, String reinspectCron, Boolean metadataBlockEnabled, Map<SupportedPackageType, List<String>> patternMap,
        List<String> repos, int retryCount) {
        super(InspectionModule.class.getSimpleName(), enabled);
        this.inspectionCron = blackDuckIdentifyArtifactsCron;
        this.reinspectCron = reinspectCron;
        this.metadataBlockEnabled = metadataBlockEnabled;
        this.patternMap = patternMap;
        this.repos = repos;
        this.retryCount = retryCount;
    }

    public static InspectionModuleConfig createFromProperties(ConfigurationPropertyManager configurationPropertyManager, ArtifactoryPAPIService artifactoryPAPIService) throws IOException {
        Boolean enabled = configurationPropertyManager.getBooleanProperty(InspectionModuleProperty.ENABLED);
        String blackDuckIdentifyArtifactsCron = configurationPropertyManager.getProperty(InspectionModuleProperty.CRON);
        String reinspectCron = configurationPropertyManager.getProperty(InspectionModuleProperty.REINSPECT_CRON);
        Boolean metadataBlockEnabled = configurationPropertyManager.getBooleanProperty(InspectionModuleProperty.METADATA_BLOCK);

        Map<SupportedPackageType, List<String>> patternMap = Arrays.stream(SupportedPackageType.values())
                                                                       .collect(Collectors.toMap(Function.identity(), supportedPackageType -> configurationPropertyManager.getPropertyAsList(supportedPackageType.getPatternProperty())));

        List<String> repos = configurationPropertyManager.getRepositoryKeysFromProperties(InspectionModuleProperty.REPOS, InspectionModuleProperty.REPOS_CSV_PATH).stream()
                                       .filter(artifactoryPAPIService::isValidRepository)
                                       .collect(Collectors.toList());
        Integer retryCount = configurationPropertyManager.getIntegerProperty(InspectionModuleProperty.RETRY_COUNT);

        return new InspectionModuleConfig(enabled, blackDuckIdentifyArtifactsCron, reinspectCron, metadataBlockEnabled, patternMap, repos, retryCount);
    }

    @Override
    public void validate(PropertyGroupReport propertyGroupReport) {
        validateBoolean(propertyGroupReport, InspectionModuleProperty.ENABLED, isEnabledUnverified());
        validateCronExpression(propertyGroupReport, InspectionModuleProperty.CRON, inspectionCron);
        validateCronExpression(propertyGroupReport, InspectionModuleProperty.REINSPECT_CRON, reinspectCron);
        validateBoolean(propertyGroupReport, InspectionModuleProperty.METADATA_BLOCK, metadataBlockEnabled);
        Arrays.stream(SupportedPackageType.values())
            .forEach(packageType -> validateList(propertyGroupReport, packageType.getPatternProperty(), getPatternsForPackageType(packageType)));
        validateList(propertyGroupReport, InspectionModuleProperty.REPOS, repos,
            String.format("No valid repositories specified. Please set the %s or %s property with valid repositories", InspectionModuleProperty.REPOS.getKey(), InspectionModuleProperty.REPOS_CSV_PATH.getKey()));
        validateInteger(propertyGroupReport, InspectionModuleProperty.RETRY_COUNT, retryCount, 0, Integer.MAX_VALUE);
    }

    public String getInspectionCron() {
        return inspectionCron;
    }

    public String getReinspectCron() {
        return reinspectCron;
    }

    public Boolean isMetadataBlockEnabled() {
        return metadataBlockEnabled;
    }

    public List<String> getRepos() {
        return repos;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public List<String> getPatternsForPackageType(String packageType) {
        return SupportedPackageType.getAsSupportedPackageType(packageType)
                   .map(patternMap::get)
                   .orElse(Collections.emptyList());
    }

    public List<String> getPatternsForPackageType(SupportedPackageType packageType) {
        return patternMap.get(packageType);
    }
}
