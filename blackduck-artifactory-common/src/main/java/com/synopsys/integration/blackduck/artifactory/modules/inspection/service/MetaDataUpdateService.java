/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.modules.UpdateStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModule;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.ArtifactNotificationService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class MetaDataUpdateService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(MetaDataUpdateService.class));

    private final InspectionPropertyService inspectionPropertyService;
    private final ArtifactNotificationService artifactNotificationService;

    public MetaDataUpdateService(InspectionPropertyService inspectionPropertyService, ArtifactNotificationService artifactNotificationService) {
        this.inspectionPropertyService = inspectionPropertyService;
        this.artifactNotificationService = artifactNotificationService;
    }

    public void updateMetadata(List<RepoPath> repoKeyPaths) {
        Date now = new Date();
        Date earliestDate = now;
        List<RepoPath> repoKeyPathsToUpdate = new ArrayList<>();
        for (RepoPath repoKeyPath : repoKeyPaths) {
            boolean shouldTryUpdate = inspectionPropertyService.assertInspectionStatus(repoKeyPath, InspectionStatus.SUCCESS) || inspectionPropertyService.assertInspectionStatus(repoKeyPath, InspectionStatus.PENDING);

            if (shouldTryUpdate) {
                Optional<Date> lastUpdateProperty = inspectionPropertyService.getLastUpdate(repoKeyPath);
                Optional<Date> lastInspectionProperty = inspectionPropertyService.getLastInspection(repoKeyPath);
                Date dateToCheck;

                if (lastUpdateProperty.isPresent()) {
                    dateToCheck = lastUpdateProperty.get();
                } else if (lastInspectionProperty.isPresent()) {
                    dateToCheck = lastInspectionProperty.get();
                } else {
                    String message = String.format(
                        "Could not find timestamp property on %s. Black Duck artifactory notifications is likely malformed and requires re-inspection. Run the blackDuckDeleteInspectionProperties rest endpoint to re-inspect all configured repositories or delete the malformed properties manually.",
                        repoKeyPath.toPath());
                    logger.debug(message);
                    inspectionPropertyService.failInspection(repoKeyPath, message);
                    continue;
                }

                if (dateToCheck.before(earliestDate)) {
                    earliestDate = dateToCheck;
                }

                repoKeyPathsToUpdate.add(repoKeyPath);
            }
        }

        try {
            artifactNotificationService.updateMetadataFromNotifications(repoKeyPathsToUpdate, earliestDate, now);

        } catch (IntegrationException e) {
            logger.error(String.format("The Black Duck %s encountered a problem while updating artifact notifications from BlackDuck notifications.", InspectionModule.class.getSimpleName()));
            logger.debug(e.getMessage(), e);
            repoKeyPaths.forEach(repoKeyPath -> inspectionPropertyService.setUpdateStatus(repoKeyPath, UpdateStatus.OUT_OF_DATE));
        }
    }

}
