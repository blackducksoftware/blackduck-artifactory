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
package com.synopsys.integration.blackduck.artifactory.modules.inspection;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.ArtifactMetaData;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.ArtifactMetaDataService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.Artifact;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.VulnerabilityAggregate;
import com.synopsys.integration.blackduck.service.ComponentService;
import com.synopsys.integration.blackduck.service.model.ComponentVersionVulnerabilities;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class MetaDataPopulationService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(MetaDataPopulationService.class));

    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final CacheInspectorService cacheInspectorService;
    private final ArtifactMetaDataService artifactMetaDataService;
    private final ComponentService componentService;

    public MetaDataPopulationService(final ArtifactoryPropertyService artifactoryPropertyService, final CacheInspectorService cacheInspectorService, final ArtifactMetaDataService artifactMetaDataService,
        final ComponentService componentService) {
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.artifactMetaDataService = artifactMetaDataService;
        this.cacheInspectorService = cacheInspectorService;
        this.componentService = componentService;
    }

    public void populateMetadata(final String repoKey) {
        final RepoPath repoKeyPath = RepoPathFactory.create(repoKey);
        final boolean isStatusPending = cacheInspectorService.assertInspectionStatus(repoKeyPath, InspectionStatus.PENDING);

        if (isStatusPending) {
            logger.debug(String.format("Populating metadata in bulk on repoKey: %s", repoKey));
            try {
                final String projectName = cacheInspectorService.getRepoProjectName(repoKey);
                final String projectVersionName = cacheInspectorService.getRepoProjectVersionName(repoKey);

                final List<ArtifactMetaData> artifactMetaDataList = artifactMetaDataService.getArtifactMetadataOfRepository(repoKey, projectName, projectVersionName);
                populateBlackDuckMetadataFromIdMetadata(repoKey, artifactMetaDataList);

                cacheInspectorService.setInspectionStatus(repoKeyPath, InspectionStatus.SUCCESS);
            } catch (final Exception e) {
                logger.error(String.format("The Black Duck %s encountered a problem while populating artifact metadata in repository '%s'", InspectionModule.class.getSimpleName(), repoKey), e);
            }
        }
    }

    public Optional<ExternalId> populateExternalIdMetadata(final Artifact artifact) {
        return populateExternalIdMetadata(artifact.getRepoPath(), artifact.getExternalId().orElse(null));
    }

    public Optional<ExternalId> populateExternalIdMetadata(final RepoPath repoPath, final ExternalId externalId) {
        if (externalId == null) {
            logger.debug(String.format("Could not populate artifact with metadata. Missing externalId: %s", repoPath));
            cacheInspectorService.failInspection(repoPath, "Artifactory failed to provide sufficient information to identify the artifact");
            return Optional.empty();
        }

        final String blackDuckOriginId = externalId.createBlackDuckOriginId();
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID, blackDuckOriginId, logger);
        final String blackduckForge = externalId.forge.getName();
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_FORGE, blackduckForge, logger);

        cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.PENDING);

        return Optional.of(externalId);
    }

    public void populateBlackDuckMetadata(final RepoPath repoPath, final ComponentVersionView componentVersionView, final VersionBomComponentView versionBomComponentView) throws IntegrationException {
        final PolicySummaryStatusType policyStatus = versionBomComponentView.getPolicyStatus();
        final ComponentVersionVulnerabilities componentVersionVulnerabilities = componentService.getComponentVersionVulnerabilities(componentVersionView);
        final VulnerabilityAggregate vulnerabilityAggregate = VulnerabilityAggregate.fromVulnerabilityV2Views(componentVersionVulnerabilities.getVulnerabilities());
        populateBlackDuckMetadata(repoPath, vulnerabilityAggregate, policyStatus, componentVersionView.getHref().orElse(null));
    }

    private void populateBlackDuckMetadata(final RepoPath repoPath, final VulnerabilityAggregate vulnerabilityAggregate, final PolicySummaryStatusType policySummaryStatusType, final String componentVersionUrl) {
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.HIGH_VULNERABILITIES, Integer.toString(vulnerabilityAggregate.getHighSeverityCount()), logger);
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.MEDIUM_VULNERABILITIES, Integer.toString(vulnerabilityAggregate.getMediumSeverityCount()), logger);
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.LOW_VULNERABILITIES, Integer.toString(vulnerabilityAggregate.getLowSeverityCount()), logger);
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, policySummaryStatusType.toString(), logger);
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.COMPONENT_VERSION_URL, Optional.of(componentVersionUrl).orElse("Unavailable"), logger);
        cacheInspectorService.setInspectionStatus(repoPath, InspectionStatus.SUCCESS);
    }

    public void populateBlackDuckMetadataFromIdMetadata(final String repoKey, final List<ArtifactMetaData> artifactMetaDataList) {
        for (final ArtifactMetaData artifactMetaData : artifactMetaDataList) {
            if (StringUtils.isNoneBlank(artifactMetaData.originId, artifactMetaData.forge)) {
                final SetMultimap<String, String> setMultimap = HashMultimap.create();
                setMultimap.put(BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID.getName(), artifactMetaData.originId);
                setMultimap.put(BlackDuckArtifactoryProperty.BLACKDUCK_FORGE.getName(), artifactMetaData.forge);
                final List<RepoPath> artifactsWithOriginId = artifactoryPropertyService.getAllItemsInRepoWithPropertiesAndValues(setMultimap, repoKey);
                for (final RepoPath repoPath : artifactsWithOriginId) {
                    final VulnerabilityAggregate vulnerabilityAggregate = new VulnerabilityAggregate(artifactMetaData.highSeverityCount, artifactMetaData.mediumSeverityCount, artifactMetaData.lowSeverityCount);
                    populateBlackDuckMetadata(repoPath, vulnerabilityAggregate, artifactMetaData.policyStatus, artifactMetaData.componentVersionLink);
                }
            }
        }
    }

}
