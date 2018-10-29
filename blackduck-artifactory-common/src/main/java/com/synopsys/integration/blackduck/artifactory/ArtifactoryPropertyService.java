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
package com.synopsys.integration.blackduck.artifactory;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.artifactory.search.Searches;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.NameVersion;

public class ArtifactoryPropertyService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final Repositories repositories;
    private final Searches searches;
    private final DateTimeManager dateTimeManager;

    public ArtifactoryPropertyService(final Repositories repositories, final Searches searches, final DateTimeManager dateTimeManager) {
        this.repositories = repositories;
        this.searches = searches;
        this.dateTimeManager = dateTimeManager;
    }

    public Optional<String> getProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        String propertyValue = StringUtils.stripToNull(repositories.getProperty(repoPath, property.getName()));

        // TODO: Remove in 7.X.X
        // If the property isn't found, see if it can be found by its deprecated name
        if (propertyValue == null) {
            propertyValue = StringUtils.stripToNull(repositories.getProperty(repoPath, property.getOldName()));

            if (propertyValue != null) {
                if (property.getName() == null) {
                    logger.warn(String.format("The property %s is deprecated! This should be removed from: %s", property.getOldName(), repoPath.toPath()));
                } else {
                    logger.warn(String.format("Property %s is deprecated! Please use %s: %s", property.getOldName(), property.getName(), repoPath.toPath()));
                }
                logger.warn(String.format("Endpoints exists to assist in updating deprecated properties. Documentation can be found here %s", PluginConstants.PUBLIC_DOCUMENTATION_LINK));
            }
        }

        return Optional.ofNullable(propertyValue);
    }

    public Optional<Date> getDateFromProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        final Optional<String> dateTimeAsString = getProperty(repoPath, property);

        return dateTimeAsString.map(dateTimeManager::getDateFromString);
    }

    public void setProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final String value) {
        repositories.setProperty(repoPath, property.getName(), value);
        logger.debug(String.format("Set property %s to %s on %s", property.getName(), value, repoPath.toPath()));
    }

    public void setPropertyToDate(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final Date date) {
        final String dateTimeAsString = dateTimeManager.getStringFromDate(date);
        setProperty(repoPath, property, dateTimeAsString);
    }

    public void deleteProperty(final RepoPath repoPath, final String propertyName) {
        repositories.deleteProperty(repoPath, propertyName);
        logger.debug(String.format("Removed property %s from %s", propertyName, repoPath.toPath()));
    }

    public void deleteProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        deleteProperty(repoPath, property.getName());
    }

    public void deleteDeprecatedProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        deleteProperty(repoPath, property.getOldName());
    }

    public void deleteAllBlackDuckPropertiesFromRepo(final String repoKey) {
        deleteAllBlackDuckPropertiesFromRepo(repoKey, new HashMap<>());
    }

    public void deleteAllBlackDuckPropertiesFromRepo(final String repoKey, final Map<String, List<String>> params) {
        final List<RepoPath> repoPaths = Arrays.stream(BlackDuckArtifactoryProperty.values())
                                             .map(artifactoryProperty -> getAllItemsInRepoWithAnyProperties(repoKey, artifactoryProperty))
                                             .flatMap(List::stream)
                                             .collect(Collectors.toList());

        repoPaths.forEach(repoPath -> deleteAllBlackDuckPropertiesFromRepoPath(repoPath, params));
    }

    public void deleteAllBlackDuckPropertiesFromRepoPath(final RepoPath repoPath) {
        deleteAllBlackDuckPropertiesFromRepoPath(repoPath, new HashMap<>());
    }

    public void deleteAllBlackDuckPropertiesFromRepoPath(final RepoPath repoPath, final Map<String, List<String>> params) {
        final List<BlackDuckArtifactoryProperty> properties = Arrays.stream(BlackDuckArtifactoryProperty.values())
                                                                  .filter(property -> property.getName() != null)
                                                                  .filter(property -> !isPropertyInParams(property, params))
                                                                  .collect(Collectors.toList());

        properties.forEach(property -> deleteProperty(repoPath, property));
    }

    private boolean isPropertyInParams(final BlackDuckArtifactoryProperty blackDuckArtifactoryProperty, final Map<String, List<String>> params) {
        return params.entrySet().stream()
                   .filter(stringListEntry -> stringListEntry.getKey().equals("properties"))
                   .map(Map.Entry::getValue)
                   .flatMap(List::stream)
                   .anyMatch(paramValue -> paramValue.equals(blackDuckArtifactoryProperty.getName()) || paramValue.equals(blackDuckArtifactoryProperty.getOldName()));
    }

    public List<RepoPath> getAllItemsInRepoWithProperties(final String repoKey, final BlackDuckArtifactoryProperty... properties) {
        final SetMultimap<String, String> setMultimap = Arrays.stream(properties)
                                                            .filter(property -> property.getName() != null)
                                                            .collect(HashMultimap::create, (multimap, property) -> multimap.put(property.getName(), "*"), (self, other) -> self.putAll(other));

        return getAllItemsInRepoWithPropertiesAndValues(setMultimap, repoKey);
    }

    public List<RepoPath> getAllItemsInRepoWithAnyProperties(final String repoKey, final BlackDuckArtifactoryProperty... properties) {
        return Arrays.stream(properties)
                   .map(property -> getAllItemsInRepoWithProperties(repoKey, property))
                   .flatMap(List::stream)
                   .collect(Collectors.toList());
    }

    public List<RepoPath> getAllItemsInRepoWithPropertiesAndValues(final SetMultimap<String, String> setMultimap, final String repoKey) {
        return searches.itemsByProperties(setMultimap, repoKey);
    }

    public Optional<NameVersion> getProjectNameVersion(final RepoPath repoPath) {
        final Optional<String> projectName = getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME);
        final Optional<String> projectVersionName = getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME);
        NameVersion nameVersion = null;

        if (projectName.isPresent() && projectVersionName.isPresent()) {
            nameVersion = new NameVersion(projectName.get(), projectVersionName.get());
        }

        return Optional.ofNullable(nameVersion);
    }

    // TODO: Remove in 7.X.X
    public List<RepoPath> getAllItemsInRepoWithDeprecatedProperties(final String repoKey, final BlackDuckArtifactoryProperty... properties) {
        final SetMultimap<String, String> setMultimap = Arrays.stream(properties)
                                                            .filter(property -> StringUtils.isNotBlank(property.getOldName()))
                                                            .collect(HashMultimap::create, (multimap, property) -> multimap.put(property.getName(), "*"), (self, other) -> self.putAll(other));

        return getAllItemsInRepoWithPropertiesAndValues(setMultimap, repoKey);
    }

    // TODO: Remove in 7.X.X
    public void updateDeprecatedPropertyName(final RepoPath repoPath, final BlackDuckArtifactoryProperty artifactoryProperty) {
        final String deprecatedName = artifactoryProperty.getOldName();

        if (StringUtils.isBlank(deprecatedName) || !repositories.hasProperty(repoPath, deprecatedName)) {
            return; // Nothing to update
        }

        if (artifactoryProperty.getName() == null) {
            deleteDeprecatedProperty(repoPath, artifactoryProperty);
        } else {
            final String propertyValue = repositories.getProperty(repoPath, deprecatedName);
            deleteDeprecatedProperty(repoPath, artifactoryProperty);
            setProperty(repoPath, artifactoryProperty, propertyValue);
            logger.info(String.format("Renamed property %s to %s on %s", artifactoryProperty.getOldName(), artifactoryProperty.getName(), repoPath.toPath()));
        }
    }

    // TODO: Remove in 7.X.X
    public void updateAllBlackDuckPropertiesFromRepoKey(final String repoKey) {
        for (final BlackDuckArtifactoryProperty property : BlackDuckArtifactoryProperty.values()) {
            final List<RepoPath> repoPathsWithProperty = getAllItemsInRepoWithDeprecatedProperties(repoKey, property);
            repoPathsWithProperty.forEach(repoPath -> updateDeprecatedPropertyName(repoPath, property));
        }
    }
}
