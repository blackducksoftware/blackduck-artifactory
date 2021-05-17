/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package modules.inspection.notifications.processor;

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
import com.synopsys.integration.blackduck.api.generated.component.VulnerabilityCvss2View;
import com.synopsys.integration.blackduck.api.generated.enumeration.VulnerabilitySeverityType;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VulnerabilityView;
import com.synopsys.integration.blackduck.api.manual.component.AffectedProjectVersion;
import com.synopsys.integration.blackduck.api.manual.component.VulnerabilityNotificationContent;
import com.synopsys.integration.blackduck.api.manual.view.VulnerabilityNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.RepositoryProjectNameLookup;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.VulnerabilityNotificationService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.ProcessedVulnerabilityNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor.VulnerabilityProcessor;
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
        vulnerabilityResourceLink.setHref(new HttpUrl("https://blackduck-server/vulnerability/link/url"));
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
        content.setComponentVersion("https://synopsys.com/api/components/08f3bea3-fbfb-4f01-97dd-3f49419f3ea9/versions/e7142eee-d1a2-4b8e-ba87-01f84ac82b1f");
        notificationUserView.setContent(content);

        List<ProcessedVulnerabilityNotification> processedVulnerabilityNotifications = vulnerabilityProcessor.processVulnerabilityNotifications(Collections.singletonList(notificationUserView), repositoryFilter);

        Assertions.assertEquals(1, processedVulnerabilityNotifications.size());

        ProcessedVulnerabilityNotification processedVulnerabilityNotification = processedVulnerabilityNotifications.get(0);

        Assertions.assertEquals("component-name", processedVulnerabilityNotification.getComponentName());
        Assertions.assertEquals("component-version-name-1.0", processedVulnerabilityNotification.getComponentVersionName());
        Assertions.assertEquals("e7142eee-d1a2-4b8e-ba87-01f84ac82b1f", processedVulnerabilityNotification.getComponentVersionId());
        Assertions.assertEquals(Collections.singletonList(repoPath), processedVulnerabilityNotification.getAffectedRepoKeyPaths());
        VulnerabilityAggregate aggregate = processedVulnerabilityNotification.getVulnerabilityAggregate();
        Assertions.assertEquals(2, aggregate.getHighSeverityCount());
        Assertions.assertEquals(1, aggregate.getMediumSeverityCount());
        Assertions.assertEquals(1, aggregate.getLowSeverityCount());
    }

    private VulnerabilityView createView(String severity) {
        VulnerabilityView vulnerabilityView = new VulnerabilityView();
        VulnerabilitySeverityType severityType = VulnerabilitySeverityType.valueOf(severity);
        vulnerabilityView.setSeverity(severityType);
        // Expect a NullPointerException if the VulnerabilityAggregate uses the wrong cvss type.
        VulnerabilityCvss2View cvss2 = new VulnerabilityCvss2View();
        cvss2.setSeverity(severityType);
        vulnerabilityView.setCvss2(cvss2);

        return vulnerabilityView;
    }
}
