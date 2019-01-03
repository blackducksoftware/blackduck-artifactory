/**
 * blackduck-artifactory-common
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.synopsys.integration.blackduck.api.UriSingleResponse;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.OriginView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.notification.NotificationDetailResult;
import com.synopsys.integration.blackduck.notification.NotificationDetailResults;
import com.synopsys.integration.blackduck.notification.content.detail.NotificationContentDetail;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;

public class CompositeComponentManager {
    private final IntLogger intLogger;
    private final BlackDuckService blackDuckService;

    private Set<String> projectVersionUrisToLookFor;

    public CompositeComponentManager(final IntLogger intLogger, final BlackDuckService blackDuckService) {
        this.intLogger = intLogger;
        this.blackDuckService = blackDuckService;
        projectVersionUrisToLookFor = new HashSet<>();
    }

    public List<CompositeComponentModel> parseBom(final ProjectVersionView projectVersionView, final List<VersionBomComponentView> versionBomComponentViews) {
        projectVersionUrisToLookFor = new HashSet<>();
        projectVersionUrisToLookFor.add(projectVersionView.getMeta().getHref());

        return versionBomComponentViews
                   .stream()
                   .map(this::generateCompositeComponentModel)
                   .collect(Collectors.toList());
    }

    public List<CompositeComponentModel> parseNotifications(final NotificationDetailResults notificationDetailResults, final List<ProjectVersionView> projectVersionViewsToLookFor) {
        projectVersionUrisToLookFor = projectVersionViewsToLookFor
                                          .stream()
                                          .map(it -> it.getMeta().getHref())
                                          .collect(Collectors.toSet());

        return notificationDetailResults.getResults()
                   .stream()
                   .map(NotificationDetailResult::getNotificationContentDetails)
                   .map(this::generateCompositeComponentModels)
                   .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);
    }

    private List<CompositeComponentModel> generateCompositeComponentModels(final List<NotificationContentDetail> notificationContentDetails) {
        return notificationContentDetails
                   .stream()
                   .filter(this::containsRelevantProjectVersionInformation)
                   .map(this::generateCompositeComponentModel)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .collect(Collectors.toList());
    }

    private boolean containsRelevantProjectVersionInformation(final NotificationContentDetail notificationContentDetail) {
        boolean relevant = false;
        final Optional<UriSingleResponse<ProjectVersionView>> optionalProjectVersionUriResponse = notificationContentDetail.getProjectVersion();

        if (optionalProjectVersionUriResponse.isPresent()) {
            final UriSingleResponse<ProjectVersionView> projectVersionUriResponse = optionalProjectVersionUriResponse.get();
            relevant = projectVersionUrisToLookFor.contains(projectVersionUriResponse.getUri());
        }
        return relevant;
    }

    private Optional<CompositeComponentModel> generateCompositeComponentModel(final NotificationContentDetail notificationContentDetail) {
        CompositeComponentModel compositeComponentModel = null;
        try {
            final Optional<UriSingleResponse<ComponentVersionView>> optionalComponentVersionUriResponse = notificationContentDetail.getComponentVersion();
            final Optional<UriSingleResponse<VersionBomComponentView>> optionalVersionBomComponentViewUriSingleResponse = notificationContentDetail.getBomComponent();

            if (optionalComponentVersionUriResponse.isPresent() && optionalVersionBomComponentViewUriSingleResponse.isPresent()) {
                final UriSingleResponse<ComponentVersionView> componentVersionViewUriResponse = optionalComponentVersionUriResponse.get();
                final UriSingleResponse<VersionBomComponentView> versionBomComponentUriResponse = optionalVersionBomComponentViewUriSingleResponse.get();

                compositeComponentModel = createCompositeComponentModel(componentVersionViewUriResponse, versionBomComponentUriResponse);
            } else {
                throw new IntegrationException("ProjectVersion data was missing from notification");
            }
        } catch (final IntegrationException e) {
            intLogger.error(String.format("Could not parse notification to get all component details: %s", e.getMessage()), e);
        }

        return Optional.ofNullable(compositeComponentModel);
    }

    private CompositeComponentModel generateCompositeComponentModel(final VersionBomComponentView versionBomComponentView) {
        CompositeComponentModel compositeComponentModel = new CompositeComponentModel();
        final UriSingleResponse<ComponentVersionView> componentVersionViewUriResponse = new UriSingleResponse<>(versionBomComponentView.getComponentVersion(), ComponentVersionView.class);

        try {
            compositeComponentModel = createCompositeComponentModel(componentVersionViewUriResponse, versionBomComponentView);
        } catch (final IntegrationException e) {
            intLogger.error(String.format("Could not create the CompositeComponentModel: %s", e.getMessage()), e);
        }

        return compositeComponentModel;
    }

    private CompositeComponentModel createCompositeComponentModel(final UriSingleResponse<ComponentVersionView> componentVersionUriResponse, final UriSingleResponse<VersionBomComponentView> versionBomComponentUriResponse)
        throws IntegrationException {
        final VersionBomComponentView versionBomComponentView = blackDuckService.getResponse(versionBomComponentUriResponse);

        return createCompositeComponentModel(componentVersionUriResponse, versionBomComponentView);
    }

    private CompositeComponentModel createCompositeComponentModel(final UriSingleResponse<ComponentVersionView> componentVersionUriResponse, final VersionBomComponentView versionBomComponentView) throws IntegrationException {
        final ComponentVersionView componentVersionView = blackDuckService.getResponse(componentVersionUriResponse);
        final List<OriginView> originViews = blackDuckService.getAllResponses(componentVersionView, ComponentVersionView.ORIGINS_LINK_RESPONSE);

        return new CompositeComponentModel(versionBomComponentView, componentVersionView, originViews);
    }
}
