package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications;

import com.synopsys.integration.blackduck.artifactory.ArtifactSearchService;

public class NotificationSearch {
    private final ArtifactSearchService artifactSearchService;

    public NotificationSearch(ArtifactSearchService artifactSearchService) {
        this.artifactSearchService = artifactSearchService;
    }

    public void findArtifacts() {

    }
}
