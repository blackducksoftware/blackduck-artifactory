/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import static java.lang.Boolean.FALSE;

import java.util.Optional;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.ArtifactInspectionService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;

public class InspectionCancelDecider implements CancelDecider {
    private final InspectionModuleConfig inspectionModuleConfig;
    private final InspectionPropertyService inspectionPropertyService;
    private final ArtifactInspectionService artifactInspectionService;

    public InspectionCancelDecider(InspectionModuleConfig inspectionModuleConfig, InspectionPropertyService inspectionPropertyService, ArtifactInspectionService artifactInspectionService) {
        this.inspectionModuleConfig = inspectionModuleConfig;
        this.inspectionPropertyService = inspectionPropertyService;
        this.artifactInspectionService = artifactInspectionService;
    }

    @Override
    public CancelDecision getCancelDecision(RepoPath repoPath) {
        if (FALSE.equals(inspectionModuleConfig.isMetadataBlockEnabled())) {
            return CancelDecision.NO_CANCELLATION();
        }

        Optional<InspectionStatus> inspectionStatus = inspectionPropertyService.getInspectionStatus(repoPath);
        if (inspectionStatus.isPresent() && inspectionStatus.get().equals(InspectionStatus.PENDING)) {
            return CancelDecision.NO_CANCELLATION();
        }

        if (artifactInspectionService.shouldInspectArtifact(repoPath)) {
            return CancelDecision.CANCEL_DOWNLOAD(String.format("Missing %s inspection status on an artifact that should be inspected.", InspectionStatus.SUCCESS));
        }

        return CancelDecision.NO_CANCELLATION();
    }
}
