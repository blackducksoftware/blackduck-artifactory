/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.configuration.model;

import java.util.List;

public class ConfigValidationReport {
    private final PropertyGroupReport generalPropertyReport;
    private final List<PropertyGroupReport> modulePropertyReports;

    public ConfigValidationReport(PropertyGroupReport generalPropertyReport, List<PropertyGroupReport> modulePropertyReports) {
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
