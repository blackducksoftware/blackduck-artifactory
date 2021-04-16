/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyStatusType;
import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionVulnerableBomComponentsItemsVulnerabilityWithRemediationSeverityType;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.artifactory.ArtifactSearchService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.TestUtil;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.PolicyNotifications;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.NotificationProcessor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.ProcessedPolicyNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.ProcessedVulnerabilityNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.ArtifactInspectionService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;

class ArtifactNotificationServiceTest {
    @Test
    void updateMetadataFromNotifications() throws IntegrationException {
        ArtifactSearchService artifactSearchService = Mockito.mock(ArtifactSearchService.class);
        PolicyNotificationService policyNotificationService = Mockito.mock(PolicyNotificationService.class);
        VulnerabilityNotificationService vulnerabilityNotificationService = Mockito.mock(VulnerabilityNotificationService.class);

        PluginRepoPathFactory repoPathFactory = new PluginRepoPathFactory(false);
        RepoPath repoKeyPath1 = repoPathFactory.create("repo-1");
        RepoPath repoKeyPath2 = repoPathFactory.create("repo-2");
        RepoPath deletedProjectRepoKeyPath = repoPathFactory.create("repo-3");
        List<RepoPath> toBeAffectedRepoKeys = Collections.singletonList(repoKeyPath1);
        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + 1000);

