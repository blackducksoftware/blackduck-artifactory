/**
 * hub-artifactory-common
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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

import java.util.Date;
import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.ArtifactMetaDataFromNotifications;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.metadata.ArtifactMetaDataService;
import com.synopsys.integration.exception.IntegrationException;

public class MetaDataUpdateService {
    private final Logger logger = LoggerFactory.getLogger(MetaDataUpdateService.class);

    private final ArtifactMetaDataService artifactMetaDataService;
    private final MetaDataPopulationService metadataPopulationService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final CacheInspectorService cacheInspectorService;

    public MetaDataUpdateService(final ArtifactoryPropertyService artifactoryPropertyService, final CacheInspectorService cacheInspectorService, final ArtifactMetaDataService artifactMetaDataService,
        final MetaDataPopulationService metadataPopulationService) {
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.cacheInspectorService = cacheInspectorService;
        this.artifactMetaDataService = artifactMetaDataService;
        this.metadataPopulationService = metadataPopulationService;
    }

    public void updateMetadata(final String repoKey) {
        final RepoPath repoKeyPath = RepoPathFactory.create(repoKey);
        final Optional<InspectionStatus> inspectionStatus = cacheInspectorService.getInspectionStatus(repoKeyPath);

        if (inspectionStatus.isPresent() && inspectionStatus.get().equals(InspectionStatus.SUCCESS)) {
            final Optional<Date> lastUpdateProperty = artifactoryPropertyService.getDateFromProperty(repoKeyPath, BlackDuckArtifactoryProperty.LAST_UPDATE);
            final Optional<Date> lastInspectionProperty = artifactoryPropertyService.getDateFromProperty(repoKeyPath, BlackDuckArtifactoryProperty.LAST_INSPECTION);

            try {
                final Date now = new Date();
                final Date dateToCheck;

                if (lastUpdateProperty.isPresent()) {
                    dateToCheck = lastUpdateProperty.get();
                } else if (lastInspectionProperty.isPresent()) {
                    dateToCheck = lastInspectionProperty.get();
                } else {
                    throw new IntegrationException(String.format(
                        "Could not find timestamp property on %s. Black Duck artifactory metadata is likely malformed and requires re-inspection. Run the blackDuckDeleteInspectionProperties rest endpoint to re-inspect all configured repositories or delete the malformed properties manually.",
                        repoKeyPath.toPath()));
                }

                final String projectName = cacheInspectorService.getRepoProjectName(repoKey);
                final String projectVersionName = cacheInspectorService.getRepoProjectVersionName(repoKey);

                final Date lastNotificationDate = updateFromHubProjectNotifications(repoKey, projectName, projectVersionName, dateToCheck, now);
                artifactoryPropertyService.setProperty(repoKeyPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, UpdateStatus.UP_TO_DATE.toString());
                artifactoryPropertyService.setPropertyToDate(repoKeyPath, BlackDuckArtifactoryProperty.LAST_UPDATE, lastNotificationDate);
            } catch (final IntegrationException e) {
                logger.error(String.format("The Black Duck %s encountered a problem while updating artifact metadata from BlackDuck notifications in repository [%s]:", InspectionModule.class.getSimpleName(), repoKey));
                logger.debug(e.getMessage(), e);
                artifactoryPropertyService.setProperty(repoKeyPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, UpdateStatus.OUT_OF_DATE.toString());
            }
        }
    }

    private Date updateFromHubProjectNotifications(final String repoKey, final String projectName, final String projectVersionName, final Date startDate, final Date endDate) throws IntegrationException {
        final ArtifactMetaDataFromNotifications artifactMetaDataFromNotifications = artifactMetaDataService.getArtifactMetadataFromNotifications(repoKey, projectName, projectVersionName, startDate, endDate);
        metadataPopulationService.populateBlackDuckMetadataFromIdMetadata(repoKey, artifactMetaDataFromNotifications.getArtifactMetaData());

        final Optional<Date> lastNotificationDate = artifactMetaDataFromNotifications.getLastNotificationDate();

        // We don't want to miss notifications, so if something goes wrong we will err on the side of caution.
        return lastNotificationDate.orElse(startDate);
    }

}
