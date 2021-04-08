/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid;

import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.ComposerExternalIdExtractor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.conda.CondaExternalIdExtractor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ExternalIdService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ArtifactoryInfoExternalIdExtractor artifactoryInfoExternalIdExtractor;
    private final ComposerExternalIdExtractor composerExternalIdFactory;
    private final CondaExternalIdExtractor condaExternalIdExtractor;

    public ExternalIdService(ArtifactoryPAPIService artifactoryPAPIService, ArtifactoryInfoExternalIdExtractor artifactoryInfoExternalIdExtractor, ComposerExternalIdExtractor composerExternalIdFactory,
        CondaExternalIdExtractor condaExternalIdExtractor) {
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.artifactoryInfoExternalIdExtractor = artifactoryInfoExternalIdExtractor;
        this.composerExternalIdFactory = composerExternalIdFactory;
        this.condaExternalIdExtractor = condaExternalIdExtractor;
    }

    public Optional<ExternalId> extractExternalId(RepoPath repoPath) {
        String repoKey = repoPath.getRepoKey();
        String packageType = artifactoryPAPIService.getPackageType(repoKey).orElse(null);
        Optional<SupportedPackageType> supportedPackageTypeOptional = SupportedPackageType.getAsSupportedPackageType(packageType);

        ExternalId externalId = null;
        if (supportedPackageTypeOptional.isPresent()) {
            SupportedPackageType supportedPackageType = supportedPackageTypeOptional.get();

            if (supportedPackageType.equals(SupportedPackageType.COMPOSER)) {
                externalId = composerExternalIdFactory.extractExternalId(supportedPackageType, repoPath).orElse(null);
            } else if (supportedPackageType.equals(SupportedPackageType.CONDA)) {
                externalId = condaExternalIdExtractor.extractExternalId(supportedPackageType, repoPath).orElse(null);
            }

            if (externalId == null) {
                externalId = artifactoryInfoExternalIdExtractor.extractExternalId(supportedPackageType, repoPath).orElse(null);
            }
        } else {
            logger.warn(String.format("Package type (%s) not supported", packageType));
        }

        return Optional.ofNullable(externalId);
    }
}
