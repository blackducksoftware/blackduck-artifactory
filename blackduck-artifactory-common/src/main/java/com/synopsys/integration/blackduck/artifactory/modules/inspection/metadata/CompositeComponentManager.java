/**
 * hub-artifactory-common
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
import com.synopsys.integration.blackduck.notification.NotificationDetailResults;
import com.synopsys.integration.blackduck.notification.content.detail.NotificationContentDetail;
import com.synopsys.integration.blackduck.service.HubService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;

public class CompositeComponentManager {
    private final IntLogger intLogger;
    private final HubService hubService;
    private Set<String> projectVersionUrisToLookFor;

    public CompositeComponentManager(final IntLogger intLogger, final HubService hubService) {
        this.intLogger = intLogger;
        this.hubService = hubService;
        projectVersionUrisToLookFor = new HashSet<>();
    }

    public List<CompositeComponentModel> parseBom(final ProjectVersionView projectVersionView, final List<VersionBomComponentView> versionBomComponentViews) {
        projectVersionUrisToLookFor = new HashSet<>();
        projectVersionUrisToLookFor.add(projectVersionView._meta.href);

        final List<CompositeComponentModel> compositeComponentModels = versionBomComponentViews
                                                                           .stream()
                                                                           .map(versionBomComponentView -> generateCompositeComponentModel(versionBomComponentView))
                                                                           .collect(Collectors.toList());
        return compositeComponentModels;
    }

    public List<CompositeComponentModel> parseNotifications(final NotificationDetailResults notificationDetailResults, final List<ProjectVersionView> projectVersionViewsToLookFor) {
        projectVersionUrisToLookFor = projectVersionViewsToLookFor
                                          .stream()
                                          .map(it -> it._meta.href)
                                          .collect(Collectors.toSet());

        return notificationDetailResults.getResults()
                   .stream()
                   .map(notificationDetailResult -> notificationDetailResult.getNotificationContentDetails())
                   .map(notificationContentDetails -> generateCompositeComponentModels(notificationContentDetails))
                   .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);
    }

    private List<CompositeComponentModel> generateCompositeComponentModels(final List<NotificationContentDetail> notificationContentDetails) {
        final List<CompositeComponentModel> compositeComponentModels;
        compositeComponentModels = notificationContentDetails
                                       .stream()
                                       .filter(notificationContentDetail -> containsRelevantProjectVersionInformation(notificationContentDetail))
                                       .map(notificationContentDetail -> generateCompositeComponentModel(notificationContentDetail))
                                       .filter(compositeComponentModel -> compositeComponentModel.isPresent())
                                       .map(compositeComponentModel -> compositeComponentModel.get())
                                       .collect(Collectors.toList());
        return compositeComponentModels;
    }

    private boolean containsRelevantProjectVersionInformation(final NotificationContentDetail notificationContentDetail) {
        boolean relevant = false;
        final Optional<UriSingleResponse<ProjectVersionView>> optionalProjectVersionUriResponse = notificationContentDetail.getProjectVersion();

        if (optionalProjectVersionUriResponse.isPresent()) {
            final UriSingleResponse<ProjectVersionView> projectVersionUriResponse = optionalProjectVersionUriResponse.get();
            relevant = projectVersionUrisToLookFor.contains(projectVersionUriResponse.uri);
        }
        return relevant;
    }

    private Optional<CompositeComponentModel> generateCompositeComponentModel(final NotificationContentDetail notificationContentDetail) {
        CompositeComponentModel compositeComponentModel = null;
        try {
            final Optional<UriSingleResponse<ComponentVersionView>> optionalComponentVersionUriResponse = notificationContentDetail.getComponentVersion();
            final Optional<UriSingleResponse<ProjectVersionView>> optionalProjectVersionUriResponse = notificationContentDetail.getProjectVersion();

            if (optionalProjectVersionUriResponse.isPresent()) {
                if (optionalComponentVersionUriResponse.isPresent()) {
                    final UriSingleResponse<ComponentVersionView> componentVersionUriResponse = optionalComponentVersionUriResponse.get();
                    final UriSingleResponse<VersionBomComponentView> versionBomComponentUriResponse = getVersionBomComponentUriResponse(optionalProjectVersionUriResponse.get(), componentVersionUriResponse);

                    compositeComponentModel = createCompositeComponentModel(componentVersionUriResponse, versionBomComponentUriResponse);
                }
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
        final UriSingleResponse<ComponentVersionView> componentVersionViewUriResponse = new UriSingleResponse<>(versionBomComponentView.componentVersion, ComponentVersionView.class);

        try {
            compositeComponentModel = createCompositeComponentModel(componentVersionViewUriResponse, versionBomComponentView);
        } catch (final IntegrationException e) {
            intLogger.error(String.format("Could not create the CompositeComponentModel: %s", e.getMessage()), e);
        }

        return compositeComponentModel;
    }

    private CompositeComponentModel createCompositeComponentModel(final UriSingleResponse<ComponentVersionView> componentVersionUriResponse, final UriSingleResponse<VersionBomComponentView> versionBomComponentUriResponse)
        throws IntegrationException {
        final VersionBomComponentView versionBomComponentView = hubService.getResponse(versionBomComponentUriResponse);

        return createCompositeComponentModel(componentVersionUriResponse, versionBomComponentView);
    }

    private CompositeComponentModel createCompositeComponentModel(final UriSingleResponse<ComponentVersionView> componentVersionUriResponse, final VersionBomComponentView versionBomComponentView) throws IntegrationException {
        final ComponentVersionView componentVersionView = hubService.getResponse(componentVersionUriResponse);
        final List<OriginView> originViews = hubService.getAllResponses(componentVersionView, ComponentVersionView.ORIGINS_LINK_RESPONSE);

        return new CompositeComponentModel(versionBomComponentView, componentVersionView, originViews);
    }

    // not a good practice, but right now, I do not know a better way, short of searching the entire BOM, to match up a BOM component with a component/version
    // ejk - 2018-01-15
    public UriSingleResponse<VersionBomComponentView> getVersionBomComponentUriResponse(final UriSingleResponse<ProjectVersionView> projectVersionUriResponse, final UriSingleResponse<ComponentVersionView> componentVersionUriResponse) {
        final String projectVersionUri = projectVersionUriResponse.uri;
        final String componentVersionUri = componentVersionUriResponse.uri;
        final String apiComponentsLinkPrefix = "/api/components/";
        final int apiComponentsStart = componentVersionUri.indexOf(apiComponentsLinkPrefix) + apiComponentsLinkPrefix.length();
        final String versionBomComponentUri = projectVersionUri + "/components/" + componentVersionUri.substring(apiComponentsStart);
        return new UriSingleResponse<>(versionBomComponentUri, VersionBomComponentView.class);
    }

}
