/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer;

import java.util.List;
import java.util.Optional;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model.ComposerJsonResult;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model.ComposerVersion;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;

public class ComposerExternalIdExtractor {
    private final ComposerVersionSelector composerVersionSelector;
    private final ComposerJsonService composerJsonService;

    public ComposerExternalIdExtractor(ComposerVersionSelector composerVersionSelector, ComposerJsonService composerJsonService) {
        this.composerVersionSelector = composerVersionSelector;
        this.composerJsonService = composerJsonService;
    }

    public Optional<ExternalId> extractExternalId(SupportedPackageType supportedPackageType, RepoPath repoPath) {
        ComposerJsonResult composerJsonResult = composerJsonService.findJsonFiles(repoPath);

        ExternalId extractedExternalId = null;
        for (RepoPath jsonFileRepoPath : composerJsonResult.getRepoPaths()) {
            List<ComposerVersion> composerVersions = composerJsonService.parseJson(jsonFileRepoPath);
            extractedExternalId = composerVersionSelector.discoverMatchingVersion(supportedPackageType, composerJsonResult.getFileNamePieces().getHash(), composerVersions)
                                      .orElse(null);
            if (extractedExternalId != null) {
                break;
            }
        }

        return Optional.ofNullable(extractedExternalId);
    }
}
