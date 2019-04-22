/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications;

import java.util.List;
import java.util.stream.Collectors;

import com.synopsys.integration.blackduck.api.UriSingleResponse;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.OriginView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;

public class CompositeComponentManager {
    private final IntLogger intLogger;
    private final BlackDuckService blackDuckService;

    public CompositeComponentManager(final IntLogger intLogger, final BlackDuckService blackDuckService) {
        this.intLogger = intLogger;
        this.blackDuckService = blackDuckService;
    }

    public List<CompositeComponentModel> parseBom(final List<VersionBomComponentView> versionBomComponentViews) {
        return versionBomComponentViews.stream()
                   .map(this::generateCompositeComponentModel)
                   .collect(Collectors.toList());
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

    private CompositeComponentModel createCompositeComponentModel(final UriSingleResponse<ComponentVersionView> componentVersionUriResponse, final VersionBomComponentView versionBomComponentView) throws IntegrationException {
        final ComponentVersionView componentVersionView = blackDuckService.getResponse(componentVersionUriResponse);
        final List<OriginView> originViews = blackDuckService.getAllResponses(componentVersionView, ComponentVersionView.ORIGINS_LINK_RESPONSE);

        return new CompositeComponentModel(versionBomComponentView, componentVersionView, originViews);
    }
}
