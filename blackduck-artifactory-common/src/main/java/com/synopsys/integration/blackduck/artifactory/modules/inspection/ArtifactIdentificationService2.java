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
package com.synopsys.integration.blackduck.artifactory.modules.inspection;

import java.util.Optional;

import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;

import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;

public class ArtifactIdentificationService2 {
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ArtifactoryExternalIdFactory artifactoryExternalIdFactory;

    public ArtifactIdentificationService2(final ArtifactoryPAPIService artifactoryPAPIService, final ArtifactoryExternalIdFactory artifactoryExternalIdFactory) {
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.artifactoryExternalIdFactory = artifactoryExternalIdFactory;
    }

    public Optional<ArtifactIdentificationService.IdentifiedArtifact> identifyArtifact(final RepoPath repoPath, final String packageType) {
        final FileLayoutInfo fileLayoutInfo = artifactoryPAPIService.getLayoutInfo(repoPath);
        final org.artifactory.md.Properties properties = artifactoryPAPIService.getProperties(repoPath);
        final Optional<ExternalId> possibleExternalId = artifactoryExternalIdFactory.createExternalId(packageType, fileLayoutInfo, repoPath, properties);

        return possibleExternalId.map(externalId -> new ArtifactIdentificationService.IdentifiedArtifact(repoPath, externalId));
    }
}
