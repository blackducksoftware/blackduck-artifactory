package com.synopsys.integration.blackduck.artifactory.configuration;

import java.util.Optional;

import org.apache.commons.lang.StringUtils;

public class PropertyValidationReport {
    private final ConfigurationProperty configurationProperty;
    private final String errorMessage;

    public PropertyValidationReport(final ConfigurationProperty configurationProperty) {
        this(configurationProperty, null);
    }

    public PropertyValidationReport(final ConfigurationProperty configurationProperty, final String errorMessage) {
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
