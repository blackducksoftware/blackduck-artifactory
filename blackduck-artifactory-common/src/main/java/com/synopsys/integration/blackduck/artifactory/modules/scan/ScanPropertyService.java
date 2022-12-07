/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scan;

import java.util.Optional;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.blackduck.artifactory.modules.UpdateStatus;
import com.synopsys.integration.blackduck.codelocation.Result;

// TODO: This class should grow like InspectionPropertyService. Scan services shouldn't need to interface with ArtifactoryPropertyService directly.
public class ScanPropertyService extends ArtifactoryPropertyService {
    public ScanPropertyService(ArtifactoryPAPIService artifactoryPAPIService, DateTimeManager dateTimeManager) {
        super(artifactoryPAPIService, dateTimeManager);
    }

    public Optional<Result> getScanResult(RepoPath repoPath) {
        return getProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_RESULT)
                   .map(Result::valueOf);
    }

    public Optional<UpdateStatus> getUpdateStatus(RepoPath repoPath) {
        return getProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS)
                   .map(UpdateStatus::valueOf);
    }
}
