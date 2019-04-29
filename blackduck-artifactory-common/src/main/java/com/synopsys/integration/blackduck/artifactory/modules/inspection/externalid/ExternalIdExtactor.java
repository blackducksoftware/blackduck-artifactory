package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid;

import java.util.Optional;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;

public interface ExternalIdExtactor {
    Optional<ExternalId> extractExternalId(final SupportedPackageType supportedPackageType, final RepoPath repoPath);
}
