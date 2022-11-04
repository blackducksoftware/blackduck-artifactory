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
    private static String TEST_ARTIFACT_NAME = "test-artifact.jar";

    private static String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    private static Stream<Arguments> providerValuesForGetCancelDecision() {
        // Description, ScanAsAServiceBlockinStrategy, ScanAsAServiceScanStatus, ProjectVersionComponentPolicyStatusType, String(lastUpdated), String(cutoffDate), CancelDecision
        return Stream.of(
                arguments("No Blocking; Scan Success with Policy Violations",
                        BLOCK_NONE,
                        SUCCESS,
                        IN_VIOLATION,
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; Policy Violation Status: %s; repo: %s", SUCCESS.getMessage(), IN_VIOLATION.name(), artifactPath))),
                arguments("No Blocking; Scan Success with No Policy Violations",
                        BLOCK_NONE,
                        SUCCESS,
                        NOT_IN_VIOLATION,
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.NO_CANCELLATION()),
                arguments("No Blocking; Scan in progress",
                        BLOCK_NONE,
                        PROCESSING,
                        null,
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.NO_CANCELLATION()),
                arguments("No Blocking; Scan Failed",
                        BLOCK_NONE,
                        FAILED,
                        null,
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; repo: %s", FAILED.getMessage(), artifactPath))),
                arguments("Blocking; Scan Success with No Policy Violations",
                        BLOCK_ALL,
                        SUCCESS,
                        NOT_IN_VIOLATION,
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.NO_CANCELLATION()),
                arguments("Blocking; Scan Success with Policy Violations",
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION,
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; Policy Violation Status: %s; repo: %s", SUCCESS.getMessage(), IN_VIOLATION.name(), artifactPath))),
                arguments("Blocking; Scan in progress",
                        BLOCK_ALL,
                        PROCESSING,
                        null,
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; repo: %s", PROCESSING.getMessage(), artifactPath))),
                arguments("Blocking; Scan Failed",
                        BLOCK_ALL,
                        FAILED,
                        null,
                        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; repo: %s", FAILED.getMessage(), artifactPath))),
                arguments("No Blocking; Last updated prior to cutoff",
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION, // The combination of these will cause blocking if the cutoff logic fails
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(2).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.NO_CANCELLATION()),
                arguments("No Blocking; Last updated same as cutoff",
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION, // The combination of these will cause blocking if the cutoff logic fails
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(1).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        CancelDecision.NO_CANCELLATION()),
                arguments("Blocking; In violation; Provide null cutoff time",
                        BLOCK_ALL,
                        SUCCESS,
                        IN_VIOLATION,
                        Instant.now().atOffset(ZoneOffset.UTC).minusYears(3).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)),
                        null,
                        CancelDecision.CANCEL_DOWNLOAD(String.format("Download blocked; %s; Policy Violation Status: %s; repo: %s", SUCCESS.getMessage(), IN_VIOLATION.name(), artifactPath)))
        );
    }

    @Mock
    private ArtifactoryPAPIService artifactoryPAPIService;

    @Mock
    private ScanAsAServiceModuleConfig scanAsAServiceModuleConfig;

    @Mock
    private ScanAsAServicePropertyService scanAsAServicePropertyService;

    private static PluginRepoPathFactory pluginRepoPathFactory;

    private static RepoPath repoPath;

    private static RepoPath artifactPath;

    private static DateTimeManager dateTimeManager;

    private ScanAsAServiceCancelDecider sut;

    @BeforeAll
    public static void beforeAll() {
        pluginRepoPathFactory = new PluginRepoPathFactory(false);
        repoPath = pluginRepoPathFactory.create(TEST_REPO_PATH);
        artifactPath = pluginRepoPathFactory.create(repoPath.getRepoKey(), TEST_ARTIFACT_NAME);
        dateTimeManager = new DateTimeManager(DATETIME_PATTERN);
    }

    public ScanAsAServiceCancelDeciderTest() {
        initMocks(this);
        sut = new ScanAsAServiceCancelDecider(scanAsAServiceModuleConfig, scanAsAServicePropertyService, artifactoryPAPIService);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("providerValuesForGetCancelDecision")
    public void testGetCancelDecision(String description,
            ScanAsAServiceBlockingStrategy blockingStrategy,
            ScanAsAServiceScanStatus scanStatus,
            ProjectVersionComponentPolicyStatusType policyViolationStatus,
            String lastUpdated,
            String cutoffDate,
            CancelDecision expectedResult) {
        Mockito.when(scanAsAServiceModuleConfig.getBlockingStrategy()).thenReturn(blockingStrategy);
        Mockito.when(scanAsAServiceModuleConfig.getDateTimeManager()).thenReturn(dateTimeManager);
        Mockito.when(scanAsAServiceModuleConfig.getCutoffDateString()).thenReturn(Optional.ofNullable(cutoffDate));
        Mockito.when(scanAsAServicePropertyService.getScanStatusProperty(artifactPath)).thenReturn(Optional.ofNullable(scanStatus));
        Mockito.when(scanAsAServicePropertyService.getPolicyStatus(artifactPath)).thenReturn(Optional.ofNullable(policyViolationStatus));
        Mockito.when(artifactoryPAPIService.getItemInfo(artifactPath)).thenReturn(new ItemInfo() {
            @Override public long getId() {
                return 0;
            }

            @Override public RepoPath getRepoPath() {
                return null;
            }

            @Override public boolean isFolder() {
                return false;
            }

            @Override public String getName() {
                return null;
            }

            @Override public String getRepoKey() {
                return null;
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
        CancelDecision actualResult = sut.getCancelDecision(artifactPath);
        assertEquals(expectedResult, actualResult);
    }
}