        PolicyNotifications policyNotifications = new PolicyNotifications(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        Mockito.when(policyNotificationService.fetchPolicyNotifications(startDate, endDate)).thenReturn(policyNotifications);

        String policyOverrideComponentName = "policy-override-component";
        String policyOverrideComponentVersion = "1.0";
        RepoPath policyOverrideComponentRepoPath = repoPathFactory.create(repoKeyPath1.getRepoKey(), policyOverrideComponentName);
        PolicyStatusReport policyOverrideStatusReport = new PolicyStatusReport(PolicyStatusType.IN_VIOLATION_OVERRIDDEN, Collections.singletonList(PolicyRuleSeverityType.BLOCKER));
        ProcessedPolicyNotification processedPolicyOverrideNotification = new ProcessedPolicyNotification(policyOverrideComponentName, policyOverrideComponentVersion, policyOverrideStatusReport, toBeAffectedRepoKeys);
        Mockito.when(artifactSearchService.findArtifactsUsingComponentNameVersions(policyOverrideComponentName, policyOverrideComponentVersion, toBeAffectedRepoKeys))
            .thenReturn(Collections.singletonList(policyOverrideComponentRepoPath));

        String policyClearedComponentName = "policy-cleared-component";
        String policyClearedComponentVersion = "2.0";
        RepoPath policyClearedComponentRepoPath = repoPathFactory.create(repoKeyPath1.getRepoKey(), policyClearedComponentName);
        PolicyStatusReport policyClearedStatusReport = new PolicyStatusReport(PolicyStatusType.NOT_IN_VIOLATION, Collections.emptyList());
        ProcessedPolicyNotification processedPolicyClearedNotification = new ProcessedPolicyNotification(policyClearedComponentName, policyClearedComponentVersion, policyClearedStatusReport, toBeAffectedRepoKeys);
        Mockito.when(artifactSearchService.findArtifactsUsingComponentNameVersions(policyClearedComponentName, policyClearedComponentVersion, toBeAffectedRepoKeys))
            .thenReturn(Collections.singletonList(policyClearedComponentRepoPath));

        String policyViolationComponentName = "policy-violation-component";
        String policyViolationComponentVersion = "3.0";
        RepoPath policyViolationComponentRepoPath = repoPathFactory.create(repoKeyPath1.getRepoKey(), policyViolationComponentName);
        PolicyStatusReport policyViolationStatusReport = new PolicyStatusReport(PolicyStatusType.IN_VIOLATION, Collections.singletonList(PolicyRuleSeverityType.BLOCKER));
        ProcessedPolicyNotification processedPolicyViolationNotification = new ProcessedPolicyNotification(policyViolationComponentName, policyViolationComponentVersion, policyViolationStatusReport, toBeAffectedRepoKeys);
        Mockito.when(artifactSearchService.findArtifactsUsingComponentNameVersions(policyViolationComponentName, policyViolationComponentVersion, toBeAffectedRepoKeys))
            .thenReturn(Collections.singletonList(policyViolationComponentRepoPath));

        String vulnerableComponentName = "vulnerable-component";
        String vulnerableComponentVersion = "4.0";
        RepoPath vulnerableComponentRepoPath = repoPathFactory.create(repoKeyPath1.getRepoKey(), vulnerableComponentName);

        HashMap<ProjectVersionVulnerableBomComponentsItemsVulnerabilityWithRemediationSeverityType, Integer> severityMap = new HashMap<>();
        severityMap.put(ProjectVersionVulnerableBomComponentsItemsVulnerabilityWithRemediationSeverityType.CRITICAL, 4);
        severityMap.put(ProjectVersionVulnerableBomComponentsItemsVulnerabilityWithRemediationSeverityType.HIGH, 3);
        severityMap.put(ProjectVersionVulnerableBomComponentsItemsVulnerabilityWithRemediationSeverityType.MEDIUM, 2);
        severityMap.put(ProjectVersionVulnerableBomComponentsItemsVulnerabilityWithRemediationSeverityType.LOW, 1);
        VulnerabilityAggregate vulnerabilityAggregate = new VulnerabilityAggregate(severityMap);

        ProcessedVulnerabilityNotification processedVulnerabilityNotification = new ProcessedVulnerabilityNotification(vulnerableComponentName, vulnerableComponentVersion, toBeAffectedRepoKeys, vulnerabilityAggregate);
        Mockito.when(artifactSearchService.findArtifactsUsingComponentNameVersions(vulnerableComponentName, vulnerableComponentVersion, toBeAffectedRepoKeys))
            .thenReturn(Collections.singletonList(vulnerableComponentRepoPath));

        Map<RepoPath, Map<String, String>> propertyMap = new HashMap<>();
        InspectionPropertyService inspectionPropertyService = TestUtil.INSTANCE.createSpoofedInspectionPropertyService(propertyMap);

        NotificationProcessor notificationProcessor = Mockito.mock(NotificationProcessor.class);
        Mockito.when(notificationProcessor.processPolicyNotifications(Mockito.any(), Mockito.any())).thenReturn(Arrays.asList(processedPolicyOverrideNotification, processedPolicyClearedNotification, processedPolicyViolationNotification));
        Mockito.when(notificationProcessor.processVulnerabilityNotifications(Mockito.any(), Mockito.any())).thenReturn(Collections.singletonList(processedVulnerabilityNotification));

        ArtifactInspectionService artifactInspectionService = Mockito.mock(ArtifactInspectionService.class);
        ProjectVersionView projectVersionView = Mockito.mock(ProjectVersionView.class);
        Mockito.when(projectVersionView.getFirstLinkSafely(ProjectVersionView.COMPONENTS_LINK)).thenReturn(Optional.of(new HttpUrl("https://synopsys.com")));
        ProjectVersionWrapper projectVersionWrapper = new ProjectVersionWrapper();
        projectVersionWrapper.setProjectVersionView(projectVersionView);
        Mockito.when(artifactInspectionService.fetchProjectVersionWrapper(repoKeyPath1.getRepoKey())).thenReturn(projectVersionWrapper);
        Mockito.when(artifactInspectionService.fetchProjectVersionWrapper(repoKeyPath2.getRepoKey())).thenReturn(projectVersionWrapper);
        Mockito.when(artifactInspectionService.fetchProjectVersionWrapper(deletedProjectRepoKeyPath.getRepoKey())).thenThrow(new IntegrationException("Missing project version in Black Duck."));

        ArtifactNotificationService artifactNotificationService = new ArtifactNotificationService(artifactSearchService, inspectionPropertyService, artifactInspectionService, policyNotificationService, vulnerabilityNotificationService,
            notificationProcessor);

        artifactNotificationService.updateMetadataFromNotifications(Arrays.asList(repoKeyPath1, repoKeyPath2, deletedProjectRepoKeyPath), startDate, endDate);

        assertRepositoryProperties(propertyMap, repoKeyPath1);
        assertRepositoryProperties(propertyMap, repoKeyPath2);
        assertPolicyProperties(propertyMap, policyOverrideComponentRepoPath, policyOverrideStatusReport);
        assertPolicyProperties(propertyMap, policyClearedComponentRepoPath, policyClearedStatusReport);
        assertPolicyProperties(propertyMap, policyViolationComponentRepoPath, policyViolationStatusReport);
        assertVulnerabilityProperties(propertyMap, vulnerableComponentRepoPath, vulnerabilityAggregate);
        assertPropertyValue(propertyMap, repoKeyPath1, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL, "https://synopsys.com");
        assertPropertyValue(propertyMap, repoKeyPath2, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL, "https://synopsys.com");

        // Deleted project version
        assertHasProperty(propertyMap, deletedProjectRepoKeyPath, BlackDuckArtifactoryProperty.LAST_INSPECTION);
        assertPropertyValue(propertyMap, deletedProjectRepoKeyPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS, InspectionStatus.FAILURE.name());
        assertHasProperty(propertyMap, deletedProjectRepoKeyPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS_MESSAGE);
    }

