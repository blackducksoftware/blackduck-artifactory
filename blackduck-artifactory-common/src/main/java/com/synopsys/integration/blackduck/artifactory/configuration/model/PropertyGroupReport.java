/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.configuration.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty;
import com.synopsys.integration.builder.BuilderStatus;

public class PropertyGroupReport {
    private final List<PropertyValidationResult> propertyReports = new ArrayList<>();

    private final String propertyGroupName;
    private final BuilderStatus builderStatus;

    public PropertyGroupReport(String propertyGroupName, BuilderStatus builderStatus) {
        this.propertyGroupName = propertyGroupName;
        this.builderStatus = builderStatus;
    }

    public void addPropertyValidationReport(PropertyValidationResult propertyValidationResult) {
        propertyReports.add(propertyValidationResult);
    }

    public void addErrorMessage(ConfigurationProperty configurationProperty, String errorMessage) {
        PropertyValidationResult propertyValidationResult = new PropertyValidationResult(configurationProperty, errorMessage);
        addPropertyValidationReport(propertyValidationResult);
    }

    public boolean hasError() {
        boolean propertyReportErrorExists = propertyReports.stream()
                                                      .map(PropertyValidationResult::getErrorMessage)
                                                      .anyMatch(Optional::isPresent);

        return propertyReportErrorExists || !builderStatus.isValid();
    }

    public List<PropertyValidationResult> getPropertyReports() {
        return propertyReports;
    }

    public BuilderStatus getBuilderStatus() {
        return builderStatus;
    }

    public String getPropertyGroupName() {
        return propertyGroupName;
    }
}
