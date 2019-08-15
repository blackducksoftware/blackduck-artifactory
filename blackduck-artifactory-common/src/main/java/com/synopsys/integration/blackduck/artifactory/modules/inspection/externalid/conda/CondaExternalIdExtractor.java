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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.conda;

import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.NameVersion;

public class CondaExternalIdExtractor {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ExternalIdFactory externalIdFactory;

    public CondaExternalIdExtractor(final ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public Optional<ExternalId> extractExternalId(final SupportedPackageType supportedPackageType, final RepoPath repoPath) {
        ExternalId externalId = null;
        try {
            final NameVersion nameVersion = extractFileNamePieces(repoPath.getName());
            final RepoPath parentRepoPath = repoPath.getParent();
            final String name = nameVersion.getName();

            if (parentRepoPath == null) {
                throw new IntegrationException("Artifact does not have a parent folder. Cannot extract architecture.");
            }
            final String architecture = parentRepoPath.getName().trim();
            final String version = nameVersion.getVersion() + "-" + architecture;

            externalId = externalIdFactory.createNameVersionExternalId(supportedPackageType.getForge(), name, version);
        } catch (final IntegrationException e) {
            logger.info(String.format("Failed to extract name version from filename on at %s", repoPath.getPath()));
            logger.debug(e.getMessage(), e);
        }

        return Optional.ofNullable(externalId);
    }

    private NameVersion extractFileNamePieces(final String fileName) throws IntegrationException {
        final String[] fileNamePieces = fileName.split("-");
        validateLength(fileNamePieces, 3);

        final String[] buildStringExtensionPieces = fileNamePieces[2].split("\\.", 2);
        validateLength(buildStringExtensionPieces, 2);
        final String buildString = buildStringExtensionPieces[0];

        final String componentName = fileNamePieces[0].trim();
        final String componentVersion = fileNamePieces[1].trim() + "-" + buildString;

        return new NameVersion(componentName, componentVersion);
    }

    private void validateLength(final String[] pieces, final int expectedLength) throws IntegrationException {
        if (pieces.length != expectedLength) {
            throw new IntegrationException("Failed to parse conda filename to extract component details.");
        }
    }
}
