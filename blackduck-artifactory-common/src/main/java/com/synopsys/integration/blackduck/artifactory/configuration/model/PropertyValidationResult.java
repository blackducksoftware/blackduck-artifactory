/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.configuration.model;

import java.util.Optional;

import org.apache.commons.lang.StringUtils;

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty;

public class PropertyValidationResult {
    private final ConfigurationProperty configurationProperty;
    private final String errorMessage;

    public PropertyValidationResult(ConfigurationProperty configurationProperty) {
        this(configurationProperty, null);
    }

    public PropertyValidationResult(ConfigurationProperty configurationProperty, String errorMessage) {
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
