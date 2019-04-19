package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.UriSingleResponse;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VulnerabilityView;
import com.synopsys.integration.blackduck.api.manual.component.VulnerabilityNotificationContent;
import com.synopsys.integration.blackduck.api.manual.view.NotificationUserView;
import com.synopsys.integration.blackduck.api.manual.view.VulnerabilityNotificationUserView;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityNotification;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class NotificationProcessor {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final BlackDuckService blackDuckService;

    public NotificationProcessor(final BlackDuckService blackDuckService) {
        this.blackDuckService = blackDuckService;
    }

    public List<VulnerabilityNotification> getVulnerabilityNotifications(final List<NotificationUserView> notificationUserViews) {
        return notificationUserViews.stream()
                   .filter(notificationUserView -> notificationUserView instanceof VulnerabilityNotificationUserView)
                   .map(notificationUserView -> (VulnerabilityNotificationUserView) notificationUserView)
                   .map(this::aggregateVulnerabilityNotification)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .collect(Collectors.toList());
    }

    private Optional<VulnerabilityNotification> aggregateVulnerabilityNotification(final VulnerabilityNotificationUserView notificationUserView) {
        VulnerabilityNotification vulnerabilityNotification = null;
        try {
            final VulnerabilityNotificationContent content = notificationUserView.getContent();
            final String componentVersionOriginId = content.getComponentVersionOriginId();
            final UriSingleResponse<ComponentVersionView> componentVersionViewUri = new UriSingleResponse<>(content.getComponentVersion(), ComponentVersionView.class);
            final ComponentVersionView componentVersionView = blackDuckService.getResponse(componentVersionViewUri);
            final Optional<String> vulnerabilitiesLink = componentVersionView.getFirstLink(ComponentVersionView.VULNERABILITIES_LINK);

            if (vulnerabilitiesLink.isPresent()) {
                final List<VulnerabilityView> componentVulnerabilities = blackDuckService.getAllResponses(vulnerabilitiesLink.get(), VulnerabilityView.class);
                final VulnerabilityAggregate vulnerabilityAggregate = VulnerabilityAggregate.fromVulnerabilityViews(componentVulnerabilities);
                vulnerabilityNotification = new VulnerabilityNotification(content.getAffectedProjectVersions(), componentVersionView, vulnerabilityAggregate);
            } else {
                throw new IntegrationException(String.format("Failed to find vulnerabilities link for component with origin id '%s'", componentVersionOriginId));
            }
        } catch (final IntegrationException e) {
            logger.debug("An error occurred when attempting to aggregate vulnerabilities from a notification.", e);
        }

        return Optional.ofNullable(vulnerabilityNotification);
    }
}
