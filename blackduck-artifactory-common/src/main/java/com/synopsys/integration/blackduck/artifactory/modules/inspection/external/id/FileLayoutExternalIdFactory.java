package com.synopsys.integration.blackduck.artifactory.modules.inspection.external.id;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;

import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;

public class FileLayoutExternalIdFactory extends BaseExternalIdFactory {
    private final SupportedPackageType supportedPackageType;
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ExternalIdFactory externalIdFactory;

    public FileLayoutExternalIdFactory(final SupportedPackageType supportedPackageType, final ArtifactoryPAPIService artifactoryPAPIService, final ExternalIdFactory externalIdFactory) {
        this.supportedPackageType = supportedPackageType;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.externalIdFactory = externalIdFactory;
    }

    @Override
    public Optional<ExternalId> extractExternalId(final RepoPath repoPath) {
        final FileLayoutInfo fileLayoutInfo = artifactoryPAPIService.getLayoutInfo(repoPath);
        final ExternalId externalId;
        if (supportedPackageType.hasNameVersionProperties()) {
            externalId = createNameVersionExternalIdFromFileLayoutInfo(supportedPackageType.getForge(), fileLayoutInfo).orElse(null);
        } else {
            externalId = createMavenExternalId(fileLayoutInfo).orElse(null);
        }

        return Optional.ofNullable(externalId);
    }

    private Optional<ExternalId> createNameVersionExternalIdFromFileLayoutInfo(final Forge forge, final FileLayoutInfo fileLayoutInfo) {
        final String name = fileLayoutInfo.getModule();
        final String version = fileLayoutInfo.getBaseRevision();
        return createNameVersionExternalId(externalIdFactory, forge, name, version);
    }

    private Optional<ExternalId> createMavenExternalId(final FileLayoutInfo fileLayoutInfo) {
        ExternalId externalId = null;
        final String group = fileLayoutInfo.getOrganization();
        final String name = fileLayoutInfo.getModule();
        final String version = fileLayoutInfo.getBaseRevision();
        if (StringUtils.isNoneBlank(group, name, version)) {
            externalId = externalIdFactory.createMavenExternalId(group, name, version);
        }
        return Optional.ofNullable(externalId);
    }
}
