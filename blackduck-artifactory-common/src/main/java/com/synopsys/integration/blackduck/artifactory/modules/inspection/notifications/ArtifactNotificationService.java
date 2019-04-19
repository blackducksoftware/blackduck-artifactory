package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.discovery.ApiDiscovery;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.OriginView;
import com.synopsys.integration.blackduck.api.generated.view.UserView;
import com.synopsys.integration.blackduck.api.manual.component.AffectedProjectVersion;
import com.synopsys.integration.blackduck.api.manual.view.NotificationUserView;
import com.synopsys.integration.blackduck.artifactory.ArtifactSearchService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.AffectedArtifact;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.BlackDuckNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.PolicyVulnerabilityAggregate;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityNotification;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.NotificationService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ArtifactNotificationService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final NotificationProcessor notificationProcessor;
    private final BlackDuckService blackDuckService;
    private final NotificationService notificationService;
    private final ArtifactSearchService artifactSearchService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final InspectionPropertyService inspectionPropertyService;

    public ArtifactNotificationService(final NotificationProcessor notificationProcessor, final BlackDuckService blackDuckService, final NotificationService notificationService, final ArtifactSearchService artifactSearchService,
        final ArtifactoryPropertyService artifactoryPropertyService, final InspectionPropertyService inspectionPropertyService) {
        this.notificationProcessor = notificationProcessor;
        this.blackDuckService = blackDuckService;
        this.notificationService = notificationService;
        this.artifactSearchService = artifactSearchService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.inspectionPropertyService = inspectionPropertyService;
    }

    // TODO: Process other type of notifications other than vulnerabilities
    public void updateMetadataFromNotifications(final List<String> repoKeys, final Date startDate, final Date endDate) throws IntegrationException {
        final UserView currentUser = blackDuckService.getResponse(ApiDiscovery.CURRENT_USER_LINK_RESPONSE);
        final List<NotificationUserView> notificationUserViews = notificationService.getAllUserNotifications(currentUser, startDate, endDate);
        final Map<String, PolicyVulnerabilityAggregate.Builder> artifactMetadataAggregateMap = new HashMap<>();
        final List<VulnerabilityNotification> vulnerabilityNotifications = notificationProcessor.getVulnerabilityNotifications(notificationUserViews);

        processVulnerabilityNotifications(repoKeys, vulnerabilityNotifications, artifactMetadataAggregateMap);

        for (final Map.Entry<String, PolicyVulnerabilityAggregate.Builder> entry : artifactMetadataAggregateMap.entrySet()) {
            final RepoPath repoPath = RepoPathFactory.create(entry.getKey());
            final PolicyVulnerabilityAggregate policyVulnerabilityAggregate = entry.getValue().build();

            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.HIGH_VULNERABILITIES2, policyVulnerabilityAggregate.getHighVulnerabilities(), logger);
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.MEDIUM_VULNERABILITIES2, policyVulnerabilityAggregate.getMediumVulnerabilities(), logger);
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.LOW_VULNERABILITIES2, policyVulnerabilityAggregate.getLowVulnerabilities(), logger);
            policyVulnerabilityAggregate.getComponentVersionUrl().ifPresent(componentVersionUrl -> artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.COMPONENT_VERSION_URL2, componentVersionUrl, logger));
        }

        //        final PolicyVulnerabilityAggregate policyVulnerabilityAggregate = PolicyVulnerabilityAggregate.fromVulnerabilityAggregate(vulnerabilityAggregate, null, null); // TODO
        //        inspectionPropertyService.setPolicyAndVulnerabilityProperties(repoPath, policyVulnerabilityAggregate);
    }

    private void processVulnerabilityNotifications(final List<String> repoKeys, final List<VulnerabilityNotification> vulnerabilityNotifications, final Map<String, PolicyVulnerabilityAggregate.Builder> artifactMetadataAggregateMap) {
        final List<AffectedArtifact<VulnerabilityNotification>> affectedArtifacts = vulnerabilityNotifications.stream()
                                                                                        .map(notification -> findAffectedArtifacts(repoKeys, notification))
                                                                                        .flatMap(List::stream)
                                                                                        .collect(Collectors.toList());

        for (final AffectedArtifact<VulnerabilityNotification> affectedArtifact : affectedArtifacts) {
            final String key = affectedArtifact.getRepoPath().getId().replaceFirst(":", "/");
            final VulnerabilityNotification vulnerabilityNotification = affectedArtifact.getBlackDuckNotification();
            final VulnerabilityAggregate vulnerabilityAggregate = vulnerabilityNotification.getVulnerabilityAggregate();
            final Optional<String> href = affectedArtifact.getBlackDuckNotification().getComponentVersionView().getHref();

            final PolicyVulnerabilityAggregate.Builder builder;
            if (artifactMetadataAggregateMap.containsKey(key)) {
                builder = artifactMetadataAggregateMap.get(key);
            } else {
                builder = new PolicyVulnerabilityAggregate.Builder();
                artifactMetadataAggregateMap.put(key, builder);
            }

            builder.setHighVulnerabilities(vulnerabilityAggregate.getHighSeverityCount());
            builder.setMediumVulnerabilities(vulnerabilityAggregate.getMediumSeverityCount());
            builder.setLowVulnerabilities(vulnerabilityAggregate.getLowSeverityCount());
            builder.setComponentVersionUrl(href.orElse(null));
        }
    }

    private <T extends BlackDuckNotification> List<AffectedArtifact<T>> findAffectedArtifacts(final List<String> repoKeys, final T notification) {
        final List<AffectedArtifact<T>> affectedArtifacts = new ArrayList<>();

        try {
            final List<AffectedProjectVersion> affectedProjectVersions = notification.getAffectedProjectVersions();
            final String[] affectedRepoKeys = determineAffectedRepos(repoKeys, affectedProjectVersions).toArray(new String[0]);
            final ComponentVersionView componentVersionView = notification.getComponentVersionView();
            final List<OriginView> originViews = blackDuckService.getAllResponses(componentVersionView, ComponentVersionView.ORIGINS_LINK_RESPONSE);

            for (final OriginView originView : originViews) {
                final String forge = originView.getOriginName();
                final String originId = originView.getOriginId();
                final List<AffectedArtifact<T>> artifactsWithOriginId = artifactSearchService.findArtifactsWithOriginId(forge, originId, affectedRepoKeys).stream()
                                                                            .map(repoPath -> new AffectedArtifact<>(repoPath, notification))
                                                                            .collect(Collectors.toList());
                affectedArtifacts.addAll(artifactsWithOriginId);
            }

            return affectedArtifacts;
        } catch (final IntegrationException e) {
            logger.error(String.format("Failed to get origins for: %s", notification.getComponentVersionView().getHref().orElse("Unknown")), e);
        }

        return affectedArtifacts;
    }

    private List<String> determineAffectedRepos(final List<String> repoKeys, final List<AffectedProjectVersion> affectedProjectVersions) {
        final List<String> affectedRepos = new ArrayList<>();
        final Map<String, String> nameVersionToRepoKeyMap = projectNameVersionToRepoKey(repoKeys);
        for (final AffectedProjectVersion affectedProjectVersion : affectedProjectVersions) {
            final String projectName = affectedProjectVersion.getProjectName();
            final String projectVersionName = affectedProjectVersion.getProjectVersionName();
            final String projectNameVersionKey = generateProjectNameKey(projectName, projectVersionName);
            final String repoKey = nameVersionToRepoKeyMap.get(projectNameVersionKey);
            affectedRepos.add(repoKey);
        }

        return affectedRepos;
    }

    private Map<String, String> projectNameVersionToRepoKey(final List<String> repoKeys) {
        final Map<String, String> nameVersionToRepoKeyMap = new HashMap<>();
        for (final String repoKey : repoKeys) {
            final String repoProjectName = inspectionPropertyService.getRepoProjectName(repoKey);
            final String repoProjectVersionName = inspectionPropertyService.getRepoProjectVersionName(repoKey);
            final String key = generateProjectNameKey(repoProjectName, repoProjectVersionName);
            nameVersionToRepoKeyMap.put(key, repoKey);
        }

        return nameVersionToRepoKeyMap;
    }

    private String generateProjectNameKey(final String projectName, final String projectVersionName) {
        return projectName + ":" + projectVersionName;
    }
}
