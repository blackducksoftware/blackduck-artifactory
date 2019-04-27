package com.synopsys.integration.blackduck.artifactory.modules.inspection.external.id;

import java.util.Optional;

import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;

import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;

public class ArtifactoryPropertiesExternalIdFactory extends BaseExternalIdFactory {
    private final SupportedPackageType supportedPackageType;
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ExternalIdFactory externalIdFactory;

    public ArtifactoryPropertiesExternalIdFactory(final SupportedPackageType supportedPackageType, final ArtifactoryPAPIService artifactoryPAPIService, final ExternalIdFactory externalIdFactory) {
        this.supportedPackageType = supportedPackageType;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.externalIdFactory = externalIdFactory;
    }

    @Override
    public Optional<ExternalId> extractExternalId(final RepoPath repoPath) {
        final Properties properties = artifactoryPAPIService.getProperties(repoPath);
        final Forge forge = supportedPackageType.getForge();
        final String namePropertyKey = supportedPackageType.getArtifactoryNameProperty();
        final String versionPropertyKey = supportedPackageType.getArtifactoryVersionProperty();
        final String name = properties.getFirst(namePropertyKey);
        final String version = properties.getFirst(versionPropertyKey);
        return createNameVersionExternalId(externalIdFactory, forge, name, version);
    }
}
