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

package cancel;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionComponentPolicyStatusType;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.modules.cancel.CancelDecision;
import com.synopsys.integration.blackduck.artifactory.modules.cancel.ScanAsAServiceCancelDecider;
import com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServiceBlockingStrategy;
import com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServiceModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServicePropertyService;
import com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServiceScanStatus;

import static com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionComponentPolicyStatusType.IN_VIOLATION;
import static com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionComponentPolicyStatusType.NOT_IN_VIOLATION;
import static com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServiceBlockingStrategy.BLOCK_ALL;
import static com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServiceBlockingStrategy.BLOCK_NONE;
import static com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServiceBlockingStrategy.BLOCK_OFF;
import static com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServiceScanStatus.FAILED;
import static com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServiceScanStatus.PROCESSING;
import static com.synopsys.integration.blackduck.artifactory.modules.scaaas.ScanAsAServiceScanStatus.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.MockitoAnnotations.initMocks;

public class ScanAsAServiceCancelDeciderTest {
    private static String TEST_REPO_PATH = "test-repo";

    private static String TEST_OUTSIDE_REPO_PATH = "test-outside-repo";

    private static String TEST_ARTIFACT_NAME = "test-artifact.jar";

    private static String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    private static Stream<Arguments> providerValuesForBlockNoneGetCancelDecisionTests() {
        // Description, ArtifactRepoPath, ScanAsAServiceBlockinStrategy, ScanAsAServiceScanStatus, ProjectVersionComponentPolicyStatusType, CancelDecision
        return Stream.of(
                arguments("Blocking; Blocking Strategy BLOCK_NONE; Scan Success with Policy Violations",
                        artifactPath,
                        BLOCK_NONE,
                        SUCCESS,
                        IN_VIOLATION,
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; Policy Violation Status: %s; repo: %s", SUCCESS.getMessage(), IN_VIOLATION.name(), artifactPath))),
                arguments("No Blocking; Blocking Strategy BLOCK_NONE; Scan Success with No Policy Violations",
                        artifactPath,
                        BLOCK_NONE,
                        SUCCESS,
                        NOT_IN_VIOLATION,
                        CancelDecision.NO_CANCELLATION()),
                arguments("No Blocking; Blocking Strategy BLOCK_NONE; Scan in progress",
                        artifactPath,
                        BLOCK_NONE,
                        PROCESSING,
                        null,
                        CancelDecision.NO_CANCELLATION()),
                arguments("No Blocking; Blocking Strategy BLOCK_NONE; Scan Failed",
                        artifactPath,
                        BLOCK_NONE,
                        FAILED,
                        null,
                        CancelDecision.NO_CANCELLATION())
        );
    }

    private static Stream<Arguments> providerValuesForBlockAllGetCancelDecisionTests() {
        // Description, ArtifactRepoPath, ScanAsAServiceBlockinStrategy, ScanAsAServiceScanStatus, ProjectVersionComponentPolicyStatusType, CancelDecision
        return Stream.of(
                arguments("Blocking; Blocking Strategy BLOCK_ALL; Scan Success with No Policy Violations",
                        artifactPath,
                        BLOCK_ALL,
                        SUCCESS,
                        NOT_IN_VIOLATION,
                        CancelDecision.NO_CANCELLATION()),
                arguments("Blocking; Blocking Strategy BLOCK_ALL; Scan Success with Policy Violations",
                        artifactPath,
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION,
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; Policy Violation Status: %s; repo: %s", SUCCESS.getMessage(), IN_VIOLATION.name(), artifactPath))),
                arguments("Blocking; Blocking Strategy BLOCK_ALL; Scan in progress",
                        artifactPath,
                        BLOCK_ALL,
                        PROCESSING,
                        null,
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; repo: %s", PROCESSING.getMessage(), artifactPath))),
                arguments("Blocking; Blocking Strategy BLOCK_ALL; Scan Failed",
                        artifactPath,
                        BLOCK_ALL,
                        FAILED,
                        null,
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; repo: %s", FAILED.getMessage(), artifactPath)))
        );
    }

    private static Stream<Arguments> providerValuesForBlockOffGetCancelDecisionTests() {
        // Description, ArtifactRepoPath, ScanAsAServiceBlockinStrategy, ScanAsAServiceScanStatus, ProjectVersionComponentPolicyStatusType, CancelDecision
        return Stream.of(
                arguments("No Blocking; Blocking Strategy BLOCK_OFF; Scan in progress",
                        artifactPath,
                        BLOCK_OFF,
                        PROCESSING,
                        null,
                        CancelDecision.NO_CANCELLATION()),
                arguments("No Blocking; Blocking Strategy BLOCK_OFF; No Policy violations",
                        artifactPath,
                        BLOCK_OFF,
                        SUCCESS,
                        NOT_IN_VIOLATION,
                        CancelDecision.NO_CANCELLATION()),
                arguments("No Blocking; Blocking Strategy BLOCK_OFF; Policy violations",
                        artifactPath,
                        BLOCK_OFF,
                        SUCCESS,
                        IN_VIOLATION,
                        CancelDecision.NO_CANCELLATION()),
                arguments("No Blocking; Blocking Strategy BLOCK_OFF; scan failed",
                        artifactPath,
                        BLOCK_OFF,
                        FAILED,
                        null,
                        CancelDecision.NO_CANCELLATION())        );
    }

    private static Stream<Arguments> providerValuesForCutoffGetCancelDecisionTests() {
        // Description, ArtifactRepoPath, String(lastUpdated), String(cutoffDate), CancelDecision
        return Stream.of(
                arguments("No Blocking; Last updated prior to cutoff",
                        artifactPath,
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(2).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.NO_CANCELLATION()),
                arguments("No Blocking; Last updated same as cutoff",
                        artifactPath,
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.NO_CANCELLATION()),
                arguments("Blocking; In violation; Provide null cutoff time",
                        artifactPath,
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(3).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        null,
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; Policy Violation Status: %s; repo: %s", SUCCESS.getMessage(), IN_VIOLATION.name(), artifactPath)))
        );
    }

    private static Stream<Arguments> providerValuesForNoScanStatusGetCancelDecisionTests() {
        // Description, ArtifactRepoPath, ScanAsAServiceBlockinStrategy, ScanAsAServiceScanStatus, ProjectVersionComponentPolicyStatusType, CancelDecision
        return Stream.of(
                arguments("No Blocking; Blocking Strategy BLOCK_NONE; No scan status",
                        artifactPath,
                        BLOCK_NONE,
                        null,
                        null,
                        CancelDecision.NO_CANCELLATION()),
                arguments("Blocking; Blocking Strategy BLOCK_ALL; No scan status",
                        artifactPath,
                        BLOCK_ALL,
                        null,
                        null,
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; Scan not scheduled and blocking strategy: %s; repo: %s", BLOCK_ALL, artifactPath))),
                arguments("No Blocking; Blocking Strategy BLOCK_OFF; No scan status",
                        artifactPath,
                        BLOCK_OFF,
                        null,
                        null,
                        CancelDecision.NO_CANCELLATION())
        );
    }

    private static Stream<Arguments> providerValuesForRepoPathAndFilePatternGetCancelDecisionTests() {
        // Description, ArtifactRepoPath, ScanAsAServiceBlockinStrategy, ScanAsAServiceScanStatus, ProjectVersionComponentPolicyStatusType, List<String>(allowedFilePatterns), List<String>(excludedFilePatterns), CancelDecision
        return Stream.of(
                arguments("No Blocking; Repo path not in blocking repos",
                        outsideArtifactPath,
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION, // The combination of these will cause blocking if the blocking repos check fails
                        null,
                        null,
                        CancelDecision.NO_CANCELLATION()),
                arguments("Blocking; Allowed File Pattern not specified; Exclude File Pattern not specified",
                        artifactPath,
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION,
                        null,
                        null,
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; Policy Violation Status: %s; repo: %s", SUCCESS.getMessage(), IN_VIOLATION.name(), artifactPath))),
                arguments("No Blocking; Allowed File Pattern not specified; Exclude File Pattern matched",
                        artifactPath,
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION, // The combination of these will cause blocking if the allowed file pattern check fails
                        null,
                        List.of("*fact.jar"),
                        CancelDecision.NO_CANCELLATION()),
                arguments("Blocking; Allowed File Pattern not specified; Exclude File Pattern not matched",
                        artifactPath,
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION,
                        null,
                        List.of("*fact.war"),
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; Policy Violation Status: %s; repo: %s", SUCCESS.getMessage(), IN_VIOLATION.name(), artifactPath))),
                arguments("Blocking; Allowed File Pattern matched; Exclude File Pattern not specified",
                        artifactPath,
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION,
                        List.of("*.war", "*.jar"),
                        null,
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; Policy Violation Status: %s; repo: %s", SUCCESS.getMessage(), IN_VIOLATION.name(), artifactPath))),
                arguments("No Blocking; Allowed File Pattern matched; Exclude File Pattern matched",
                        artifactPath,
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION, // The combination of these will cause blocking if the allowed file pattern check fails
                        List.of("*.war", "*.jar"),
                        List.of("*fact.jar"),
                        CancelDecision.NO_CANCELLATION()),
                arguments("Blocking; Allowed File Pattern matched; Exclude File Pattern not matched",
                        artifactPath,
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION,
                        List.of("*.war", "*.jar"),
                        List.of("*fact.war"),
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; Policy Violation Status: %s; repo: %s", SUCCESS.getMessage(), IN_VIOLATION.name(), artifactPath))),
                arguments("No Blocking; Allowed File Pattern not matched",
                        artifactPath,
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION, // The combination of these will cause blocking if the allowed file pattern check fails
                        List.of("*.war", "*.tgz"),
                        null,
                        CancelDecision.NO_CANCELLATION())
        );
    }

    @Mock
    private ArtifactoryPAPIService artifactoryPAPIService;

    @Mock
    private ScanAsAServiceModuleConfig scanAsAServiceModuleConfig;

    @Mock
    private ScanAsAServicePropertyService scanAsAServicePropertyService;

    private static PluginRepoPathFactory pluginRepoPathFactory;

    private static RepoPath artifactPath;

    private static RepoPath outsideArtifactPath;

    private static DateTimeManager dateTimeManager;

    private static Pair<String, String> lastUpdatedLaterThanCutoffTime;

    private ScanAsAServiceCancelDecider sut;

    @BeforeAll
    public static void beforeAll() {
        pluginRepoPathFactory = new PluginRepoPathFactory(false);
        RepoPath repoPath = pluginRepoPathFactory.create(TEST_REPO_PATH);
        artifactPath = pluginRepoPathFactory.create(repoPath.getRepoKey(), TEST_ARTIFACT_NAME);
        RepoPath outsideRepoPath = pluginRepoPathFactory.create(TEST_OUTSIDE_REPO_PATH);
        outsideArtifactPath = pluginRepoPathFactory.create(outsideRepoPath.getRepoKey(), TEST_ARTIFACT_NAME);
        dateTimeManager = new DateTimeManager(DATETIME_PATTERN);
        lastUpdatedLaterThanCutoffTime = Pair.of(
                Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN))
        );
    }

    public ScanAsAServiceCancelDeciderTest() {
        initMocks(this);
        sut = new ScanAsAServiceCancelDecider(scanAsAServiceModuleConfig, scanAsAServicePropertyService, artifactoryPAPIService);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("providerValuesForBlockNoneGetCancelDecisionTests")
    public void testGetCancelDecisionWithBlockNoneStrategy(String description,
            RepoPath repoPath,
            ScanAsAServiceBlockingStrategy blockingStrategy,
            ScanAsAServiceScanStatus scanStatus,
            ProjectVersionComponentPolicyStatusType policyViolationStatus,
            CancelDecision expectedResult) {
        CancelDecision actualResult = getCancelDecisionRunner(repoPath,
                blockingStrategy,
                scanStatus,
                policyViolationStatus,
                lastUpdatedLaterThanCutoffTime.getLeft(),
                lastUpdatedLaterThanCutoffTime.getRight(),
                null,
                null);
        assertEquals(expectedResult, actualResult);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("providerValuesForBlockAllGetCancelDecisionTests")
    public void testGetCancelDecisionWithBlockAllStrategy(String description,
            RepoPath repoPath,
            ScanAsAServiceBlockingStrategy blockingStrategy,
            ScanAsAServiceScanStatus scanStatus,
            ProjectVersionComponentPolicyStatusType policyViolationStatus,
            CancelDecision expectedResult) {
        CancelDecision actualResult = getCancelDecisionRunner(repoPath,
                blockingStrategy,
                scanStatus,
                policyViolationStatus,
                lastUpdatedLaterThanCutoffTime.getLeft(),
                lastUpdatedLaterThanCutoffTime.getRight(),
                null,
                null);
        assertEquals(expectedResult, actualResult);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("providerValuesForBlockOffGetCancelDecisionTests")
    public void testGetCancelDecisionWithBlockingOffStrategy(String description,
            RepoPath repoPath,
            ScanAsAServiceBlockingStrategy blockingStrategy,
            ScanAsAServiceScanStatus scanStatus,
            ProjectVersionComponentPolicyStatusType policyViolationStatus,
            CancelDecision expectedResult) {
        CancelDecision actualResult = getCancelDecisionRunner(repoPath,
                blockingStrategy,
                scanStatus,
                policyViolationStatus,
                lastUpdatedLaterThanCutoffTime.getLeft(),
                lastUpdatedLaterThanCutoffTime.getRight(),
                null,
                null);
        assertEquals(expectedResult, actualResult);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("providerValuesForNoScanStatusGetCancelDecisionTests")
    public void testGetCancelDecisionWithNoScanStatus(String description,
            RepoPath repoPath,
            ScanAsAServiceBlockingStrategy blockingStrategy,
            ScanAsAServiceScanStatus scanStatus,
            ProjectVersionComponentPolicyStatusType policyViolationStatus,
            CancelDecision expectedResult) {
        CancelDecision actualResult = getCancelDecisionRunner(repoPath,
                blockingStrategy,
                scanStatus,
                policyViolationStatus,
                lastUpdatedLaterThanCutoffTime.getLeft(),
                lastUpdatedLaterThanCutoffTime.getRight(),
                null,
                null);
        assertEquals(expectedResult, actualResult);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("providerValuesForCutoffGetCancelDecisionTests")
    public void testGetCancelDecisionWithCutoff(String description,
            RepoPath repoPath,
            String lastUpdatedTime,
            String cutoffTime,
            CancelDecision expectedResult) {
        CancelDecision actualResult = getCancelDecisionRunner(repoPath,
                BLOCK_ALL,
                SUCCESS,
                IN_VIOLATION, // The combination of these will cause blocking if the cutoff logic fails
                lastUpdatedTime,
                cutoffTime,
                null,
                null);
        assertEquals(expectedResult, actualResult);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("providerValuesForRepoPathAndFilePatternGetCancelDecisionTests")
    public void testGetCancelDecisionWithRepoPathAndFilePattern(String description,
            RepoPath repoPath,
            ScanAsAServiceBlockingStrategy blockingStrategy,
            ScanAsAServiceScanStatus scanStatus,
            ProjectVersionComponentPolicyStatusType policyViolationStatus,
            List<String> allowedFilePatterns,
            List<String> excludedFilePatterns,
            CancelDecision expectedResult) {
        CancelDecision actualResult = getCancelDecisionRunner(repoPath,
                blockingStrategy,
                scanStatus,
                policyViolationStatus,
                lastUpdatedLaterThanCutoffTime.getLeft(),
                lastUpdatedLaterThanCutoffTime.getRight(),
                allowedFilePatterns,
                excludedFilePatterns);
        assertEquals(expectedResult, actualResult);
    }

    private CancelDecision getCancelDecisionRunner(RepoPath repoPath,
            ScanAsAServiceBlockingStrategy blockingStrategy,
            ScanAsAServiceScanStatus scanStatus,
            ProjectVersionComponentPolicyStatusType policyViolationStatus,
            String lastUpdated,
            String cutoffDate,
            List<String> allowedFilePatterns,
            List<String> excludedFilePatterns) {
        Mockito.when(scanAsAServiceModuleConfig.getBlockingStrategy()).thenReturn(blockingStrategy);
        Mockito.when(scanAsAServiceModuleConfig.getBlockingRepos()).thenReturn(List.of(TEST_REPO_PATH));
        Mockito.when(scanAsAServiceModuleConfig.getDateTimeManager()).thenReturn(dateTimeManager);
        Mockito.when(scanAsAServiceModuleConfig.getCutoffDateString()).thenReturn(Optional.ofNullable(cutoffDate));
        Mockito.when(scanAsAServicePropertyService.getScanStatusProperty(repoPath)).thenReturn(Optional.ofNullable(scanStatus));
        Mockito.when(scanAsAServicePropertyService.getPolicyStatus(repoPath)).thenReturn(Optional.ofNullable(policyViolationStatus));
        Mockito.when(scanAsAServiceModuleConfig.getAllowedFileNamePatterns()).thenReturn(Optional.ofNullable(allowedFilePatterns));
        Mockito.when(scanAsAServiceModuleConfig.getExcludedFileNamePatterns()).thenReturn(Optional.ofNullable(excludedFilePatterns));
        Mockito.when(artifactoryPAPIService.getItemInfo(repoPath)).thenReturn(new ItemInfo() {
            @Override public long getId() {
                return 0;
            }

            @Override public RepoPath getRepoPath() {
                return repoPath;
            }

            @Override public boolean isFolder() {
                return false;
            }

            @Override public String getName() {
                return repoPath.getName();
            }

            @Override public String getRepoKey() {
                return repoPath.getRepoKey();
            }

            @Override public String getRelPath() {
                return null;
            }

            @Override public long getCreated() {
                return 0;
            }

            @Override public long getLastModified() {
                return 0;
            }

            @Override public String getModifiedBy() {
                return null;
            }

            @Override public String getCreatedBy() {
                return null;
            }

            @Override public long getLastUpdated() {
                return dateTimeManager.getTimeFromString(lastUpdated);
            }

            @Override public boolean isIdentical(ItemInfo info) {
                return false;
            }

            @Override public int compareTo(@NotNull ItemInfo o) {
                return 0;
            }
        });
        return sut.getCancelDecision(repoPath);
    }
}
