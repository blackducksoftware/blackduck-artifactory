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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.ExternalIdProperties;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class BlackDuckPropertiesExternalIdExtractor implements ExternalIdExtactor {
    public static final String INVALID_PROPERTY_MESSAGE_FORMAT = "Property %s does not exist or is invalid.";
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));
    private final InspectionPropertyService inspectionPropertyService;
    private final ExternalIdFactory externalIdFactory;

    public BlackDuckPropertiesExternalIdExtractor(final InspectionPropertyService inspectionPropertyService, final ExternalIdFactory externalIdFactory) {
        this.inspectionPropertyService = inspectionPropertyService;
        this.externalIdFactory = externalIdFactory;
    }

    /**
     * Extracts an ExternalId from the blackduck forge and originId properties
     */
    @Override
    public Optional<ExternalId> extractExternalId(final SupportedPackageType supportedPackageType, final RepoPath repoPath) {
        final ExternalIdProperties externalIdProperties = inspectionPropertyService.getExternalIdProperties(repoPath);

        ExternalId externalId = null;
        final Map<String, Forge> knownForges = Forge.getKnownForges();
        Arrays.stream(SupportedPackageType.values())
            .map(SupportedPackageType::getForge)
            .forEach(artifactoryForge -> knownForges.putIfAbsent(artifactoryForge.getName(), artifactoryForge));

        final Forge forge = externalIdProperties.getForge().map(knownForges::get).orElse(null);
        final String originId = externalIdProperties.getOriginId().orElse(null);

        if (forge == null) {
            logger.debug(String.format(INVALID_PROPERTY_MESSAGE_FORMAT, BlackDuckArtifactoryProperty.BLACKDUCK_FORGE.getName()));
        } else if (originId == null) {
            logger.debug(String.format(INVALID_PROPERTY_MESSAGE_FORMAT, BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID.getName()));
        } else {
            final String[] originIdPieces = originId.split(forge.getSeparator());

            if (originIdPieces.length == 2) {
                externalId = externalIdFactory.createNameVersionExternalId(forge, originIdPieces[0], originIdPieces[1]);
            } else if (originIdPieces.length == 3 && forge.equals(Forge.MAVEN)) {
                externalId = externalIdFactory.createMavenExternalId(originIdPieces[0], originIdPieces[1], originIdPieces[2]);
            } else {
                // In this case, we assume that the name must have the KbSeparator in it, ex: @babel/core/7.4.3
                final int kbSeparatorIndex = originId.lastIndexOf(forge.getSeparator());
                final String name = originId.substring(0, kbSeparatorIndex);
                final String version = originId.substring(kbSeparatorIndex + 1);
                externalId = externalIdFactory.createNameVersionExternalId(forge, name, version);
            }
        }

        return Optional.ofNullable(externalId);
    }
}
