/**
 * blackduck-artifactory-common
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.NotificationView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.api.generated.view.VulnerabilityV2View;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.notification.CommonNotificationView;
import com.synopsys.integration.blackduck.notification.NotificationDetailResults;
import com.synopsys.integration.blackduck.notification.content.detail.NotificationContentDetailFactory;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.CommonNotificationService;
import com.synopsys.integration.blackduck.service.NotificationService;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ArtifactMetaDataService {
    private static final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(ArtifactMetaDataService.class));

    private final BlackDuckServicesFactory blackDuckServicesFactory;

    public static ArtifactMetaDataService createDefault(final BlackDuckServerConfig blackDuckServerConfig) {
        final BlackDuckServicesFactory blackDuckServicesFactory = blackDuckServerConfig.createBlackDuckServicesFactory(logger);

        return new ArtifactMetaDataService(blackDuckServicesFactory);
    }

    public ArtifactMetaDataService(final BlackDuckServicesFactory blackDuckServicesFactory) {
        this.blackDuckServicesFactory = blackDuckServicesFactory;
    }

    public List<ArtifactMetaData> getArtifactMetadataOfRepository(final String repoKey, final String projectName, final String projectVersionName) throws IntegrationException {
        final BlackDuckService blackDuckService = blackDuckServicesFactory.createBlackDuckService();
        final ProjectService projectDataService = blackDuckServicesFactory.createProjectService();
        final CompositeComponentManager compositeComponentManager = new CompositeComponentManager(logger, blackDuckService);
        final Map<String, ArtifactMetaData> idToArtifactMetaData = new HashMap<>();

        final Optional<ProjectVersionWrapper> projectVersionWrapper = projectDataService.getProjectVersion(projectName, projectVersionName);

        if (projectVersionWrapper.isPresent()) {
            final ProjectVersionView projectVersionView = projectVersionWrapper.get().getProjectVersionView();
            final List<VersionBomComponentView> versionBomComponentViews = blackDuckService.getAllResponses(projectVersionView, ProjectVersionView.COMPONENTS_LINK_RESPONSE);
            final List<CompositeComponentModel> projectVersionComponentVersionModels = compositeComponentManager.parseBom(projectVersionView, versionBomComponentViews);

            for (final CompositeComponentModel projectVersionComponentVersionModel : projectVersionComponentVersionModels) {
                populateMetaDataMap(repoKey, idToArtifactMetaData, blackDuckService, projectVersionComponentVersionModel);
            }
        }

        return new ArrayList<>(idToArtifactMetaData.values());
    }

    public Optional<ArtifactMetaDataFromNotifications> getArtifactMetadataFromNotifications(final String repoKey, final String projectName, final String projectVersionName, final Date startDate, final Date endDate)
        throws IntegrationException {
        final NotificationService notificationService = blackDuckServicesFactory.createNotificationService();
        final CommonNotificationService commonNotificationService = blackDuckServicesFactory.createCommonNotificationService(new NotificationContentDetailFactory(BlackDuckServicesFactory.createDefaultGson()), false);
        final ProjectService projectDataService = blackDuckServicesFactory.createProjectService();
        final BlackDuckService blackDuckService = blackDuckServicesFactory.createBlackDuckService();
        final CompositeComponentManager compositeComponentManager = new CompositeComponentManager(logger, blackDuckService);
        final Map<String, ArtifactMetaData> idToArtifactMetaData = new HashMap<>();

        final List<NotificationView> notificationViews = notificationService.getAllNotifications(startDate, endDate);
        final List<CommonNotificationView> commonNotificationViews = commonNotificationService.getCommonNotifications(notificationViews);
        final NotificationDetailResults notificationDetailResults = commonNotificationService.getNotificationDetailResults(commonNotificationViews);
        final Optional<ProjectVersionWrapper> projectVersionWrapper = projectDataService.getProjectVersion(projectName, projectVersionName);

        ArtifactMetaDataFromNotifications artifactMetaDataFromNotifications = null;
        if (projectVersionWrapper.isPresent()) {
            final ProjectVersionView projectVersionView = projectVersionWrapper.get().getProjectVersionView();
            final List<ProjectVersionView> projectVersionViews = Collections.singletonList(projectVersionView);
            final List<CompositeComponentModel> projectVersionComponentVersionModels = compositeComponentManager.parseNotifications(notificationDetailResults, projectVersionViews);

            for (final CompositeComponentModel projectVersionComponentVersionModel : projectVersionComponentVersionModels) {
                populateMetaDataMap(repoKey, idToArtifactMetaData, blackDuckService, projectVersionComponentVersionModel);
            }

            artifactMetaDataFromNotifications = new ArtifactMetaDataFromNotifications(notificationDetailResults.getLatestNotificationCreatedAtDate(), new ArrayList<>(idToArtifactMetaData.values()));
        }

        return Optional.ofNullable(artifactMetaDataFromNotifications);
    }

    private void populateMetaDataMap(final String repoKey, final Map<String, ArtifactMetaData> idToArtifactMetaData, final BlackDuckService blackDuckService, final CompositeComponentModel compositeComponentModel) {
        compositeComponentModel.originViews.forEach(originView -> {
            final String forge = originView.getOriginName();
            final String originId = originView.getOriginId();
            if (!idToArtifactMetaData.containsKey(key(forge, originId))) {
                final ArtifactMetaData artifactMetaData = new ArtifactMetaData();
                artifactMetaData.repoKey = repoKey;
                artifactMetaData.forge = forge;
                artifactMetaData.originId = originId;
                artifactMetaData.componentVersionLink = compositeComponentModel.componentVersionView.getMeta().getHref();
                artifactMetaData.policyStatus = compositeComponentModel.versionBomComponentView.getPolicyStatus();

                populateVulnerabilityCounts(artifactMetaData, compositeComponentModel.componentVersionView, blackDuckService);

                idToArtifactMetaData.put(key(forge, originId), artifactMetaData);
            }
        });
    }

    private void populateVulnerabilityCounts(final ArtifactMetaData artifactMetaData, final ComponentVersionView componentVersionView, final BlackDuckService blackDuckService) {
        final Optional<String> vulnerabilitiesLink = componentVersionView.getFirstLink(ComponentVersionView.VULNERABILITIES_LINK);

        if (vulnerabilitiesLink.isPresent()) {
            try {
                final List<VulnerabilityV2View> componentVulnerabilities = blackDuckService.getAllResponses(vulnerabilitiesLink.get(), VulnerabilityV2View.class);
                componentVulnerabilities.forEach(vulnerability -> {
                    if ("HIGH".equals(vulnerability.getSeverity())) {
                        artifactMetaData.highSeverityCount++;
                    } else if ("MEDIUM".equals(vulnerability.getSeverity())) {
                        artifactMetaData.mediumSeverityCount++;
                    } else if ("LOW".equals(vulnerability.getSeverity())) {
                        artifactMetaData.lowSeverityCount++;
                    }
                });
            } catch (final IntegrationException e) {
                logger.error(String.format("Can't populate vulnerability counts for %s: %s", componentVersionView.getMeta().getHref(), e.getMessage()));
            }
        }
    }

    private String key(final String forge, final String originId) {
        return forge + ":" + originId;
    }
}
