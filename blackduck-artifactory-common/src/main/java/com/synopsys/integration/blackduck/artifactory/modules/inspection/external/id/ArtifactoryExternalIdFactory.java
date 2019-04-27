package com.synopsys.integration.blackduck.artifactory.modules.inspection.external.id;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.external.id.composer.ComposerExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ArtifactoryExternalIdFactory implements ExternalIdExtractor {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));
    private final Map<String, ExternalIdExtractor> externalIdExtractorMap = new HashMap<>();

    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ExternalIdFactory externalIdFactory;
    private final InspectionPropertyService inspectionPropertyService;
    private final BlackDuckPropertiesExternalIdFactory blackDuckPropertiesExternalIdFactory;
    private final ComposerExternalIdFactory composerExternalIdFactory;

    public ArtifactoryExternalIdFactory(final ArtifactoryPAPIService artifactoryPAPIService, final ExternalIdFactory externalIdFactory, final InspectionPropertyService inspectionPropertyService,
        final ComposerExternalIdFactory composerExternalIdFactory) {
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.externalIdFactory = externalIdFactory;
        this.inspectionPropertyService = inspectionPropertyService;
        this.composerExternalIdFactory = composerExternalIdFactory;
        this.blackDuckPropertiesExternalIdFactory = new BlackDuckPropertiesExternalIdFactory(inspectionPropertyService, externalIdFactory);
    }

    @Override
    public Optional<ExternalId> extractExternalId(final RepoPath repoPath) {
        final String repoKey = repoPath.getRepoKey();
        final String packageType = artifactoryPAPIService.getPackageType(repoKey).orElse(null);
        final Optional<SupportedPackageType> supportedPackageType = SupportedPackageType.getAsSupportedPackageType(packageType);

        ExternalId externalId = null;
        if (inspectionPropertyService.hasExternalIdProperties(repoPath)) {
            externalId = blackDuckPropertiesExternalIdFactory.extractExternalId(repoPath).orElse(null);
        }

        if (externalId == null && supportedPackageType.isPresent()) {
            if (supportedPackageType.get().equals(SupportedPackageType.COMPOSER)) {
                externalId = composerExternalIdFactory.extractExternalId(repoPath).orElse(null);
            } else {
                final ExternalIdExtractor externalIdExtractor = getExternalIdExtractor(supportedPackageType.get());
                externalId = externalIdExtractor.extractExternalId(repoPath).orElse(null);
            }
        } else if (!supportedPackageType.isPresent()) {
            logger.warn(String.format("Package type (%s) not supported", packageType));
        }

        return Optional.ofNullable(externalId);
    }

    private ExternalIdExtractor getExternalIdExtractor(final SupportedPackageType supportedPackageType) {
        final String key = supportedPackageType.getArtifactoryName();

        if (!externalIdExtractorMap.containsKey(key)) {
            if (supportedPackageType.hasNameVersionProperties()) {
                final ArtifactoryPropertiesExternalIdFactory artifactoryPropertiesExternalIdFactory = new ArtifactoryPropertiesExternalIdFactory(supportedPackageType, artifactoryPAPIService, externalIdFactory);
                final FileLayoutExternalIdFactory fileLayoutExternalIdFactory = new FileLayoutExternalIdFactory(supportedPackageType, artifactoryPAPIService, externalIdFactory);
                final NameVersionExternalIdFactory nameVersionExternalIdFactory = new NameVersionExternalIdFactory(artifactoryPropertiesExternalIdFactory, fileLayoutExternalIdFactory);
                externalIdExtractorMap.put(key, nameVersionExternalIdFactory);
            } else {
                final FileLayoutExternalIdFactory fileLayoutExternalIdFactory = new FileLayoutExternalIdFactory(supportedPackageType, artifactoryPAPIService, externalIdFactory);
                externalIdExtractorMap.put(key, fileLayoutExternalIdFactory);
            }
        }

        return externalIdExtractorMap.get(key);
    }
}
