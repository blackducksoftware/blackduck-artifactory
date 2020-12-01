/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.artifactory;

import java.util.List;
import java.util.stream.Collectors;

import org.artifactory.repo.RepoPath;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;

// TODO: Move search services from ArtifactoryPropertyService to here.
public class ArtifactSearchService {
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ArtifactoryPropertyService artifactoryPropertyService;

    public ArtifactSearchService(ArtifactoryPAPIService artifactoryPAPIService, ArtifactoryPropertyService artifactoryPropertyService) {
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.artifactoryPropertyService = artifactoryPropertyService;
    }

    public List<RepoPath> findArtifactsUsingComponentNameVersions(String componentName, String componentVersionName, List<RepoPath> repoKeyPaths) {
        List<String> repoKeys = repoKeyPaths.stream().map(RepoPath::getRepoKey).collect(Collectors.toList());
        return findArtifactsWithComponentNameVersion(componentName, componentVersionName, repoKeys);
    }

    public List<RepoPath> findArtifactsWithComponentNameVersion(String componentName, String componentVersionName, List<String> repoKeys) {
        SetMultimap<String, String> setMultimap = HashMultimap.create();
        setMultimap.put(BlackDuckArtifactoryProperty.COMPONENT_NAME_VERSION.getPropertyName(), String.format(InspectionPropertyService.COMPONENT_NAME_VERSION_FORMAT, componentName, componentVersionName));

        return artifactoryPropertyService.getItemsContainingPropertiesAndValues(setMultimap, repoKeys.toArray(new String[0]));
    }

    public List<RepoPath> findArtifactByName(String artifactName, String... repoKeys) {
        return artifactoryPAPIService.itemsByName(artifactName, repoKeys);
    }
}
