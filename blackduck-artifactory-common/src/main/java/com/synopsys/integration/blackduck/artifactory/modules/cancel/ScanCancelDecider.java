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
package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import static java.lang.Boolean.FALSE;

import java.io.File;
import java.util.Optional;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanPropertyService;
import com.synopsys.integration.blackduck.codelocation.Result;

public class ScanCancelDecider extends CancelDecider {
    private final ScanModuleConfig scanModuleConfig;
    private final ScanPropertyService scanPropertyService;
    private final ArtifactoryPAPIService artifactoryPAPIService;

    public ScanCancelDecider(ScanModuleConfig scanModuleConfig, ScanPropertyService scanPropertyService, ArtifactoryPAPIService artifactoryPAPIService) {
        this.scanModuleConfig = scanModuleConfig;
        this.scanPropertyService = scanPropertyService;
        this.artifactoryPAPIService = artifactoryPAPIService;
    }

    @Override
    public CancelDecision getCancelDecision(RepoPath repoPath) {
        boolean metadataBlockDisabled = FALSE.equals(scanModuleConfig.isMetadataBlockEnabled());
        boolean shouldScanRepository = !scanModuleConfig.getRepos().contains(repoPath.getRepoKey());
        ItemInfo itemInfo = artifactoryPAPIService.getItemInfo(repoPath);
        if (metadataBlockDisabled || shouldScanRepository || itemInfo.isFolder()) {
            return CancelDecision.NO_CANCELLATION();
        }

        Optional<Result> scanResult = scanPropertyService.getScanResult(repoPath);
        if (scanResult.isPresent() && Result.FAILURE.equals(scanResult.get())) {
            return CancelDecision.CANCEL_DOWNLOAD(String.format("The artifact was not successfully scanned. Found result %s.", Result.FAILURE));
        } else if (scanResult.isPresent() && Result.SUCCESS.equals(scanResult.get())) {
            return CancelDecision.NO_CANCELLATION();
        }

        File artifact = new File(itemInfo.getName());

        for (String namePattern : scanModuleConfig.getNamePatterns()) {
            WildcardFileFilter wildcardFileFilter = new WildcardFileFilter(namePattern);
            if (wildcardFileFilter.accept(artifact)) {
                return CancelDecision.CANCEL_DOWNLOAD(String.format("Missing the %s scan result on an artifact that should be scanned.", Result.SUCCESS));
            }
        }

        return CancelDecision.NO_CANCELLATION();
    }
}
