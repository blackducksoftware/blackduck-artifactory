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
import com.synopsys.integration.blackduck.artifactory.modules.scaas.ScanAsAServiceBlockingStrategy;
import com.synopsys.integration.blackduck.artifactory.modules.scaas.ScanAsAServiceModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.scaas.ScanAsAServicePropertyService;
import com.synopsys.integration.blackduck.artifactory.modules.scaas.ScanAsAServiceScanStatus;

import static com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionComponentPolicyStatusType.IN_VIOLATION;
import static com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionComponentPolicyStatusType.NOT_IN_VIOLATION;
import static com.synopsys.integration.blackduck.artifactory.modules.scaas.ScanAsAServiceBlockingStrategy.BLOCK_ALL;
import static com.synopsys.integration.blackduck.artifactory.modules.scaas.ScanAsAServiceBlockingStrategy.BLOCK_NONE;
import static com.synopsys.integration.blackduck.artifactory.modules.scaas.ScanAsAServiceScanStatus.FAILED;
import static com.synopsys.integration.blackduck.artifactory.modules.scaas.ScanAsAServiceScanStatus.PROCESSING;
import static com.synopsys.integration.blackduck.artifactory.modules.scaas.ScanAsAServiceScanStatus.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.MockitoAnnotations.initMocks;

public class ScanAsAServiceCancelDeciderTest {
    private static String TEST_REPO_PATH = "test-repo";

    private static String TEST_OUTSIDE_REPO_PATH = "test-outside-repo";

    private static String TEST_ARTIFACT_NAME = "test-artifact.jar";

    private static String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    private static Stream<Arguments> providerValuesForGetCancelDecision() {
        // Description, ArtifactRepoPath, ScanAsAServiceBlockinStrategy, ScanAsAServiceScanStatus, ProjectVersionComponentPolicyStatusType, String(lastUpdated), String(cutoffDate), CancelDecision
        return Stream.of(
                arguments("No Blocking; Scan Success with Policy Violations",
                        artifactPath,
                        BLOCK_NONE,
                        SUCCESS,
                        IN_VIOLATION,
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; Policy Violation Status: %s; repo: %s", SUCCESS.getMessage(), IN_VIOLATION.name(), artifactPath))),
                arguments("No Blocking; Scan Success with No Policy Violations",
                        artifactPath,
                        BLOCK_NONE,
                        SUCCESS,
                        NOT_IN_VIOLATION,
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.NO_CANCELLATION()),
                arguments("No Blocking; Scan in progress",
                        artifactPath,
                        BLOCK_NONE,
                        PROCESSING,
                        null,
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.NO_CANCELLATION()),
                arguments("No Blocking; Scan Failed",
                        artifactPath,
                        BLOCK_NONE,
                        FAILED,
                        null,
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; repo: %s", FAILED.getMessage(), artifactPath))),
                arguments("Blocking; Scan Success with No Policy Violations",
                        artifactPath,
                        BLOCK_ALL,
                        SUCCESS,
                        NOT_IN_VIOLATION,
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.NO_CANCELLATION()),
                arguments("Blocking; Scan Success with Policy Violations",
                        artifactPath,
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION,
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; Policy Violation Status: %s; repo: %s", SUCCESS.getMessage(), IN_VIOLATION.name(), artifactPath))),
                arguments("Blocking; Scan in progress",
                        artifactPath,
                        BLOCK_ALL,
                        PROCESSING,
                        null,
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; repo: %s", PROCESSING.getMessage(), artifactPath))),
                arguments("Blocking; Scan Failed",
                        artifactPath,
                        BLOCK_ALL,
                        FAILED,
                        null,
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; repo: %s", FAILED.getMessage(), artifactPath))),
                arguments("No Blocking; Last updated prior to cutoff",
                        artifactPath,
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION, // The combination of these will cause blocking if the cutoff logic fails
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(2).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.NO_CANCELLATION()),
                arguments("No Blocking; Last updated same as cutoff",
                        artifactPath,
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION, // The combination of these will cause blocking if the cutoff logic fails
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.NO_CANCELLATION()),
                arguments("Blocking; In violation; Provide null cutoff time",
                        artifactPath,
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION,
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(3).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        null,
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; Policy Violation Status: %s; repo: %s", SUCCESS.getMessage(), IN_VIOLATION.name(), artifactPath))),
                arguments("No Blocking; Repo path not in blocking repos",
                        outsideArtifactPath,
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION, // The combination of these will cause blocking if the blocking repos check fails
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
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

    private ScanAsAServiceCancelDecider sut;

    @BeforeAll
    public static void beforeAll() {
        pluginRepoPathFactory = new PluginRepoPathFactory(false);
        RepoPath repoPath = pluginRepoPathFactory.create(TEST_REPO_PATH);
        artifactPath = pluginRepoPathFactory.create(repoPath.getRepoKey(), TEST_ARTIFACT_NAME);
        RepoPath outsideRepoPath = pluginRepoPathFactory.create(TEST_OUTSIDE_REPO_PATH);
        outsideArtifactPath = pluginRepoPathFactory.create(outsideRepoPath.getRepoKey(), TEST_ARTIFACT_NAME);
        dateTimeManager = new DateTimeManager(DATETIME_PATTERN);
    }

    public ScanAsAServiceCancelDeciderTest() {
        initMocks(this);
        sut = new ScanAsAServiceCancelDecider(scanAsAServiceModuleConfig, scanAsAServicePropertyService, artifactoryPAPIService);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("providerValuesForGetCancelDecision")
    public void testGetCancelDecision(String description,
            RepoPath repoPath,
            ScanAsAServiceBlockingStrategy blockingStrategy,
            ScanAsAServiceScanStatus scanStatus,
            ProjectVersionComponentPolicyStatusType policyViolationStatus,
            String lastUpdated,
            String cutoffDate,
            CancelDecision expectedResult) {
        Mockito.when(scanAsAServiceModuleConfig.getBlockingStrategy()).thenReturn(blockingStrategy);
        Mockito.when(scanAsAServiceModuleConfig.getBlockingRepos()).thenReturn(List.of(TEST_REPO_PATH));
        Mockito.when(scanAsAServiceModuleConfig.getDateTimeManager()).thenReturn(dateTimeManager);
        Mockito.when(scanAsAServiceModuleConfig.getCutoffDateString()).thenReturn(Optional.ofNullable(cutoffDate));
        Mockito.when(scanAsAServicePropertyService.getScanStatusProperty(repoPath)).thenReturn(Optional.ofNullable(scanStatus));
        Mockito.when(scanAsAServicePropertyService.getPolicyStatus(repoPath)).thenReturn(Optional.ofNullable(policyViolationStatus));
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
                return null;
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
        CancelDecision actualResult = sut.getCancelDecision(repoPath);
        assertEquals(expectedResult, actualResult);
    }
}
