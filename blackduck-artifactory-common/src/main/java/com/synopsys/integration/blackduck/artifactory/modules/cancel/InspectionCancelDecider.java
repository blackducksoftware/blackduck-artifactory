/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import static java.lang.Boolean.FALSE;

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

        boolean hasSuccess = inspectionPropertyService.getInspectionStatus(repoPath)
                                 .filter(InspectionStatus.SUCCESS::equals)
                                 .isPresent();
        if (!hasSuccess && artifactInspectionService.shouldInspectArtifact(repoPath)) {
            return CancelDecision.CANCEL_DOWNLOAD(String.format("Missing %s inspection status on an artifact that should be inspected.", InspectionStatus.SUCCESS));
        }

        return CancelDecision.NO_CANCELLATION();
    }
}
