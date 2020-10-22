package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.api.enumeration.PolicySeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.artifactory.ArtifactSearchService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.TestUtil;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.PolicyNotifications;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.PolicyOverrideProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.PolicyRuleClearedProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.PolicyViolationProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.ProcessedPolicyNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.ProcessedVulnerabilityNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.VulnerabilityProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.util.NameVersion;

class ArtifactNotificationServiceTest {

    @Test
    void updateMetadataFromNotifications() throws IntegrationException {
        ArtifactSearchService artifactSearchService = Mockito.mock(ArtifactSearchService.class);
        PolicyNotificationService policyNotificationService = Mockito.mock(PolicyNotificationService.class);
        VulnerabilityNotificationService vulnerabilityNotificationService = Mockito.mock(VulnerabilityNotificationService.class);
        PolicyOverrideProcessor policyOverrideProcessor = Mockito.mock(PolicyOverrideProcessor.class);
        PolicyRuleClearedProcessor policyRuleClearedProcessor = Mockito.mock(PolicyRuleClearedProcessor.class);
        PolicyViolationProcessor policyViolationProcessor = Mockito.mock(PolicyViolationProcessor.class);
        VulnerabilityProcessor vulnerabilityProcessor = Mockito.mock(VulnerabilityProcessor.class);

        PluginRepoPathFactory repoPathFactory = new PluginRepoPathFactory(false);
        RepoPath repoKeyPath1 = repoPathFactory.create("repo-1");
        RepoPath repoKeyPath2 = repoPathFactory.create("repo-2");
        List<RepoPath> toBeAffectedRepoKeys = Collections.singletonList(repoKeyPath1);
        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + 1000);

        PolicyNotifications policyNotifications = new PolicyNotifications(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        Mockito.when(policyNotificationService.fetchPolicyNotifications(startDate, endDate)).thenReturn(policyNotifications);

        Map<NameVersion, RepoPath> nameVersionRepoPathMap = new HashMap<>();
        nameVersionRepoPathMap.put(new NameVersion("repo-1", "version"), repoKeyPath1);
        RepositoryProjectNameLookup repositoryProjectNameLookup = new RepositoryProjectNameLookup(nameVersionRepoPathMap);

        String policyOverrideComponentName = "policy-override-component";
        String policyOverrideComponentVersion = "1.0";
        RepoPath policyOverrideComponentRepoPath = repoPathFactory.create(repoKeyPath1.getRepoKey(), policyOverrideComponentName);
        PolicyStatusReport policyOverrideStatusReport = new PolicyStatusReport(PolicySummaryStatusType.IN_VIOLATION_OVERRIDDEN, Collections.singletonList(PolicySeverityType.BLOCKER));
        ProcessedPolicyNotification processedPolicyOverrideNotification = new ProcessedPolicyNotification(policyOverrideComponentName, policyOverrideComponentVersion, policyOverrideStatusReport, toBeAffectedRepoKeys);
        Mockito.when(policyOverrideProcessor.processPolicyOverrideNotifications(Mockito.anyList(), Mockito.any())).thenReturn(Collections.singletonList(processedPolicyOverrideNotification));
        Mockito.when(artifactSearchService.findArtifactsUsingComponentNameVersions(policyOverrideComponentName, policyOverrideComponentVersion, toBeAffectedRepoKeys))
            .thenReturn(Collections.singletonList(policyOverrideComponentRepoPath));

        String policyClearedComponentName = "policy-cleared-component";
        String policyClearedComponentVersion = "2.0";
        RepoPath policyClearedComponentRepoPath = repoPathFactory.create(repoKeyPath1.getRepoKey(), policyClearedComponentName);
        PolicyStatusReport policyClearedStatusReport = new PolicyStatusReport(PolicySummaryStatusType.NOT_IN_VIOLATION, Collections.emptyList());
        ProcessedPolicyNotification processedPolicyClearedNotification = new ProcessedPolicyNotification(policyClearedComponentName, policyClearedComponentVersion, policyClearedStatusReport, toBeAffectedRepoKeys);
        Mockito.when(policyRuleClearedProcessor.processPolicyRuleClearedNotifications(Mockito.anyList(), Mockito.any())).thenReturn(Collections.singletonList(processedPolicyClearedNotification));
        Mockito.when(artifactSearchService.findArtifactsUsingComponentNameVersions(policyClearedComponentName, policyClearedComponentVersion, toBeAffectedRepoKeys))
            .thenReturn(Collections.singletonList(policyClearedComponentRepoPath));

        String policyViolationComponentName = "policy-violation-component";
        String policyViolationComponentVersion = "3.0";
        RepoPath policyViolationComponentRepoPath = repoPathFactory.create(repoKeyPath1.getRepoKey(), policyViolationComponentName);
        PolicyStatusReport policyViolationStatusReport = new PolicyStatusReport(PolicySummaryStatusType.IN_VIOLATION, Collections.singletonList(PolicySeverityType.BLOCKER));
        ProcessedPolicyNotification processedPolicyViolationNotification = new ProcessedPolicyNotification(policyViolationComponentName, policyViolationComponentVersion, policyViolationStatusReport, toBeAffectedRepoKeys);
        Mockito.when(policyViolationProcessor.processPolicyViolationNotifications(Mockito.anyList(), Mockito.any())).thenReturn(Collections.singletonList(processedPolicyViolationNotification));
        Mockito.when(artifactSearchService.findArtifactsUsingComponentNameVersions(policyViolationComponentName, policyViolationComponentVersion, toBeAffectedRepoKeys))
            .thenReturn(Collections.singletonList(policyViolationComponentRepoPath));

        String vulnerableComponentName = "vulnerable-component";
        String vulnerableComponentVersion = "4.0";
        RepoPath vulnerableComponentRepoPath = repoPathFactory.create(repoKeyPath1.getRepoKey(), vulnerableComponentName);
        VulnerabilityAggregate vulnerabilityAggregate = new VulnerabilityAggregate(3, 2, 1);
        ProcessedVulnerabilityNotification processedVulnerabilityNotification = new ProcessedVulnerabilityNotification(vulnerableComponentName, vulnerableComponentVersion, toBeAffectedRepoKeys, vulnerabilityAggregate);
        Mockito.when(vulnerabilityProcessor.processVulnerabilityNotifications(Mockito.anyList(), Mockito.any())).thenReturn(Collections.singletonList(processedVulnerabilityNotification));
        Mockito.when(artifactSearchService.findArtifactsUsingComponentNameVersions(vulnerableComponentName, vulnerableComponentVersion, toBeAffectedRepoKeys))
            .thenReturn(Collections.singletonList(vulnerableComponentRepoPath));

        Map<RepoPath, Map<String, String>> propertyMap = new HashMap<>();
        InspectionPropertyService inspectionPropertyService = TestUtil.INSTANCE.createSpoofedInspectionPropertyService(propertyMap);

        ArtifactNotificationService artifactNotificationService = new ArtifactNotificationService(artifactSearchService, inspectionPropertyService, policyNotificationService, vulnerabilityNotificationService, policyOverrideProcessor,
            policyRuleClearedProcessor, policyViolationProcessor, vulnerabilityProcessor);

        artifactNotificationService.updateMetadataFromNotifications(Arrays.asList(repoKeyPath1, repoKeyPath2), startDate, endDate);

        assertPolicyProperties(propertyMap, policyOverrideComponentRepoPath, policyOverrideStatusReport);
        assertPolicyProperties(propertyMap, policyClearedComponentRepoPath, policyClearedStatusReport);
        assertPolicyProperties(propertyMap, policyViolationComponentRepoPath, policyViolationStatusReport);
        assertVulnerabilityProperties(propertyMap, vulnerableComponentRepoPath, vulnerabilityAggregate);
    }

