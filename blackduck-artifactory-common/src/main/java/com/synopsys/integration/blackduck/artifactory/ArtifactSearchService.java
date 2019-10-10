/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2019 Synopsys, Inc.
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

import org.artifactory.repo.RepoPath;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

// TODO: Move search services from ArtifactoryPropertyService to here.
public class ArtifactSearchService {
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ArtifactoryPropertyService artifactoryPropertyService;

    public ArtifactSearchService(final ArtifactoryPAPIService artifactoryPAPIService, final ArtifactoryPropertyService artifactoryPropertyService) {
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.artifactoryPropertyService = artifactoryPropertyService;
    }

    public List<RepoPath> findArtifactsWithOriginId(final String forge, final String originId, final String... repoKeys) {
        final SetMultimap<String, String> setMultimap = HashMultimap.create();
        setMultimap.put(BlackDuckArtifactoryProperty.BLACKDUCK_FORGE.getPropertyName(), forge);
        setMultimap.put(BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID.getPropertyName(), originId);

        return artifactoryPropertyService.getItemsContainingPropertiesAndValues(setMultimap, repoKeys);
    }

    public List<RepoPath> findArtifactByName(final String artifactName, final String... repoKeys) {
        return artifactoryPAPIService.itemsByName(artifactName, repoKeys);
    }
}
