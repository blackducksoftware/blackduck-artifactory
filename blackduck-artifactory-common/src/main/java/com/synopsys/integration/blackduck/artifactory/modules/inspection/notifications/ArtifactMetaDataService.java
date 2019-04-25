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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.UriSingleResponse;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.OriginView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.api.generated.view.VulnerabilityView;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.ArtifactMetaData;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.service.BlackDuckService;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ArtifactMetaDataService {
    private static final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(ArtifactMetaDataService.class));

    private final BlackDuckService blackDuckService;
    private final ProjectService projectService;

    public static ArtifactMetaDataService createDefault(final BlackDuckServerConfig blackDuckServerConfig) {
        final BlackDuckServicesFactory blackDuckServicesFactory = blackDuckServerConfig.createBlackDuckServicesFactory(logger);
        final BlackDuckService blackDuckService = blackDuckServicesFactory.createBlackDuckService();
        final ProjectService projectService = blackDuckServicesFactory.createProjectService();
        return new ArtifactMetaDataService(blackDuckService, projectService);
    }

    public ArtifactMetaDataService(final BlackDuckService blackDuckService, final ProjectService projectService) {
        this.blackDuckService = blackDuckService;
        this.projectService = projectService;
    }

    public List<ArtifactMetaData> getArtifactMetadataOfRepository(final String repoKey, final String projectName, final String projectVersionName) throws IntegrationException {
        final Map<String, ArtifactMetaData> idToArtifactMetaData = new HashMap<>();

        final Optional<ProjectVersionWrapper> projectVersionWrapper = projectService.getProjectVersion(projectName, projectVersionName);

        if (projectVersionWrapper.isPresent()) {
            final ProjectVersionView projectVersionView = projectVersionWrapper.get().getProjectVersionView();
            final List<VersionBomComponentView> versionBomComponentViews = blackDuckService.getAllResponses(projectVersionView, ProjectVersionView.COMPONENTS_LINK_RESPONSE);
            final List<CompositeComponentModel> projectVersionComponentVersionModels = versionBomComponentViews.stream()
                                                                                           .map(this::generateCompositeComponentModel)
                                                                                           .collect(Collectors.toList());

            for (final CompositeComponentModel projectVersionComponentVersionModel : projectVersionComponentVersionModels) {
                populateMetaDataMap(repoKey, idToArtifactMetaData, projectVersionComponentVersionModel);
            }
        } else {
            logger.debug(String.format("Failed to find project '%s' and version '%s' on repo '%s'", projectName, projectVersionName, repoKey));
        }

        return new ArrayList<>(idToArtifactMetaData.values());
    }

    private CompositeComponentModel generateCompositeComponentModel(final VersionBomComponentView versionBomComponentView) {
        CompositeComponentModel compositeComponentModel = new CompositeComponentModel();
        final UriSingleResponse<ComponentVersionView> componentVersionViewUriResponse = new UriSingleResponse<>(versionBomComponentView.getComponentVersion(), ComponentVersionView.class);

        try {
            final ComponentVersionView componentVersionView = blackDuckService.getResponse(componentVersionViewUriResponse);
            final List<OriginView> originViews = blackDuckService.getAllResponses(componentVersionView, ComponentVersionView.ORIGINS_LINK_RESPONSE);
            compositeComponentModel = new CompositeComponentModel(versionBomComponentView, componentVersionView, originViews);
        } catch (final IntegrationException e) {
            logger.error(String.format("Could not create the CompositeComponentModel: %s", e.getMessage()), e);
        }

        return compositeComponentModel;
    }

    private void populateMetaDataMap(final String repoKey, final Map<String, ArtifactMetaData> idToArtifactMetaData, final CompositeComponentModel compositeComponentModel) {
        final ComponentVersionView componentVersionView = compositeComponentModel.componentVersionView;
        final Optional<String> vulnerabilitiesLink = componentVersionView.getFirstLink(ComponentVersionView.VULNERABILITIES_LINK);

        VulnerabilityAggregate vulnerabilityAggregate = null;
        if (vulnerabilitiesLink.isPresent()) {
            try {
                final List<VulnerabilityView> vulnerabilityViews = blackDuckService.getAllResponses(vulnerabilitiesLink.get(), VulnerabilityView.class);
                vulnerabilityAggregate = VulnerabilityAggregate.fromVulnerabilityViews(vulnerabilityViews);
            } catch (final IntegrationException e) {
                logger.error(String.format("Can't populate vulnerability counts for %s: %s", componentVersionView.getMeta().getHref(), e.getMessage()));
            }
        }

        for (final OriginView originView : compositeComponentModel.originViews) {
            final String forge = originView.getOriginName();
            final String originId = originView.getOriginId();
            if (!idToArtifactMetaData.containsKey(key(forge, originId))) {
                final ArtifactMetaData artifactMetaData = new ArtifactMetaData();
                artifactMetaData.repoKey = repoKey;
                artifactMetaData.forge = forge;
                artifactMetaData.originId = originId;
                artifactMetaData.componentVersionLink = componentVersionView.getMeta().getHref();
                artifactMetaData.policyStatus = compositeComponentModel.versionBomComponentView.getPolicyStatus();
                artifactMetaData.vulnerabilityAggregate = vulnerabilityAggregate;

                idToArtifactMetaData.put(key(forge, originId), artifactMetaData);
            }
        }
    }

    private String key(final String forge, final String originId) {
        return forge + ":" + originId;
    }
}