    private void assertVulnerabilityProperties(Map<RepoPath, Map<String, String>> propertyMap, RepoPath repoPath, VulnerabilityAggregate expectedVulnerabilityAggregate) {
        assertPropertyValue(propertyMap, repoPath, BlackDuckArtifactoryProperty.LOW_VULNERABILITIES, String.valueOf(expectedVulnerabilityAggregate.getLowSeverityCount()));
        assertPropertyValue(propertyMap, repoPath, BlackDuckArtifactoryProperty.MEDIUM_VULNERABILITIES, String.valueOf(expectedVulnerabilityAggregate.getMediumSeverityCount()));
        assertPropertyValue(propertyMap, repoPath, BlackDuckArtifactoryProperty.HIGH_VULNERABILITIES, String.valueOf(expectedVulnerabilityAggregate.getHighSeverityCount()));
    }

    private void assertPolicyProperties(Map<RepoPath, Map<String, String>> propertyMap, RepoPath repoPath, PolicyStatusReport expectedPolicyStatusReport) {
        assertPropertyValue(propertyMap, repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, expectedPolicyStatusReport.getPolicySummaryStatusType().name());
        if (expectedPolicyStatusReport.getPolicySeverityTypes().isEmpty()) {
            assertMissingProperty(propertyMap, repoPath, BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES);
        } else {
            String policySeverityTypes = StringUtils.join(expectedPolicyStatusReport.getPolicySeverityTypes(), ",");
            assertPropertyValue(propertyMap, repoPath, BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES, policySeverityTypes);
        }
    }

    private void assertMissingProperty(Map<RepoPath, Map<String, String>> propertyMap, RepoPath repoPath, BlackDuckArtifactoryProperty property) {
        Map<String, String> properties = propertyMap.get(repoPath);
        Assertions.assertNotNull(properties);
        String propertyValue = properties.get(property.getPropertyName());
        Assertions.assertNull(propertyValue);
    }

    private void assertPropertyValue(Map<RepoPath, Map<String, String>> propertyMap, RepoPath repoPath, BlackDuckArtifactoryProperty property, String expectedPropertyValue) {
        Map<String, String> properties = propertyMap.get(repoPath);
        Assertions.assertNotNull(properties);
        String propertyValue = properties.get(property.getPropertyName());
        Assertions.assertNotNull(propertyValue);
        Assertions.assertEquals(expectedPropertyValue, propertyValue);
    }
}