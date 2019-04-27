package com.synopsys.integration.blackduck.artifactory.modules.inspection.external.id;

import java.util.Optional;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.bdio.model.externalid.ExternalId;

public interface ExternalIdExtractor {
    Optional<ExternalId> extractExternalId(final RepoPath repoPath);
}
