package com.synopsys.integration.blackduck.artifactory.modules.inspection;

import java.util.Optional;

import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;

import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.IdentifiedArtifact;

public class ArtifactIdentificationService2 {
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ArtifactoryExternalIdFactory artifactoryExternalIdFactory;

    public ArtifactIdentificationService2(final ArtifactoryPAPIService artifactoryPAPIService, final ArtifactoryExternalIdFactory artifactoryExternalIdFactory) {
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.artifactoryExternalIdFactory = artifactoryExternalIdFactory;
    }

    public Optional<IdentifiedArtifact> identifyArtifact(final RepoPath repoPath, final String packageType) {
        final FileLayoutInfo fileLayoutInfo = artifactoryPAPIService.getLayoutInfo(repoPath);
        final org.artifactory.md.Properties properties = artifactoryPAPIService.getProperties(repoPath);
        final Optional<ExternalId> possibleExternalId = artifactoryExternalIdFactory.createExternalId(packageType, fileLayoutInfo, repoPath, properties);

        return possibleExternalId.map(externalId -> new IdentifiedArtifact(repoPath, externalId));
    }
}
