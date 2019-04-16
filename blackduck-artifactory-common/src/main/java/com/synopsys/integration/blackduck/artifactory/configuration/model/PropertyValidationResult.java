package com.synopsys.integration.blackduck.artifactory.configuration.model;

import java.util.Optional;

import org.apache.commons.lang.StringUtils;

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty;

public class PropertyValidationResult {
    private final ConfigurationProperty configurationProperty;
    private final String errorMessage;

    public PropertyValidationResult(final ConfigurationProperty configurationProperty) {
        this(configurationProperty, null);
    }

    public PropertyValidationResult(final ConfigurationProperty configurationProperty, final String errorMessage) {
        this.configurationProperty = configurationProperty;
        this.errorMessage = StringUtils.stripToNull(errorMessage);
    }

    public ConfigurationProperty getConfigurationProperty() {
        return configurationProperty;
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }
}
