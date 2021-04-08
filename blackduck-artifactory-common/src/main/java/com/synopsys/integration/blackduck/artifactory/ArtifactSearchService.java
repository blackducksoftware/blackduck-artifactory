/**
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
