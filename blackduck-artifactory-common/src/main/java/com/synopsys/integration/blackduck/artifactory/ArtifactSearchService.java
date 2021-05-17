/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory;

import java.util.List;
import java.util.stream.Collectors;

import org.artifactory.repo.RepoPath;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

// TODO: Move search services from ArtifactoryPropertyService to here.
public class ArtifactSearchService {
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ArtifactoryPropertyService artifactoryPropertyService;

    public ArtifactSearchService(ArtifactoryPAPIService artifactoryPAPIService, ArtifactoryPropertyService artifactoryPropertyService) {
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.artifactoryPropertyService = artifactoryPropertyService;
    }

    public List<RepoPath> findArtifactsWithComponentVersionId(String componentVersionId, List<RepoPath> repoKeyPaths) {
        List<String> repoKeys = repoKeyPaths.stream().map(RepoPath::getRepoKey).collect(Collectors.toList());
        return findArtifactsWithComponentVersionIdFromKeys(componentVersionId, repoKeys);
    }

    public List<RepoPath> findArtifactsWithComponentVersionIdFromKeys(String componentVersionId, List<String> repoKeys) {
        SetMultimap<String, String> setMultimap = HashMultimap.create();
        setMultimap.put(BlackDuckArtifactoryProperty.COMPONENT_VERSION_ID.getPropertyName(), componentVersionId);

        return artifactoryPropertyService.getItemsContainingPropertiesAndValues(setMultimap, repoKeys.toArray(new String[0]));
    }

    public List<RepoPath> findArtifactByName(String artifactName, String... repoKeys) {
        return artifactoryPAPIService.itemsByName(artifactName, repoKeys);
    }
}
