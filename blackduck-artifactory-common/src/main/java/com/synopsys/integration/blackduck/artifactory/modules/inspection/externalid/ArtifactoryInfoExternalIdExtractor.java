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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid;

import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;

import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;

public class ArtifactoryInfoExternalIdExtractor implements ExternalIdExtactor {
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ExternalIdFactory externalIdFactory;

    public ArtifactoryInfoExternalIdExtractor(final ArtifactoryPAPIService artifactoryPAPIService, final ExternalIdFactory externalIdFactory) {
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.externalIdFactory = externalIdFactory;
    }

    @Override
    public Optional<ExternalId> extractExternalId(final SupportedPackageType supportedPackageType, final RepoPath repoPath) {
        ExternalId externalId = extractExternalIdFromProperties(supportedPackageType, repoPath).orElse(null);

        if (externalId == null) {
            externalId = extractExternalIdFromFileLayoutInfo(supportedPackageType, repoPath).orElse(null);
        }

        return Optional.ofNullable(externalId);
    }

    private Optional<ExternalId> extractExternalIdFromProperties(final SupportedPackageType supportedPackageType, final RepoPath repoPath) {
        final Properties properties = artifactoryPAPIService.getProperties(repoPath);
        final Forge forge = supportedPackageType.getForge();
        final String namePropertyKey = supportedPackageType.getArtifactoryNameProperty();
        final String versionPropertyKey = supportedPackageType.getArtifactoryVersionProperty();

        if (namePropertyKey == null || versionPropertyKey == null) {
            return Optional.empty();
        }

        final String name = properties.getFirst(namePropertyKey);
        final String version = properties.getFirst(versionPropertyKey);
        return createNameVersionExternalId(externalIdFactory, forge, name, version);
    }

    private Optional<ExternalId> extractExternalIdFromFileLayoutInfo(final SupportedPackageType supportedPackageType, final RepoPath repoPath) {
        final FileLayoutInfo fileLayoutInfo = artifactoryPAPIService.getLayoutInfo(repoPath);
        final ExternalId externalId;
        if (supportedPackageType.hasNameVersionProperties()) {
            externalId = createNameVersionExternalIdFromFileLayoutInfo(supportedPackageType.getForge(), fileLayoutInfo).orElse(null);
        } else {
            externalId = createMavenExternalId(fileLayoutInfo).orElse(null);
        }

        return Optional.ofNullable(externalId);
    }

    private Optional<ExternalId> createNameVersionExternalIdFromFileLayoutInfo(final Forge forge, final FileLayoutInfo fileLayoutInfo) {
        final String name = fileLayoutInfo.getModule();
        final String version = fileLayoutInfo.getBaseRevision();
        return createNameVersionExternalId(externalIdFactory, forge, name, version);
    }

    private Optional<ExternalId> createMavenExternalId(final FileLayoutInfo fileLayoutInfo) {
        ExternalId externalId = null;
        final String group = fileLayoutInfo.getOrganization();
        final String name = fileLayoutInfo.getModule();
        final String version = fileLayoutInfo.getBaseRevision();
        if (StringUtils.isNoneBlank(group, name, version)) {
            externalId = externalIdFactory.createMavenExternalId(group, name, version);
        }
        return Optional.ofNullable(externalId);
    }

    private Optional<ExternalId> createNameVersionExternalId(final ExternalIdFactory externalIdFactory, final Forge forge, @Nullable final String name, @Nullable final String version) {
        ExternalId externalId = null;
        if (StringUtils.isNoneBlank(name, version)) {
            externalId = externalIdFactory.createNameVersionExternalId(forge, name, version);
        }
        return Optional.ofNullable(externalId);
    }
}
