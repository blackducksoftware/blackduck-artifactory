package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.artifactory.repo.RepoPath;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.ArtifactInspectionService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;

class InspectionCancelDeciderTest {
    private final static RepoPath REPO_PATH = new PluginRepoPathFactory(false).create("repo", "inspectedArtifact.jar");

    private static Stream<Arguments> provideConfigForCancelDecision() {
        return Stream.of(
            // Should not cancel download
            Arguments.of(false, false, false, InspectionStatus.SUCCESS), // Blocking disabled and should not inspect artifact
            Arguments.of(false, false, true, InspectionStatus.SUCCESS), // Blocking disabled, but should inspect artifact
            Arguments.of(false, true, false, InspectionStatus.SUCCESS), // Blocking enabled, but should not inspect artifact
            Arguments.of(false, true, true, InspectionStatus.SUCCESS), // Would block if needed, but inspection status is SUCCESS

            // Should cancel download
            Arguments.of(true, true, true, InspectionStatus.FAILURE), // Artifact failed inspection
            Arguments.of(true, true, true, InspectionStatus.PENDING), // Artifact inspection incomplete
            Arguments.of(true, true, true, null) // Artifact has yet to be inspected
        );
    }

    @ParameterizedTest
    @MethodSource("provideConfigForCancelDecision")
    public void testDecision(boolean shouldCancel, boolean metadataBlockEnabled, boolean shouldInspectArtifact, @Nullable InspectionStatus inspectionStatus) {
        InspectionPropertyService inspectionPropertyService = Mockito.mock(InspectionPropertyService.class);
        ArtifactInspectionService artifactInspectionService = Mockito.mock(ArtifactInspectionService.class);
        Mockito.when(inspectionPropertyService.getInspectionStatus(REPO_PATH)).thenReturn(Optional.ofNullable(inspectionStatus));
        Mockito.when(artifactInspectionService.shouldInspectArtifact(REPO_PATH)).thenReturn(shouldInspectArtifact);
        List<String> metadataBlockingRepos = Collections.singletonList(REPO_PATH.getRepoKey());

        CancelDecider cancelDecider = new InspectionCancelDecider(metadataBlockEnabled, metadataBlockingRepos, inspectionPropertyService, artifactInspectionService);
        CancelDeciderTestUtil.assertCancellationDecision(shouldCancel, REPO_PATH, cancelDecider);
    }
}
