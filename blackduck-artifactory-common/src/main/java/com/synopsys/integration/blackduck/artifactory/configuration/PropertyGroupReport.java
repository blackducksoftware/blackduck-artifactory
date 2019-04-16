package com.synopsys.integration.blackduck.artifactory.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.synopsys.integration.builder.BuilderStatus;

public class PropertyGroupReport {
    private final List<PropertyValidationResult> propertyReports = new ArrayList<>();

    private final BuilderStatus builderStatus;

    public PropertyGroupReport(final BuilderStatus builderStatus) {
        this.builderStatus = builderStatus;
    }

    public void addPropertyValidationReport(final PropertyValidationResult propertyValidationResult) {
        propertyReports.add(propertyValidationResult);
    }

    public void addErrorMessage(final ConfigurationProperty configurationProperty, final String errorMessage) {
        final PropertyValidationResult propertyValidationResult = new PropertyValidationResult(configurationProperty, errorMessage);
        addPropertyValidationReport(propertyValidationResult);
    }

    public boolean hasError() {
        final boolean propertyReportErrorExists = propertyReports.stream()
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
}
