package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.api.core.ResourceLink;
import com.synopsys.integration.blackduck.api.core.ResourceMetadata;
import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionVulnerableBomComponentsItemsVulnerabilityWithRemediationSeverityType;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VulnerabilityView;
import com.synopsys.integration.blackduck.api.manual.component.AffectedProjectVersion;
import com.synopsys.integration.blackduck.api.manual.component.VulnerabilityNotificationContent;
import com.synopsys.integration.blackduck.api.manual.view.VulnerabilityNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.RepositoryProjectNameLookup;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.VulnerabilityNotificationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;

class VulnerabiityProcessorTest {
    @Test
    void processVulnerabilityNotifications() throws IntegrationException {
        VulnerabilityNotificationService vulnerabilityNotificationService = Mockito.mock(VulnerabilityNotificationService.class);
        ComponentVersionView componentVersionView = new ComponentVersionView();
        componentVersionView.setVersionName("component-version-name-1.0");
        Mockito.when(vulnerabilityNotificationService.fetchComponentVersionView(Mockito.any())).thenReturn(componentVersionView);

        ResourceLink vulnerabilityResourceLink = new ResourceLink();
        vulnerabilityResourceLink.setHref(new HttpUrl("vulnerability/link/url"));
        vulnerabilityResourceLink.setRel(ComponentVersionView.VULNERABILITIES_LINK);
        ResourceMetadata resourceMetadata = new ResourceMetadata();
        resourceMetadata.setLinks(Collections.singletonList(vulnerabilityResourceLink));
        componentVersionView.setMeta(resourceMetadata);

        List<VulnerabilityView> vulnerabilityViews = Arrays.asList(createView("HIGH"), createView("MEDIUM"), createView("LOW"), createView("HIGH"));
        Mockito.when(vulnerabilityNotificationService.fetchVulnerabilitiesForComponent(componentVersionView)).thenReturn(vulnerabilityViews);

        RepositoryProjectNameLookup repositoryFilter = Mockito.mock(RepositoryProjectNameLookup.class);
        RepoPath repoPath = new PluginRepoPathFactory(false).create("repo-1");

        Mockito.when(repositoryFilter.getRepoKeyPath(Mockito.any())).thenReturn(Optional.of(repoPath));

        VulnerabilityProcessor vulnerabilityProcessor = new VulnerabilityProcessor(vulnerabilityNotificationService);
        VulnerabilityNotificationUserView notificationUserView = new VulnerabilityNotificationUserView();
        VulnerabilityNotificationContent content = new VulnerabilityNotificationContent();
        AffectedProjectVersion affectedProjectVersion = new AffectedProjectVersion();
        affectedProjectVersion.setProjectName("project-name");
        affectedProjectVersion.setProjectVersionName("project-version-name");
        content.setAffectedProjectVersions(Collections.singletonList(affectedProjectVersion));
        content.setComponentName("component-name");
        content.setComponentVersion("component/version/url");

        notificationUserView.setContent(content);

        List<ProcessedVulnerabilityNotification> processedVulnerabilityNotifications = vulnerabilityProcessor.processVulnerabilityNotifications(Collections.singletonList(notificationUserView), repositoryFilter);

        Assertions.assertEquals(1, processedVulnerabilityNotifications.size());

        ProcessedVulnerabilityNotification processedVulnerabilityNotification = processedVulnerabilityNotifications.get(0);

        Assertions.assertEquals("component-name", processedVulnerabilityNotification.getComponentName());
        Assertions.assertEquals("component-version-name-1.0", processedVulnerabilityNotification.getComponentVersionName());
        Assertions.assertEquals(Collections.singletonList(repoPath), processedVulnerabilityNotification.getAffectedRepoKeyPaths());
        VulnerabilityAggregate aggregate = processedVulnerabilityNotification.getVulnerabilityAggregate();
        Assertions.assertEquals(2, aggregate.getHighSeverityCount());
        Assertions.assertEquals(1, aggregate.getMediumSeverityCount());
        Assertions.assertEquals(1, aggregate.getLowSeverityCount());
    }

    private VulnerabilityView createView(String severity) {
        VulnerabilityView vulnerabilityView = new VulnerabilityView();
        ProjectVersionVulnerableBomComponentsItemsVulnerabilityWithRemediationSeverityType severityType = ProjectVersionVulnerableBomComponentsItemsVulnerabilityWithRemediationSeverityType.valueOf(severity);
        vulnerabilityView.setSeverity(severityType);
        return vulnerabilityView;
    }
}