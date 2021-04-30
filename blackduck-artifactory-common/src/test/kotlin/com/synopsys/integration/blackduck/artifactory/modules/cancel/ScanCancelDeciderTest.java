package com.synopsys.integration.blackduck.artifactory.modules.cancel;

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
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanPropertyService;
import com.synopsys.integration.blackduck.codelocation.Result;

class ScanCancelDeciderTest {
    private static final PluginRepoPathFactory PLUGIN_REPO_PATH_FACTORY = new PluginRepoPathFactory(false);
    private static final RepoPath TEST_REPO_PATH = PLUGIN_REPO_PATH_FACTORY.create("test-repo");
    private static final RepoPath ARTIFACT_REPO_PATH = PLUGIN_REPO_PATH_FACTORY.create(TEST_REPO_PATH.getRepoKey(), "artifact-to-be-scanned.zip");

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
        CancelDecider cancelDecider = createCancelDecider(metadataBlockEnabled, isFolder, matchesExtension, scanResult);
        if (!metadataBlockEnabled || isFolder || !matchesExtension) {
            CancelDeciderTestUtil.assertCancellationDecision(false, ARTIFACT_REPO_PATH, cancelDecider);
        } else if (scanResult == null || Result.FAILURE.equals(scanResult)) {
            CancelDeciderTestUtil.assertCancellationDecision(true, ARTIFACT_REPO_PATH, cancelDecider, "Scan result missing or FAILURE. Should cancel.");
        } else {
            CancelDeciderTestUtil.assertCancellationDecision(false, ARTIFACT_REPO_PATH, cancelDecider);
        }
    }

    private CancelDecider createCancelDecider(boolean metadataBlockEnabled, boolean isFolder, boolean matchesExtension, @Nullable Result scanResult) {
        ScanModuleConfig scanModuleConfig = Mockito.mock(ScanModuleConfig.class);
        ScanPropertyService scanPropertyService = Mockito.mock(ScanPropertyService.class);
        ArtifactoryPAPIService artifactoryPAPIService = Mockito.mock(ArtifactoryPAPIService.class);
        CancelDecider scanCancelDecider = new ScanCancelDecider(scanModuleConfig, scanPropertyService, artifactoryPAPIService);

        ItemInfo artifactItemInfo = Mockito.mock(ItemInfo.class);

        Mockito.when(scanModuleConfig.isMetadataBlockEnabled()).thenReturn(metadataBlockEnabled);
        Mockito.when(scanModuleConfig.getRepos()).thenReturn(Collections.singletonList(TEST_REPO_PATH.getRepoKey()));
        Mockito.when(artifactItemInfo.isFolder()).thenReturn(isFolder);
        Mockito.when(artifactoryPAPIService.getItemInfo(ARTIFACT_REPO_PATH)).thenReturn(artifactItemInfo);

        Mockito.when(scanPropertyService.getScanResult(ARTIFACT_REPO_PATH)).thenReturn(Optional.ofNullable(scanResult));

        Mockito.when(artifactItemInfo.getName()).thenReturn("artifact-to-be-scanned.zip");
        String artifactExtension = matchesExtension ? ".zip" : ".doesntmatch";
        Mockito.when(scanModuleConfig.getNamePatterns()).thenReturn(Collections.singletonList("*" + artifactExtension));

        return scanCancelDecider;
    }
}
