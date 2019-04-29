package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid;

import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.ExternalIdProperties;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class BlackDuckPropertiesExternalIdExtractor {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final InspectionPropertyService inspectionPropertyService;
    private final ExternalIdFactory externalIdFactory;

    public BlackDuckPropertiesExternalIdExtractor(final InspectionPropertyService inspectionPropertyService, final ExternalIdFactory externalIdFactory) {
        this.inspectionPropertyService = inspectionPropertyService;
        this.externalIdFactory = externalIdFactory;
    }

    /**
     * Extracts an ExternalId from the blackduck forge and originId properties
     */
    public Optional<ExternalId> extractExternalId(final RepoPath repoPath) {
        final ExternalIdProperties externalIdProperties = inspectionPropertyService.getExternalIdProperties(repoPath);

        ExternalId externalId = null;
        if (externalIdProperties.getForge().isPresent() && externalIdProperties.getOriginId().isPresent()) {
            final Forge forge = Forge.getKnownForges().get(externalIdProperties.getForge().get());
            if (forge == null) {
                logger.debug(String.format("Failed to extract forge from property %s.", BlackDuckArtifactoryProperty.BLACKDUCK_FORGE.getName()));
                return Optional.empty();
            }

            final String originId = externalIdProperties.getOriginId().get();
            final String[] originIdPieces = originId.split(forge.getKbSeparator());

            if (originIdPieces.length == 2) {
                externalId = externalIdFactory.createNameVersionExternalId(forge, originIdPieces[0], originIdPieces[1]);
            } else if (originIdPieces.length == 3 && forge.equals(Forge.MAVEN)) {
                externalId = externalIdFactory.createMavenExternalId(originIdPieces[0], originIdPieces[1], originIdPieces[2]);
            } else {
                logger.debug(String.format("Invalid forge and/or origin id on artifact '%s'", repoPath.getPath()));
            }
        } else {
            logger.debug(String.format("Unable to generate an external id from blackduck properties on artifact '%s'", repoPath.getPath()));
        }

        return Optional.ofNullable(externalId);
    }
}
