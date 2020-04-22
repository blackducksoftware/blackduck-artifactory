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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.conda;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
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
    private static final List<String> SUPPORTED_FILE_EXTENSIONS;

    static {
        SUPPORTED_FILE_EXTENSIONS = new ArrayList<>();
        SUPPORTED_FILE_EXTENSIONS.add(".tar.bz2");
        SUPPORTED_FILE_EXTENSIONS.add(".conda");
    }

    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));
    private final Pattern pattern = Pattern.compile("(.*)-(.*)-([^-|\\s]*)");

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
        final Matcher matcher = pattern.matcher(fileName);
        if (!matcher.matches() || matcher.group(1).isEmpty() || matcher.group(2).isEmpty() || matcher.group(3).isEmpty()) {
            throw new IntegrationException("Failed to parse conda filename to extract component details.");
        }

        final String buildStringExtension = matcher.group(3);
        String buildString = null;
        for (final String supportedFileExtension : SUPPORTED_FILE_EXTENSIONS) {
            if (buildStringExtension.endsWith(supportedFileExtension)) {
                buildString = StringUtils.removeEnd(buildStringExtension, supportedFileExtension);
                break;
            }
        }
        if (buildString == null) {
            final String supportedExtensionsMessage = "Supported conda file extensions are " + String.join(", ", SUPPORTED_FILE_EXTENSIONS);
            throw new IntegrationException(String.format("Failed to parse conda filename to extract component details. Likely unsupported file extension. %s", supportedExtensionsMessage));
        }

        final String componentName = matcher.group(1).trim();
        final String componentVersion = matcher.group(2).trim() + "-" + buildString;

        return new NameVersion(componentName, componentVersion);
    }
}
