/**
 * blackduck-artifactory-common
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.util.NameVersion;

public class ArtifactoryPropertyService {
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final DateTimeManager dateTimeManager;

    public ArtifactoryPropertyService(final ArtifactoryPAPIService artifactoryPAPIService, final DateTimeManager dateTimeManager) {
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.dateTimeManager = dateTimeManager;
    }

    public boolean hasProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        final boolean currentNameExists = artifactoryPAPIService.hasProperty(repoPath, property.getName());
        final boolean oldNameExists = StringUtils.isNotBlank(property.getOldName()) && artifactoryPAPIService.hasProperty(repoPath, property.getOldName());

        return currentNameExists || oldNameExists;
    }

    public Optional<String> getProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final IntLogger logger) {
        Optional<String> propertyValue = getProperty(repoPath, property.getName());

        // If the property isn't found, see if it can be found by its deprecated name
        if (!propertyValue.isPresent()) {
            propertyValue = getDeprecatedProperty(repoPath, property, logger);
        }

        return propertyValue;
    }

    private Optional<String> getProperty(final RepoPath repoPath, final String propertyKey) {
        final String propertyValue = StringUtils.stripToNull(artifactoryPAPIService.getProperty(repoPath, propertyKey));

        return Optional.ofNullable(propertyValue);
    }

    public Optional<Date> getDateFromProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final IntLogger logger) {
        final Optional<String> dateTimeAsString = getProperty(repoPath, property, logger);

        return dateTimeAsString.map(dateTimeManager::getDateFromString);
    }

    public void setProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final String value, final IntLogger logger) {
        artifactoryPAPIService.setProperty(repoPath, property.getName(), value);
        logger.debug(String.format("Set property %s to %s on %s", property.getName(), value, repoPath.toPath()));
    }

    public void setPropertyToDate(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final Date date, final IntLogger logger) {
        final String dateTimeAsString = dateTimeManager.getStringFromDate(date);
        setProperty(repoPath, property, dateTimeAsString, logger);
    }

    public void deleteProperty(final RepoPath repoPath, final String propertyName, final IntLogger logger) {
        if (artifactoryPAPIService.hasProperty(repoPath, propertyName)) {
            artifactoryPAPIService.deleteProperty(repoPath, propertyName);
            logger.debug(String.format("Removed property %s from %s", propertyName, repoPath.toPath()));
        }
    }

    public void deleteProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final IntLogger logger) {
        deleteProperty(repoPath, property.getName(), logger);
    }

    public void deleteAllBlackDuckPropertiesFromRepo(final String repoKey, final Map<String, List<String>> params, final IntLogger logger) {
        final List<RepoPath> repoPaths = Arrays.stream(BlackDuckArtifactoryProperty.values())
                                             .map(artifactoryProperty -> getAllItemsInRepoWithAnyProperties(repoKey, artifactoryProperty))
                                             .flatMap(List::stream)
                                             .collect(Collectors.toList());

        repoPaths.forEach(repoPath -> deleteAllBlackDuckPropertiesFromRepoPath(repoPath, params, logger));
    }

    public void deleteAllBlackDuckPropertiesFromRepoPath(final RepoPath repoPath, final Map<String, List<String>> params, final IntLogger logger) {
        final List<BlackDuckArtifactoryProperty> properties = Arrays.stream(BlackDuckArtifactoryProperty.values())
                                                                  .filter(property -> property.getName() != null)
                                                                  .filter(property -> !isPropertyInParams(property, params))
                                                                  .collect(Collectors.toList());

        properties.forEach(property -> deleteProperty(repoPath, property, logger));
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
        return artifactoryPAPIService.itemsByProperties(setMultimap, repoKey);
    }

    public Optional<NameVersion> getProjectNameVersion(final RepoPath repoPath, final IntLogger logger) {
        final Optional<String> projectName = getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME, logger);
        final Optional<String> projectVersionName = getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME, logger);
        NameVersion nameVersion = null;

        if (projectName.isPresent() && projectVersionName.isPresent()) {
            nameVersion = new NameVersion(projectName.get(), projectVersionName.get());
        }

        return Optional.ofNullable(nameVersion);
    }

    // TODO: Remove in 7.X.X
    public void updateAllBlackDuckPropertiesFromRepoKey(final String repoKey, final IntLogger logger) {
        for (final BlackDuckArtifactoryProperty property : BlackDuckArtifactoryProperty.values()) {
            if (StringUtils.isBlank(property.getOldName())) {
                continue;
            }

            final SetMultimap<String, String> setMultimap = HashMultimap.create();
            setMultimap.put(property.getOldName(), "*");
            logger.debug(String.format("setMultimap for repoKey: %s", repoKey));
            setMultimap.entries().forEach(entry -> logger.debug(String.format("%s: %s", entry.getKey(), entry.getValue())));

            final Set<RepoPath> repoPathsWithProperty = new HashSet<>(getAllItemsInRepoWithPropertiesAndValues(setMultimap, repoKey));
            repoPathsWithProperty.forEach(repoPath -> updateDeprecatedPropertyName(repoPath, property, logger));
        }
    }

    // TODO: Remove in 7.X.X
    private void updateDeprecatedPropertyName(final RepoPath repoPath, final BlackDuckArtifactoryProperty artifactoryProperty, final IntLogger logger) {
        final String oldName = artifactoryProperty.getOldName();
        final Optional<String> propertyValue = getProperty(repoPath, oldName);
        if (!propertyValue.isPresent()) {
            // Nothing to update
            return;
        }

        deleteProperty(repoPath, oldName, logger);

        if (StringUtils.isNotBlank(artifactoryProperty.getName())) {
            setProperty(repoPath, artifactoryProperty, propertyValue.get(), logger);
            logger.info(String.format("Renamed property %s to %s on %s", oldName, artifactoryProperty.getName(), repoPath.toPath()));
        }
    }

    // TODO: Remove in 7.X.X
    private Optional<String> getDeprecatedProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final IntLogger logger) {
        final Optional<String> propertyValue = getProperty(repoPath, property.getOldName());

        if (propertyValue.isPresent()) {
            if (property.getName() == null) {
                logger.warn(String.format("The property %s is deprecated! This should be removed from: %s", property.getOldName(), repoPath.toPath()));
            } else {
                logger.warn(String.format("Property %s is deprecated! Please use %s: %s", property.getOldName(), property.getName(), repoPath.toPath()));
            }
            logger.warn(String.format("Endpoints exists to assist in updating deprecated properties. Documentation can be found here %s", PluginConstants.PUBLIC_DOCUMENTATION_LINK));
        }

        return propertyValue;
    }
}
