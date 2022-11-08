/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scaas;

import java.util.Optional;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionComponentPolicyStatusType;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;

public class ScanAsAServicePropertyService extends ArtifactoryPropertyService {
    public ScanAsAServicePropertyService(ArtifactoryPAPIService artifactoryPAPIService,
            DateTimeManager dateTimeManager) {
        super(artifactoryPAPIService, dateTimeManager);
    }

    public Optional<ScanAsAServiceScanStatus> getScanStatusProperty(RepoPath repoPath) {
        return getProperty(repoPath, BlackDuckArtifactoryProperty.SCAAAS_SCAN_STATUS)
                .map(ScanAsAServiceScanStatus::getValue);
    }

    public Optional<ProjectVersionComponentPolicyStatusType> getPolicyStatus(RepoPath repoPath) {
        return getProperty(repoPath, BlackDuckArtifactoryProperty.SCAAAS_POLICY_STATUS)
                .map(ProjectVersionComponentPolicyStatusType::valueOf);
    }
}
