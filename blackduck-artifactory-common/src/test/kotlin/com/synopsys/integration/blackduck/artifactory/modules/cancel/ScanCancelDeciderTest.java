package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanPropertyService;
import com.synopsys.integration.blackduck.codelocation.Result;

class ScanCancelDeciderTest {

    private static Stream<Arguments> provideConfigForCancelDecision() {
        List<Result> scanResults = new ArrayList<>(Arrays.asList(Result.values()));
        scanResults.add(null);

        return scanResults.stream()
                   .flatMap(result -> Stream.of(
                       Arguments.of(false, false, false, result),
                       Arguments.of(false, false, true, result),
                       Arguments.of(false, true, false, result),
                       Arguments.of(false, true, true, result),
                       Arguments.of(true, false, false, result),
                       Arguments.of(true, false, true, result),
                       Arguments.of(true, true, false, result),
                       Arguments.of(true, true, true, result)
                   ));
    }

    @ParameterizedTest
    @MethodSource("provideConfigForCancelDecision")
    void testDecision(boolean metadataBlockEnabled, boolean isFolder, boolean matchesExtension, @Nullable Result scanResult) {
        CancelDecision cancelDecision = getDecision(metadataBlockEnabled, isFolder, matchesExtension, scanResult);
        if (!metadataBlockEnabled || isFolder || !matchesExtension) {
            assertFalse(cancelDecision.shouldCancelDownload());
            assertTrue(StringUtils.isBlank(cancelDecision.getCancelReason()));
        } else if (scanResult == null || Result.FAILURE.equals(scanResult)) {
            assertTrue(cancelDecision.shouldCancelDownload(), "Scan result missing or FAILURE. Should cancel.");
            assertTrue(StringUtils.isNotBlank(cancelDecision.getCancelReason()));
        } else {
            assertFalse(cancelDecision.shouldCancelDownload(), "Scan Result: " + scanResult.name());
            assertTrue(StringUtils.isBlank(cancelDecision.getCancelReason()));
        }
    }

    private CancelDecision getDecision(boolean metadataBlockEnabled, boolean isFolder, boolean matchesExtension, @Nullable Result scanResult) {
        ScanModuleConfig scanModuleConfig = Mockito.mock(ScanModuleConfig.class);
        ScanPropertyService scanPropertyService = Mockito.mock(ScanPropertyService.class);
        ArtifactoryPAPIService artifactoryPAPIService = Mockito.mock(ArtifactoryPAPIService.class);
        ScanCancelDecider scanCancelDecider = new ScanCancelDecider(scanModuleConfig, scanPropertyService, artifactoryPAPIService);

        PluginRepoPathFactory pluginRepoPathFactory = new PluginRepoPathFactory(false);
        RepoPath testRepoPath = pluginRepoPathFactory.create("test-repo");
        RepoPath artifactRepoPath = pluginRepoPathFactory.create(testRepoPath.getRepoKey(), "artifact-to-be-scanned.zip");
        ItemInfo artifactItemInfo = Mockito.mock(ItemInfo.class);

        Mockito.when(scanModuleConfig.isMetadataBlockEnabled()).thenReturn(metadataBlockEnabled);
        Mockito.when(scanModuleConfig.getRepos()).thenReturn(Collections.singletonList(testRepoPath.getRepoKey()));
        Mockito.when(artifactItemInfo.isFolder()).thenReturn(isFolder);
        Mockito.when(artifactoryPAPIService.getItemInfo(artifactRepoPath)).thenReturn(artifactItemInfo);

        Mockito.when(scanPropertyService.getScanResult(artifactRepoPath)).thenReturn(Optional.ofNullable(scanResult));

        Mockito.when(artifactItemInfo.getName()).thenReturn("artifact-to-be-scanned.zip");
        String artifactExtension = matchesExtension ? ".zip" : ".doesntmatch";
        Mockito.when(scanModuleConfig.getNamePatterns()).thenReturn(Collections.singletonList("*" + artifactExtension));

        return scanCancelDecider.getCancelDecision(artifactRepoPath);
    }
}
