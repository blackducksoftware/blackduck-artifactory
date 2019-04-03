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

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.IdentifiedArtifact;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ArtifactInspectionService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ArtifactIdentificationService2 artifactIdentificationService;
    private final MetaDataPopulationService metaDataPopulationService;
    private final InspectionModuleConfig inspectionModuleConfig;
    private final PackageTypePatternManager packageTypePatternManager;

    public ArtifactInspectionService(final ArtifactoryPAPIService artifactoryPAPIService, final ArtifactIdentificationService2 artifactIdentificationService,
        final MetaDataPopulationService metaDataPopulationService, final InspectionModuleConfig inspectionModuleConfig,
        final PackageTypePatternManager packageTypePatternManager) {
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.artifactIdentificationService = artifactIdentificationService;
        this.metaDataPopulationService = metaDataPopulationService;
        this.inspectionModuleConfig = inspectionModuleConfig;
        this.packageTypePatternManager = packageTypePatternManager;
    }

    public boolean shouldInspectArtifact(final RepoPath repoPath) {
        if (!inspectionModuleConfig.getRepos().contains(repoPath.getRepoKey())) {
            return false;
        }

        final ItemInfo itemInfo = artifactoryPAPIService.getItemInfo(repoPath);
        final Optional<List<String>> patterns = artifactoryPAPIService.getPackageType(repoPath.getRepoKey())
                                                    .map(packageTypePatternManager::getPatterns)
                                                    .filter(Optional::isPresent)
                                                    .map(Optional::get);

        if (!patterns.isPresent() || patterns.get().isEmpty() || itemInfo.isFolder()) {
            return false;
        }

        final File artifact = new File(itemInfo.getName());
        final WildcardFileFilter wildcardFileFilter = new WildcardFileFilter(patterns.get());

        return wildcardFileFilter.accept(artifact);
    }

    public void inspectArtifact(final RepoPath repoPath) {
        final String repoKey = repoPath.getRepoKey();
        final Optional<String> packageType = artifactoryPAPIService.getPackageType(repoKey);
        if (packageType.isPresent()) {
            final Optional<IdentifiedArtifact> identifiedArtifact = artifactIdentificationService.identifyArtifact(repoPath, packageType.get());
            identifiedArtifact.ifPresent(metaDataPopulationService::populateExternalIdMetadata);
        } else {
            logger.debug(String.format("Package type for repo '%s' is not existent", repoKey));
        }
    }
}
