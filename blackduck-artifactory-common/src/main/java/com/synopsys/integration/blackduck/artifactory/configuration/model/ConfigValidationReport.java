package com.synopsys.integration.blackduck.artifactory.configuration.model;

import java.util.List;

public class ConfigValidationReport {
    private final PropertyGroupReport generalPropertyReport;
    private final List<PropertyGroupReport> modulePropertyReports;

    public ConfigValidationReport(final PropertyGroupReport generalPropertyReport, final List<PropertyGroupReport> modulePropertyReports) {
        this.generalPropertyReport = generalPropertyReport;
        this.modulePropertyReports = modulePropertyReports;
    }

    public PropertyGroupReport getGeneralPropertyReport() {
        return generalPropertyReport;
    }

    public List<PropertyGroupReport> getModulePropertyReports() {
        return modulePropertyReports;
    }
}
