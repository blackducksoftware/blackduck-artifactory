/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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

// TODO: Make this class abstract as each module should have a wrapper to provide a good API for setting module specific properties.
public class ArtifactoryPropertyService {
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final DateTimeManager dateTimeManager;

    public ArtifactoryPropertyService(ArtifactoryPAPIService artifactoryPAPIService, DateTimeManager dateTimeManager) {
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.dateTimeManager = dateTimeManager;
    }

    public boolean hasProperty(RepoPath repoPath, BlackDuckArtifactoryProperty property) {
        return artifactoryPAPIService.hasProperty(repoPath, property.getPropertyName());
    }

    public Optional<String> getProperty(RepoPath repoPath, BlackDuckArtifactoryProperty property) {
        return getProperty(repoPath, property.getPropertyName());
    }

    private Optional<String> getProperty(RepoPath repoPath, String propertyKey) {
        return artifactoryPAPIService.getProperty(repoPath, propertyKey)
                   .map(StringUtils::stripToNull);
    }

    public Optional<Integer> getPropertyAsInteger(RepoPath repoPath, BlackDuckArtifactoryProperty property) {
        return getProperty(repoPath, property)
                   .map(Integer::valueOf);
    }

    public Optional<Date> getDateFromProperty(RepoPath repoPath, BlackDuckArtifactoryProperty property) {
        Optional<String> dateTimeAsString = getProperty(repoPath, property);
        return dateTimeAsString.map(dateTimeManager::getDateFromString);
    }

    // TODO: These methods should not require an IntLogger, but a regular Logger
    public void setProperty(RepoPath repoPath, BlackDuckArtifactoryProperty property, String value, IntLogger logger) {
        setProperty(repoPath, property.getPropertyName(), value, logger);
    }

    private void setProperty(RepoPath repoPath, String property, String value, IntLogger logger) {
        artifactoryPAPIService.setProperty(repoPath, property, value);
        logger.debug(String.format("Set property %s to %s on %s.", property, value, repoPath.toPath()));
    }

    public void setPropertyFromDate(RepoPath repoPath, BlackDuckArtifactoryProperty property, Date date, IntLogger logger) {
        String dateTimeAsString = dateTimeManager.getStringFromDate(date);
        setProperty(repoPath, property, dateTimeAsString, logger);

        Optional<String> dateTimeAsStringConverted = dateTimeManager.geStringFromDateWithTimeZone(date);
        dateTimeAsStringConverted.ifPresent((converted) -> setProperty(repoPath, property.getTimeName(), converted, logger));
    }

    public void deleteProperty(RepoPath repoPath, BlackDuckArtifactoryProperty property, IntLogger logger) {
        deleteProperty(repoPath, property.getPropertyName(), logger);
        deleteProperty(repoPath, property.getTimeName(), logger);
    }

    private void deleteProperty(RepoPath repoPath, String propertyName, IntLogger logger) {
        if (artifactoryPAPIService.hasProperty(repoPath, propertyName)) {
            artifactoryPAPIService.deleteProperty(repoPath, propertyName);
            logger.debug("Removed property " + propertyName + " from " + repoPath.toPath() + ".");
        }
    }

    public void deleteAllBlackDuckPropertiesFromRepo(String repoKey, Map<String, List<String>> params, IntLogger logger) {
        List<RepoPath> repoPaths = Arrays.stream(BlackDuckArtifactoryProperty.values())
                                       .map(artifactoryProperty -> getItemsContainingAnyProperties(repoKey, artifactoryProperty))
                                       .flatMap(List::stream)
                                       .collect(Collectors.toList());

        repoPaths.forEach(repoPath -> deleteAllBlackDuckPropertiesFromRepoPath(repoPath, params, logger));
    }

    private List<RepoPath> getItemsContainingAnyProperties(String repoKey, BlackDuckArtifactoryProperty... properties) {
        return Arrays.stream(properties)
                   .map(property -> getItemsContainingProperties(repoKey, property))
                   .flatMap(List::stream)
                   .collect(Collectors.toList());
    }

    public void deleteAllBlackDuckPropertiesFromRepoPath(RepoPath repoPath, Map<String, List<String>> params, IntLogger logger) {
        List<BlackDuckArtifactoryProperty> properties = Arrays.stream(BlackDuckArtifactoryProperty.values())
                                                            .filter(property -> !isPropertyInParams(property, params))
                                                            .collect(Collectors.toList());

        properties.forEach(property -> deleteProperty(repoPath, property, logger));
    }

    private boolean isPropertyInParams(BlackDuckArtifactoryProperty blackDuckArtifactoryProperty, Map<String, List<String>> params) {
        return params.entrySet().stream()
                   .filter(stringListEntry -> stringListEntry.getKey().equals("properties"))
                   .map(Map.Entry::getValue)
                   .flatMap(List::stream)
                   .anyMatch(paramValue -> paramValue.equals(blackDuckArtifactoryProperty.getPropertyName()));
    }

    public List<RepoPath> getItemsContainingProperties(String repoKey, BlackDuckArtifactoryProperty... properties) {
        SetMultimap<String, String> setMultimap = Arrays.stream(properties)
                                                      .filter(property -> property.getPropertyName() != null)
                                                      .collect(HashMultimap::create, (multimap, property) -> multimap.put(property.getPropertyName(), "*"), (self, other) -> self.putAll(other));

        return getItemsContainingPropertiesAndValues(setMultimap, repoKey);
    }

    public List<RepoPath> getItemsContainingPropertiesAndValues(SetMultimap<String, String> properties, String... repoKeys) {
        Map<String, String> propertyMap = new HashMap<>();

        properties.keySet().forEach(key -> {
            Set<String> values = properties.get(key);
            if (values.size() > 1) {
                throw new UnsupportedOperationException("Cannot convert SetMultimap to Map because multiple values were assigned to the same key.");
            }

            propertyMap.put(key, values.iterator().next());
        });

        return getItemsContainingPropertiesAndValues(propertyMap, repoKeys);
    }

    private List<RepoPath> getItemsContainingPropertiesAndValues(Map<String, String> properties, String[] repoKeys) {
        return artifactoryPAPIService.itemsByProperties(properties, repoKeys);
    }

    public Optional<NameVersion> getProjectNameVersion(RepoPath repoPath) {
        Optional<String> projectName = getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME);
        Optional<String> projectVersionName = getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME);
        NameVersion nameVersion = null;

        if (projectName.isPresent() && projectVersionName.isPresent()) {
            nameVersion = new NameVersion(projectName.get(), projectVersionName.get());
        }

        return Optional.ofNullable(nameVersion);
    }
}
