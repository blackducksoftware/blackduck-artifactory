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
import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.modules.UpdateStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModule;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.ArtifactMetaDataFromNotifications;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.ArtifactMetaDataService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class MetaDataUpdateService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(MetaDataUpdateService.class));

    private final ArtifactMetaDataService artifactMetaDataService;
    private final MetaDataPopulationService metadataPopulationService;
    private final InspectionPropertyService inspectionPropertyService;

    public MetaDataUpdateService(final InspectionPropertyService inspectionPropertyService, final ArtifactMetaDataService artifactMetaDataService, final MetaDataPopulationService metadataPopulationService) {
        this.inspectionPropertyService = inspectionPropertyService;
        this.artifactMetaDataService = artifactMetaDataService;
        this.metadataPopulationService = metadataPopulationService;
    }

    public void updateMetadata(final String repoKey) {
        final RepoPath repoKeyPath = RepoPathFactory.create(repoKey);
        final boolean shouldTryUpdate = inspectionPropertyService.assertInspectionStatus(repoKeyPath, InspectionStatus.SUCCESS);

        if (shouldTryUpdate) {
            final Optional<Date> lastUpdateProperty = inspectionPropertyService.getLastUpdate(repoKeyPath);
            final Optional<Date> lastInspectionProperty = inspectionPropertyService.getLastInspection(repoKeyPath);

            try {
                final Date now = new Date();
                final Date dateToCheck;

                if (lastUpdateProperty.isPresent()) {
                    dateToCheck = lastUpdateProperty.get();
                } else if (lastInspectionProperty.isPresent()) {
                    dateToCheck = lastInspectionProperty.get();
                } else {
                    throw new IntegrationException(String.format(
                        "Could not find timestamp property on %s. Black Duck artifactory notifications is likely malformed and requires re-inspection. Run the blackDuckDeleteInspectionProperties rest endpoint to re-inspect all configured repositories or delete the malformed properties manually.",
                        repoKeyPath.toPath()));
                }

                final String projectName = inspectionPropertyService.getRepoProjectName(repoKey);
                final String projectVersionName = inspectionPropertyService.getRepoProjectVersionName(repoKey);

                final Date lastNotificationDate = updateFromHubProjectNotifications(repoKey, projectName, projectVersionName, dateToCheck, now);
                inspectionPropertyService.setUpdateStatus(repoKeyPath, UpdateStatus.UP_TO_DATE);
                inspectionPropertyService.setLastUpdate(repoKeyPath, lastNotificationDate);

                inspectionPropertyService.updateUIUrl(repoKeyPath, projectName, projectVersionName);
            } catch (final IntegrationException e) {
                logger.error(String.format("The Black Duck %s encountered a problem while updating artifact notifications from BlackDuck notifications in repository [%s]", InspectionModule.class.getSimpleName(), repoKey));
                logger.debug(e.getMessage(), e);
                inspectionPropertyService.setUpdateStatus(repoKeyPath, UpdateStatus.OUT_OF_DATE);
            }
        }
    }

    private Date updateFromHubProjectNotifications(final String repoKey, final String projectName, final String projectVersionName, final Date startDate, final Date endDate) throws IntegrationException {
        final Optional<ArtifactMetaDataFromNotifications> artifactMetaDataFromNotificationsOptional = artifactMetaDataService.getArtifactMetadataFromNotifications(repoKey, projectName, projectVersionName, startDate, endDate);

        Optional<Date> lastNotificationDate = Optional.empty();
        if (artifactMetaDataFromNotificationsOptional.isPresent()) {
            final ArtifactMetaDataFromNotifications artifactMetaDataFromNotifications = artifactMetaDataFromNotificationsOptional.get();
            metadataPopulationService.populateBlackDuckMetadataFromIdMetadata(repoKey, artifactMetaDataFromNotifications.getArtifactMetaData());
            lastNotificationDate = artifactMetaDataFromNotifications.getLastNotificationDate();
        } else {
            logger.debug("No artifact notifications from notifications");
        }

        // We don't want to miss notifications, so if something goes wrong we will err on the side of caution.
        return lastNotificationDate.orElse(startDate);
    }

}
