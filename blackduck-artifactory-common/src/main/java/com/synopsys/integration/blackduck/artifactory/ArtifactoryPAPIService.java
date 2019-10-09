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
package com.synopsys.integration.blackduck.artifactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.fs.ItemInfo;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.artifactory.repo.RepositoryConfiguration;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.search.Searches;
import org.slf4j.LoggerFactory;

import com.google.common.collect.SetMultimap;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ArtifactoryPAPIService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final PluginRepoPathFactory pluginRepoPathFactory;
    private final Repositories repositories;
    private final Searches searches;

    public ArtifactoryPAPIService(final PluginRepoPathFactory pluginRepoPathFactory, final Repositories repositories, final Searches searches) {
        this.pluginRepoPathFactory = pluginRepoPathFactory;
        this.repositories = repositories;
        this.searches = searches;
    }

    public Optional<String> getPackageType(final String repoKey) {
        return getRepositoryConfiguration(repoKey)
                   .map(RepositoryConfiguration::getPackageType);
    }

    public Long getArtifactCount(final List<String> repoKeys) {
        return repoKeys.stream()
                   .map(pluginRepoPathFactory::create)
                   .map(repositories::getArtifactsCount)
                   .mapToLong(Long::longValue)
                   .sum();
    }

    public boolean isValidRepository(final String repoKey) {
        if (StringUtils.isBlank(repoKey)) {
            logger.warn("A blank repo key is invalid");
            return false;
        }

        final RepoPath repoPath = pluginRepoPathFactory.create(repoKey);
        final boolean isValid = repositories.exists(repoPath) && getRepositoryConfiguration(repoKey).isPresent();

        if (!isValid) {
            logger.warn(String.format("Repository '%s' was not found or is not a valid repository.", repoKey));
        }

        return isValid;
    }

    // TODO: Only accept one repo at a time
    public List<RepoPath> searchForArtifactsByPatterns(final List<String> repoKeys, final List<String> patterns) {
        final List<RepoPath> repoPaths = new ArrayList<>();

        for (final String pattern : patterns) {
            final List<RepoPath> foundRepoPaths = searches.artifactsByName(pattern, repoKeys.toArray(new String[0]));
            if (!foundRepoPaths.isEmpty()) {
                repoPaths.addAll(foundRepoPaths);
                logger.debug(String.format("Found %d artifacts matching pattern [%s]", foundRepoPaths.size(), pattern));
            } else {
                logger.debug(String.format("No artifacts found that match the pattern pattern [%s]", pattern));
            }
        }

        return repoPaths;
    }

    private Optional<RepositoryConfiguration> getRepositoryConfiguration(final String repoKey) {
        return Optional.ofNullable(repositories.getRepositoryConfiguration(repoKey));
    }

    /*
    Methods below provide low level access to the Artifactory PAPI. No additional verification should be performed.
     */

    public ItemInfo getItemInfo(final RepoPath repoPath) {
        return repositories.getItemInfo(repoPath);
    }

    public FileLayoutInfo getLayoutInfo(final RepoPath repoPath) {
        return repositories.getLayoutInfo(repoPath);
    }

    public ResourceStreamHandle getContent(final RepoPath repoPath) {
        return repositories.getContent(repoPath);
    }

    public Properties getProperties(final RepoPath repoPath) {
        return repositories.getProperties(repoPath);
    }

    public boolean hasProperty(final RepoPath repoPath, final String propertyName) {
        return repositories.hasProperty(repoPath, propertyName);
    }

    public String getProperty(final RepoPath repoPath, final String propertyName) {
        return repositories.getProperty(repoPath, propertyName);
    }

    public void setProperty(final RepoPath repoPath, final String propertyName, final String value) {
        repositories.setProperty(repoPath, propertyName, value);
    }

    public void deleteProperty(final RepoPath repoPath, final String propertyName) {
        repositories.deleteProperty(repoPath, propertyName);
    }

    // TODO: Stop using ArtifactoryPAPIService for this. Use InspectionPropertyService
    public List<RepoPath> itemsByProperties(final SetMultimap<String, String> properties, final String... repoKeys) {
        return searches.itemsByProperties(properties, repoKeys);
    }

    public List<RepoPath> itemsByName(final String artifactByName, final String... repoKeys) {
        return searches.artifactsByName(artifactByName, repoKeys);
    }

    public ResourceStreamHandle getArtifactContent(final RepoPath repoPath) {
        return repositories.getContent(repoPath);
    }
}
