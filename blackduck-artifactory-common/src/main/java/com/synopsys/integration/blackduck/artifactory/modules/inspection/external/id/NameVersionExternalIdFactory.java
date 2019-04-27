package com.synopsys.integration.blackduck.artifactory.modules.inspection.external.id;

import java.util.Optional;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.bdio.model.externalid.ExternalId;

public class NameVersionExternalIdFactory implements ExternalIdExtractor {
    private final ArtifactoryPropertiesExternalIdFactory artifactoryPropertiesExternalIdFactory;
    private final FileLayoutExternalIdFactory fileLayoutExternalIdFactory;

    public NameVersionExternalIdFactory(final ArtifactoryPropertiesExternalIdFactory artifactoryPropertiesExternalIdFactory, final FileLayoutExternalIdFactory fileLayoutExternalIdFactory) {
        this.artifactoryPropertiesExternalIdFactory = artifactoryPropertiesExternalIdFactory;
        this.fileLayoutExternalIdFactory = fileLayoutExternalIdFactory;
    }

    @Override
    public Optional<ExternalId> extractExternalId(final RepoPath repoPath) {
        ExternalId externalId = artifactoryPropertiesExternalIdFactory.extractExternalId(repoPath).orElse(null);

        if (externalId == null) {
            externalId = fileLayoutExternalIdFactory.extractExternalId(repoPath).orElse(null);
        }

        return Optional.ofNullable(externalId);
    }
}
