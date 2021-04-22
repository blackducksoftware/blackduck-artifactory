/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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

public class ScanCancelDecider implements CancelDecider {
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
        boolean shouldNotScanRepository = !scanModuleConfig.getRepos().contains(repoPath.getRepoKey()); // TODO: This should be a call to another service
        if (metadataBlockDisabled || shouldNotScanRepository) {
            return CancelDecision.NO_CANCELLATION();
        }

        // Getting ItemInfo from a virtual repository causes an exception resulting in failed downloads. We must verify the repository before doing this check. (IARTH-434)
        ItemInfo itemInfo = artifactoryPAPIService.getItemInfo(repoPath);
        if (itemInfo.isFolder()) {
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
