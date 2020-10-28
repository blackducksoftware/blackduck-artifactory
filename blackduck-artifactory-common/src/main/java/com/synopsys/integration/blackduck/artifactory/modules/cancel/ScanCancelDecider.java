package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import static java.lang.Boolean.FALSE;

import java.io.File;

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

        boolean successfulScanResult = scanPropertyService.getScanResult(repoPath)
                                           .filter(Result.SUCCESS::equals)
                                           .isPresent();
        if (successfulScanResult) {
            return CancelDecision.NO_CANCELLATION();
        }

        File artifact = new File(itemInfo.getName());

        for (String namePattern : scanModuleConfig.getNamePatterns()) {
            WildcardFileFilter wildcardFileFilter = new WildcardFileFilter(namePattern);
            if (wildcardFileFilter.accept(artifact)) {
                return CancelDecision.CANCEL_DOWNLOAD(String.format("Missing %s scan result on an artifact that should be scanned: %s", Result.SUCCESS, repoPath.toPath()));
            }
        }

        return CancelDecision.NO_CANCELLATION();
    }
}