    private void assertRepositoryProperties(Map<RepoPath, Map<String, String>> propertyMap, RepoPath repoPath) {
        assertHasProperty(propertyMap, repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS);
        assertHasProperty(propertyMap, repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS);
        assertHasProperty(propertyMap, repoPath, BlackDuckArtifactoryProperty.LAST_UPDATE);
    }

    private void assertVulnerabilityProperties(Map<RepoPath, Map<String, String>> propertyMap, RepoPath repoPath, VulnerabilityAggregate expectedVulnerabilityAggregate) {
        assertPropertyValue(propertyMap, repoPath, BlackDuckArtifactoryProperty.LOW_VULNERABILITIES, String.valueOf(expectedVulnerabilityAggregate.getLowSeverityCount()));
        assertPropertyValue(propertyMap, repoPath, BlackDuckArtifactoryProperty.MEDIUM_VULNERABILITIES, String.valueOf(expectedVulnerabilityAggregate.getMediumSeverityCount()));
        assertPropertyValue(propertyMap, repoPath, BlackDuckArtifactoryProperty.HIGH_VULNERABILITIES, String.valueOf(expectedVulnerabilityAggregate.getHighSeverityCount()));
        assertPropertyValue(propertyMap, repoPath, BlackDuckArtifactoryProperty.CRITICAL_VULNERABILITIES, String.valueOf(expectedVulnerabilityAggregate.getCriticalSeverityCount()));
    }

    private void assertPolicyProperties(Map<RepoPath, Map<String, String>> propertyMap, RepoPath repoPath, PolicyStatusReport expectedPolicyStatusReport) {
        assertPropertyValue(propertyMap, repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, expectedPolicyStatusReport.getPolicyStatusType().name());
        if (expectedPolicyStatusReport.getPolicyRuleSeverityTypes().isEmpty()) {
            assertMissingProperty(propertyMap, repoPath, BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES);
        } else {
            String policySeverityTypes = StringUtils.join(expectedPolicyStatusReport.getPolicyRuleSeverityTypes(), ",");
            assertPropertyValue(propertyMap, repoPath, BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES, policySeverityTypes);
        }
    }

    private void assertMissingProperty(Map<RepoPath, Map<String, String>> propertyMap, RepoPath repoPath, BlackDuckArtifactoryProperty property) {
        Assertions.assertFalse(getProperty(propertyMap, repoPath, property).isPresent());
    }

    private void assertPropertyValue(Map<RepoPath, Map<String, String>> propertyMap, RepoPath repoPath, BlackDuckArtifactoryProperty property, String expectedPropertyValue) {
        Optional<String> propertyValue = getProperty(propertyMap, repoPath, property);
        Assertions.assertTrue(propertyValue.isPresent());
        Assertions.assertEquals(expectedPropertyValue, propertyValue.get());
    }

    private void assertHasProperty(Map<RepoPath, Map<String, String>> propertyMap, RepoPath repoPath, BlackDuckArtifactoryProperty property) {
        Assertions.assertTrue(getProperty(propertyMap, repoPath, property).isPresent());
    }

    private Optional<String> getProperty(Map<RepoPath, Map<String, String>> propertyMap, RepoPath repoPath, BlackDuckArtifactoryProperty property) {
        Map<String, String> properties = propertyMap.get(repoPath);
        if (properties == null) {
            return Optional.empty();
        }
        String propertyValue = properties.get(property.getPropertyName());
        return Optional.ofNullable(propertyValue);
    }
}
