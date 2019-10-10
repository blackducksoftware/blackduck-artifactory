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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        return artifactoryPAPIService.hasProperty(repoPath, property.getName());
    }

    public Optional<String> getProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        return getProperty(repoPath, property.getName());
    }

    private Optional<String> getProperty(final RepoPath repoPath, final String propertyKey) {
        final String propertyValue = StringUtils.stripToNull(artifactoryPAPIService.getProperty(repoPath, propertyKey));

        return Optional.ofNullable(propertyValue);
    }

    public Optional<Integer> getPropertyAsInteger(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        return getProperty(repoPath, property)
                   .map(Integer::new);
    }

    public Optional<Date> getDateFromProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        final Optional<String> dateTimeAsString = getProperty(repoPath, property);

        return dateTimeAsString.map(dateTimeManager::getDateFromString);
    }

    public void setProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final String value, final IntLogger logger) {
        setProperty(repoPath, property.getName(), value, logger);
    }

    private void setProperty(final RepoPath repoPath, final String property, final String value, final IntLogger logger) {
        artifactoryPAPIService.setProperty(repoPath, property, value);
        logger.debug(String.format("Set property %s to %s on %s", property, value, repoPath.toPath()));
    }

    public void setPropertyFromDate(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final Date date, final IntLogger logger) {
        final String dateTimeAsString = dateTimeManager.getStringFromDate(date);
        setProperty(repoPath, property, dateTimeAsString, logger);

        final Optional<String> dateTimeAsStringConverted = dateTimeManager.geStringFromDateWithTimeZone(date);
        dateTimeAsStringConverted.ifPresent(converted -> setProperty(repoPath, property.getTimeName(), converted, logger));
    }

    public void deleteProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final IntLogger logger) {
        deleteProperty(repoPath, property.getName(), logger);
        deleteProperty(repoPath, property.getTimeName(), logger);
    }

    private void deleteProperty(final RepoPath repoPath, final String propertyName, final IntLogger logger) {
        if (artifactoryPAPIService.hasProperty(repoPath, propertyName)) {
            artifactoryPAPIService.deleteProperty(repoPath, propertyName);
            logger.debug(String.format("Removed property %s from %s", propertyName, repoPath.toPath()));
        }
    }

    public void deleteAllBlackDuckPropertiesFromRepo(final String repoKey, final Map<String, List<String>> params, final IntLogger logger) {
        final List<RepoPath> repoPaths = Arrays.stream(BlackDuckArtifactoryProperty.values())
                                             .map(artifactoryProperty -> getItemsContainingAnyProperties(repoKey, artifactoryProperty))
                                             .flatMap(List::stream)
                                             .collect(Collectors.toList());

        repoPaths.forEach(repoPath -> deleteAllBlackDuckPropertiesFromRepoPath(repoPath, params, logger));
    }

    private List<RepoPath> getItemsContainingAnyProperties(final String repoKey, final BlackDuckArtifactoryProperty... properties) {
        return Arrays.stream(properties)
                   .map(property -> getItemsContainingProperties(repoKey, property))
                   .flatMap(List::stream)
                   .collect(Collectors.toList());
    }

    public void deleteAllBlackDuckPropertiesFromRepoPath(final RepoPath repoPath, final Map<String, List<String>> params, final IntLogger logger) {
        final List<BlackDuckArtifactoryProperty> properties = Arrays.stream(BlackDuckArtifactoryProperty.values())
                                                                  .filter(property -> !isPropertyInParams(property, params))
                                                                  .collect(Collectors.toList());

        properties.forEach(property -> deleteProperty(repoPath, property, logger));
    }

    private boolean isPropertyInParams(final BlackDuckArtifactoryProperty blackDuckArtifactoryProperty, final Map<String, List<String>> params) {
        return params.entrySet().stream()
                   .filter(stringListEntry -> stringListEntry.getKey().equals("properties"))
                   .map(Map.Entry::getValue)
                   .flatMap(List::stream)
                   .anyMatch(paramValue -> paramValue.equals(blackDuckArtifactoryProperty.getName()));
    }

    public List<RepoPath> getItemsContainingProperties(final String repoKey, final BlackDuckArtifactoryProperty... properties) {
        final SetMultimap<String, String> setMultimap = Arrays.stream(properties)
                                                            .filter(property -> property.getName() != null)
                                                            .collect(HashMultimap::create, (multimap, property) -> multimap.put(property.getName(), "*"), (self, other) -> self.putAll(other));

        return getItemsContainingPropertiesAndValues(setMultimap, repoKey);
    }

    public List<RepoPath> getItemsContainingPropertiesAndValues(final SetMultimap<String, String> setMultimap, final String... repoKeys) {
        return artifactoryPAPIService.itemsByProperties(setMultimap, repoKeys);
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
}
