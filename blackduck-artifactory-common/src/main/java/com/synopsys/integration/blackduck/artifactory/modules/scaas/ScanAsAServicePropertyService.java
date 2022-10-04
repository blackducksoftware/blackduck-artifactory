/*
 * Copyright (C) 2022 Synopsys Inc.
 * http://www.synopsys.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Synopsys ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Synopsys.
 */

package com.synopsys.integration.blackduck.artifactory.modules.scaas;

import java.util.Optional;

import org.artifactory.repo.RepoPath;

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
        return getProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_STATUS)
                .map(ScanAsAServiceScanStatus::getValue);
    }
}
