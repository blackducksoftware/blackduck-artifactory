package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import static java.lang.Boolean.FALSE;

import java.util.Optional;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.ArtifactInspectionService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;

public class InspectionCancelDecider extends CancelDecider {
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
            return CancelDecision.CANCEL_DOWNLOAD(String.format("Missing %s inspection status on an artifact that should be inspected: %s", InspectionStatus.SUCCESS, repoPath.toPath()));
        }

        return CancelDecision.NO_CANCELLATION();
    }
}
