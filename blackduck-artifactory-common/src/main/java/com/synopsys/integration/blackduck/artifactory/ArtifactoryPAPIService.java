/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ArtifactoryPAPIService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final PluginRepoPathFactory pluginRepoPathFactory;
    private final Repositories repositories;
    private final Searches searches;

    public ArtifactoryPAPIService(PluginRepoPathFactory pluginRepoPathFactory, Repositories repositories, Searches searches) {
        this.pluginRepoPathFactory = pluginRepoPathFactory;
        this.repositories = repositories;
        this.searches = searches;
    }

    public Optional<String> getPackageType(String repoKey) {
        return getRepositoryConfiguration(repoKey)
                   .map(RepositoryConfiguration::getPackageType);
    }

    public long getArtifactCount(List<String> repoKeys) {
        return repoKeys.stream()
                   .map(pluginRepoPathFactory::create)
                   .mapToLong(repositories::getArtifactsCount)
                   .sum();
    }

    public boolean isValidRepository(String repoKey) {
        if (StringUtils.isBlank(repoKey)) {
            logger.warn("A blank repo key is invalid");
            return false;
        }

        RepoPath repoPath = pluginRepoPathFactory.create(repoKey);
        boolean isValid = repositories.exists(repoPath) && getRepositoryConfiguration(repoKey).isPresent();

        if (!isValid) {
            logger.warn(String.format("Repository '%s' was not found or is not a valid repository.", repoKey));
        }

        return isValid;
    }

    public List<RepoPath> searchForArtifactsByPatterns(String repoKey, List<String> patterns) {
        List<RepoPath> repoPaths = new ArrayList<>();

        for (String pattern : patterns) {
            List<RepoPath> foundRepoPaths = searches.artifactsByName(pattern, repoKey);
            if (!foundRepoPaths.isEmpty()) {
                repoPaths.addAll(foundRepoPaths);
                logger.debug(String.format("Found %d artifacts matching pattern [%s]", foundRepoPaths.size(), pattern));
            } else {
                logger.debug(String.format("No artifacts found that match the pattern pattern [%s]", pattern));
            }
        }

        return repoPaths;
    }

    private Optional<RepositoryConfiguration> getRepositoryConfiguration(String repoKey) {
        return Optional.ofNullable(repositories.getRepositoryConfiguration(repoKey));
    }

    /*
    Methods below provide low level access to the Artifactory PAPI. No additional verification should be performed.
     */

    public ItemInfo getItemInfo(RepoPath repoPath) {
        return repositories.getItemInfo(repoPath);
    }

    public FileLayoutInfo getLayoutInfo(RepoPath repoPath) {
        return repositories.getLayoutInfo(repoPath);
    }

    public ResourceStreamHandle getContent(RepoPath repoPath) {
        return repositories.getContent(repoPath);
    }

    public Properties getProperties(RepoPath repoPath) {
        return repositories.getProperties(repoPath);
    }

    public boolean hasProperty(RepoPath repoPath, String propertyName) {
        return repositories.hasProperty(repoPath, propertyName);
    }

    public Optional<String> getProperty(RepoPath repoPath, String propertyName) {
        return Optional.ofNullable(repositories.getProperty(repoPath, propertyName));
    }

    public void setProperty(RepoPath repoPath, String propertyName, String value) {
        repositories.setProperty(repoPath, propertyName, value);
    }

    public void deleteProperty(RepoPath repoPath, String propertyName) {
        repositories.deleteProperty(repoPath, propertyName);
    }

    public List<RepoPath> itemsByProperties(Map<String, String> properties, String[] repoKeys) {
        SetMultimap<String, String> setMultimap = HashMultimap.create();
        properties.forEach(setMultimap::put);
        return searches.itemsByProperties(setMultimap, repoKeys);
    }

    public List<RepoPath> itemsByName(String artifactByName, String... repoKeys) {
        return searches.artifactsByName(artifactByName, repoKeys);
    }

    public ResourceStreamHandle getArtifactContent(RepoPath repoPath) {
        return repositories.getContent(repoPath);
    }
}
