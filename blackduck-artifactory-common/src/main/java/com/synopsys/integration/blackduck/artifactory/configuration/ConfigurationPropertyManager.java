/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

/**
 * Gets configuration properties and coerces them.
 * Should only be consumed by configs with validation
 */
public class ConfigurationPropertyManager {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final Properties properties;

    public ConfigurationPropertyManager(Properties properties) {
        this.properties = properties;
    }

    public List<String> getRepositoryKeysFromProperties(ConfigurationProperty repositoryKeyListProperty, ConfigurationProperty repositoryKeyCsvProperty) throws IOException {
        List<String> repositoryKeys = new LinkedList<>();

        String repositoryKeyListString = getProperty(repositoryKeyListProperty);
        String repositoryKeyCsvPath = getProperty(repositoryKeyCsvProperty);

        if (StringUtils.isNotBlank(repositoryKeyCsvPath)) {
            File repositoryKeyCsvFile = new File(repositoryKeyCsvPath);
            if (repositoryKeyCsvFile.isFile()) {
                repositoryKeys = Files.readAllLines(repositoryKeyCsvFile.toPath()).stream()
                                     .map(line -> line.split(","))
                                     .flatMap(Arrays::stream)
                                     .filter(StringUtils::isNotBlank)
                                     .collect(Collectors.toList());
            } else {
                logger.warn(String.format("A path to a CSV file was provided, but the value is not a file. Defaulting to value from the %s property.", repositoryKeyListProperty.getKey()));
            }
        }

        if (repositoryKeys.isEmpty() && StringUtils.isNotBlank(repositoryKeyListString)) {
            repositoryKeys = Arrays.stream(repositoryKeyListString.split(","))
                                 .filter(StringUtils::isNotBlank)
                                 .collect(Collectors.toList());
        }

        return repositoryKeys;
    }

    public Properties getProperties() {
        return properties;
    }

    public Set<Map.Entry<String, String>> getPropertyEntries() {
        return properties.stringPropertyNames().stream()
                   .collect(Collectors.toMap(name -> name, properties::getProperty))
                   .entrySet();
    }

    @Nullable
    public String getProperty(ConfigurationProperty configurationProperty) {
        return getProperty(configurationProperty.getKey());
    }

    @Nullable
    private String getProperty(String propertyKey) {
        return properties.getProperty(propertyKey);
    }

    @Nullable
    public Boolean getBooleanProperty(ConfigurationProperty configurationProperty) {
        return getBooleanProperty(configurationProperty.getKey());
    }

    @Nullable
    private Boolean getBooleanProperty(String propertyKey) {
        return BooleanUtils.toBooleanObject(getProperty(propertyKey));
    }

    @Nullable
    public Integer getIntegerProperty(ConfigurationProperty configurationProperty) {
        return getIntegerProperty(configurationProperty.getKey());
    }

    @Nullable
    private Integer getIntegerProperty(String propertyKey) {
        Integer value = null;
        try {
            String propertyValue = getProperty(propertyKey);
            if (propertyValue == null) {
                throw new IntegrationException("Property not set.");
            }
            value = Integer.valueOf(propertyValue);
        } catch (NumberFormatException | IntegrationException e) {
            logger.debug(String.format("Failed to parse integer for property: %s", propertyKey), e);
        }

        return value;
    }

    public List<String> getPropertyAsList(ConfigurationProperty configurationProperty) {
        List<String> values;
        String property = getProperty(configurationProperty);

        if (StringUtils.isNotBlank(property)) {
            String[] propertyValues = property.split(",");
            values = Arrays.asList(propertyValues);
        } else {
            values = Collections.emptyList();
        }

        return values;
    }
}
