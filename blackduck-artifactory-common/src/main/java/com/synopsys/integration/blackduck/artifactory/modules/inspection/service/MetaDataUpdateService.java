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

    public MetaDataUpdateService(final InspectionPropertyService inspectionPropertyService, final ArtifactNotificationService artifactNotificationService) {
        this.inspectionPropertyService = inspectionPropertyService;
        this.artifactNotificationService = artifactNotificationService;
    }

    public void updateMetadata(final List<RepoPath> repoKeyPaths) {
        final Date now = new Date();
        Date earliestDate = now;
        for (final RepoPath repoKeyPath : repoKeyPaths) {
            final boolean shouldTryUpdate = inspectionPropertyService.assertInspectionStatus(repoKeyPath, InspectionStatus.SUCCESS);

            if (shouldTryUpdate) {
                final Optional<Date> lastUpdateProperty = inspectionPropertyService.getLastUpdate(repoKeyPath);
                final Optional<Date> lastInspectionProperty = inspectionPropertyService.getLastInspection(repoKeyPath);
                final Date dateToCheck;

                if (lastUpdateProperty.isPresent()) {
                    dateToCheck = lastUpdateProperty.get();
                } else if (lastInspectionProperty.isPresent()) {
                    dateToCheck = lastInspectionProperty.get();
                } else {
                    final String message = String.format(
                        "Could not find timestamp property on %s. Black Duck artifactory notifications is likely malformed and requires re-inspection. Run the blackDuckDeleteInspectionProperties rest endpoint to re-inspect all configured repositories or delete the malformed properties manually.",
                        repoKeyPath.toPath());
                    logger.debug(message);
                    inspectionPropertyService.failInspection(repoKeyPath, message);
                    continue;
                }

                if (dateToCheck.before(earliestDate)) {
                    earliestDate = dateToCheck;
                }
            }
        }

        try {
            artifactNotificationService.updateMetadataFromNotifications(repoKeyPaths, earliestDate, now);
        } catch (final IntegrationException e) {
            logger.error(String.format("The Black Duck %s encountered a problem while updating artifact notifications from BlackDuck notifications.", InspectionModule.class.getSimpleName()));
            logger.debug(e.getMessage(), e);
            repoKeyPaths.forEach(repoKeyPath -> inspectionPropertyService.setUpdateStatus(repoKeyPath, UpdateStatus.OUT_OF_DATE));
        }
    }

}
