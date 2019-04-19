package com.synopsys.integration.blackduck.artifactory;

import java.util.List;

import org.artifactory.repo.RepoPath;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class ArtifactSearchService {
    private final ArtifactoryPropertyService artifactoryPropertyService;

    public ArtifactSearchService(final ArtifactoryPropertyService artifactoryPropertyService) {
        this.artifactoryPropertyService = artifactoryPropertyService;
    }

    public List<RepoPath> findArtifactsWithOriginId(final String forge, final String originId, final String... repoKeys) {
        final SetMultimap<String, String> setMultimap = HashMultimap.create();
        setMultimap.put(BlackDuckArtifactoryProperty.BLACKDUCK_FORGE.getName(), forge);
        setMultimap.put(BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID.getName(), originId);

        return artifactoryPropertyService.getAllItemsInRepoWithPropertiesAndValues(setMultimap, repoKeys);
    }
}
