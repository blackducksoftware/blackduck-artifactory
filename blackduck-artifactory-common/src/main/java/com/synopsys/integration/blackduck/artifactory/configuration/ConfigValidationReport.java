package com.synopsys.integration.blackduck.artifactory.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.synopsys.integration.builder.BuilderStatus;

public class ConfigValidationReport {
    private final List<PropertyValidationReport> propertyReports = new ArrayList<>();

    private final BuilderStatus builderStatus;

    public ConfigValidationReport(final BuilderStatus builderStatus) {
        this.builderStatus = builderStatus;
    }

    public void addPropertyValidationReport(final PropertyValidationReport propertyValidationReport) {
        propertyReports.add(propertyValidationReport);
    }

    public void addErrorMessage(final ConfigurationProperty configurationProperty, final String errorMessage) {
        final PropertyValidationReport propertyValidationReport = new PropertyValidationReport(configurationProperty, errorMessage);
        addPropertyValidationReport(propertyValidationReport);
    }

    public boolean hasError() {
        final boolean propertyReportErrorExists = propertyReports.stream()
                                                      .map(PropertyValidationReport::getErrorMessage)
                                                      .anyMatch(Optional::isPresent);

        return propertyReportErrorExists || !builderStatus.isValid();
    }

    public List<PropertyValidationReport> getPropertyReports() {
        return propertyReports;
    }

    public BuilderStatus getBuilderStatus() {
        return builderStatus;
    }
}
