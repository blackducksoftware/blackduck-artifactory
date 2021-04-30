package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import java.util.Arrays;
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

import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;

class PolicyCancelDeciderTest {

    private final static RepoPath REPO_PATH = new PluginRepoPathFactory(false).create("repo", "scannedArtifact.zip");

    private static Stream<Arguments> provideConfigForCancelDecision() {

        List<String> policyRepos = Collections.singletonList(REPO_PATH.getRepoKey());

        List<PolicyRuleSeverityType> onlyMajorSeverityType = Collections.singletonList(PolicyRuleSeverityType.MAJOR);
        List<PolicyRuleSeverityType> trivialMajorSeverityTypes = Arrays.asList(PolicyRuleSeverityType.TRIVIAL, PolicyRuleSeverityType.MAJOR);
        List<PolicyRuleSeverityType> trivialMajorCriticalSeverityTypes = Arrays.asList(PolicyRuleSeverityType.TRIVIAL, PolicyRuleSeverityType.MAJOR, PolicyRuleSeverityType.CRITICAL);

        return Stream.of(
            // Should cancel download for scanned artifact
            Arguments.of(true, true, true, false, policyRepos, Collections.emptyList(), null, null, null), // Has SCAN_TIME, but not OVERALL_POLICY_STATUS
            Arguments.of(true, true, true, true, policyRepos, onlyMajorSeverityType, null, "IN_VIOLATION", null), // Has SCAN_TIME and OVERALL_POLICY_STATUS of value IN_VIOLATION, but no POLICY_SEVERITY_TYPES
            Arguments.of(true, true, true, true, policyRepos, trivialMajorSeverityTypes, "CRITICAL,MAJOR", "IN_VIOLATION", null), // IN_VIOLATION with matching severity
            Arguments.of(true, true, true, true, policyRepos, trivialMajorCriticalSeverityTypes, "CRITICAL,MAJOR", "IN_VIOLATION", null), // IN_VIOLATION with multiple matching severity types
            // Should NOT cancel download for scanned artifact
            Arguments.of(false, false, true, true, Collections.emptyList(), Collections.emptyList(), null, null, null), // Blocking disabled
            Arguments.of(false, true, true, true, Collections.singletonList("non-policy-repo"), Collections.emptyList(), null, null, null), // Repo not configured for policy
            Arguments.of(false, true, true, true, policyRepos, onlyMajorSeverityType, "TRIVIAL", "IN_VIOLATION", null), // Non-matching severity types
            Arguments.of(false, true, true, true, policyRepos, onlyMajorSeverityType, null, "NOT_IN_VIOLATION", null), // Not in violation of policy
            Arguments.of(false, true, true, true, policyRepos, onlyMajorSeverityType, "MAJOR", "IN_VIOLATION_OVERRIDDEN", null), // In violation of policy, but overridden in BlackDuck
            Arguments.of(false, true, true, true, policyRepos, onlyMajorSeverityType, "TRIVIAL", "IN_VIOLATION_OVERRIDDEN", null), // In violation of non-matching policy, also overridden in BlackDuck

            // Should cancel download for inspected artifact
            Arguments.of(true, true, false, true, policyRepos, onlyMajorSeverityType, "CRITICAL,MAJOR", null, "IN_VIOLATION"), // OVERALL_POLICY_STATUS shouldn't matter
            Arguments.of(true, true, false, false, policyRepos, trivialMajorSeverityTypes, "CRITICAL,MAJOR", null, "IN_VIOLATION"), // IN_VIOLATION with matching severity
            Arguments.of(true, true, false, false, policyRepos, trivialMajorCriticalSeverityTypes, "CRITICAL,MAJOR", null, "IN_VIOLATION"), // IN_VIOLATION with multiple matching severity types
            // Should NOT cancel download of inspected artifact
            Arguments.of(false, false, false, false, Collections.emptyList(), Collections.emptyList(), null, null, null), // Blocking disabled
            Arguments.of(false, true, false, false, Collections.singletonList("non-policy-repo"), Collections.emptyList(), null, null, null), // Repo not configured for policy
            Arguments.of(false, true, false, false, policyRepos, onlyMajorSeverityType, "TRIVIAL", null, "IN_VIOLATION"), // Non-matching severity types
            Arguments.of(false, true, false, false, policyRepos, onlyMajorSeverityType, null, null, "NOT_IN_VIOLATION"), // Not in violation of policy
            Arguments.of(false, true, false, false, policyRepos, onlyMajorSeverityType, "MAJOR", null, "IN_VIOLATION_OVERRIDDEN"), // In violation of policy, but overridden in BlackDuck
            Arguments.of(false, true, false, false, policyRepos, onlyMajorSeverityType, "TRIVIAL", null, "IN_VIOLATION_OVERRIDDEN") // In violation of non-matching policy, also overridden in BlackDuck
        );
    }

    @ParameterizedTest
    @MethodSource("provideConfigForCancelDecision")
    void testDecision(
        boolean shouldCancel,
        boolean policyBlockEnabled,
        boolean hasScanTimeProperty,
        boolean hasOverallPolicyStatus,
        List<String> policyRepos,
        List<PolicyRuleSeverityType> policyRuleSeverityTypes,
        @Nullable String policySeverityTypesPropertyValue,
        @Nullable String overallPolicyStatusPropertyValue,
        @Nullable String policyStatusPropertyValue
    ) {
        ArtifactoryPropertyService artifactoryPropertyService = Mockito.mock(ArtifactoryPropertyService.class);
        Mockito.when(artifactoryPropertyService.hasProperty(REPO_PATH, BlackDuckArtifactoryProperty.SCAN_TIME)).thenReturn(hasScanTimeProperty);
        Mockito.when(artifactoryPropertyService.hasProperty(REPO_PATH, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS)).thenReturn(hasOverallPolicyStatus);
        Mockito.when(artifactoryPropertyService.getProperty(REPO_PATH, BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES)).thenReturn(Optional.ofNullable(policySeverityTypesPropertyValue));
        Mockito.when(artifactoryPropertyService.getProperty(REPO_PATH, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS)).thenReturn(Optional.ofNullable(overallPolicyStatusPropertyValue));
        Mockito.when(artifactoryPropertyService.getProperty(REPO_PATH, BlackDuckArtifactoryProperty.POLICY_STATUS)).thenReturn(Optional.ofNullable(policyStatusPropertyValue));

        CancelDecider policyCancelDecider = new PolicyCancelDecider(artifactoryPropertyService, policyBlockEnabled, policyRepos, policyRuleSeverityTypes);
        CancelDeciderTestUtil.assertCancellationDecision(shouldCancel, REPO_PATH, policyCancelDecider);
    }

}
