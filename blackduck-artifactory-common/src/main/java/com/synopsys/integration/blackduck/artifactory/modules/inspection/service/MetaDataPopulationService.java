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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.service;

import javax.annotation.Nullable;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.exception.FailedInspectionException;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.Artifact;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.PolicyVulnerabilityAggregate;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model.VulnerabilityAggregate;
import com.synopsys.integration.blackduck.service.ComponentService;
import com.synopsys.integration.blackduck.service.model.ComponentVersionVulnerabilities;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class MetaDataPopulationService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(MetaDataPopulationService.class));

    private final InspectionPropertyService inspectionPropertyService;
    private final ComponentService componentService;

    public MetaDataPopulationService(final InspectionPropertyService inspectionPropertyService, final ComponentService componentService) {
        this.inspectionPropertyService = inspectionPropertyService;
        this.componentService = componentService;
    }

    public void populateExternalIdMetadata(final Artifact artifact) throws FailedInspectionException {
        populateExternalIdMetadata(artifact.getRepoPath(), artifact.getExternalId().orElse(null));
    }

    public void populateExternalIdMetadata(final RepoPath repoPath, @Nullable final ExternalId externalId) throws FailedInspectionException {
        if (externalId == null) {
            logger.debug(String.format("Could not populate artifact with external id metadata. Missing externalId: %s", repoPath));
            throw new FailedInspectionException(repoPath, "Artifactory failed to provide sufficient information to identify the artifact");
        }

        inspectionPropertyService.setExternalIdProperties(repoPath, externalId);
    }

    public void populateBlackDuckMetadata(final RepoPath repoPath, final ComponentVersionView componentVersionView, final VersionBomComponentView versionBomComponentView) throws IntegrationException {
        final PolicySummaryStatusType policySummaryStatusType = versionBomComponentView.getPolicyStatus();
        final ComponentVersionVulnerabilities componentVersionVulnerabilities = componentService.getComponentVersionVulnerabilities(componentVersionView);
        final VulnerabilityAggregate vulnerabilityAggregate = VulnerabilityAggregate.fromVulnerabilityViews(componentVersionVulnerabilities.getVulnerabilities());
        final PolicyVulnerabilityAggregate policyVulnerabilityAggregate = new PolicyVulnerabilityAggregate(vulnerabilityAggregate, policySummaryStatusType, componentVersionView.getHref().orElse(null));
        populateBlackDuckMetadata(repoPath, policyVulnerabilityAggregate);
    }

    private void populateBlackDuckMetadata(final RepoPath repoPath, final PolicyVulnerabilityAggregate policyVulnerabilityAggregate) {
        inspectionPropertyService.setPolicyAndVulnerabilityProperties(repoPath, policyVulnerabilityAggregate);
        inspectionPropertyService.setInspectionStatus(repoPath, InspectionStatus.SUCCESS);
    }

}
