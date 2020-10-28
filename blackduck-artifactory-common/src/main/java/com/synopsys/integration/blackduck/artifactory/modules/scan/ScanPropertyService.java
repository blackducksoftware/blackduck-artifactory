/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2020 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
