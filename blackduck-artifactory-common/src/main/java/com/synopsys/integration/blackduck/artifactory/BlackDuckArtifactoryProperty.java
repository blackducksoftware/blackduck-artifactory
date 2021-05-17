/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory;

public enum BlackDuckArtifactoryProperty {
    @Deprecated
    BLACKDUCK_ORIGIN_ID("originId"),
    @Deprecated
    BLACKDUCK_FORGE("forge"),
    BLACKDUCK_PROJECT_NAME("projectName"),
    BLACKDUCK_PROJECT_VERSION_NAME("projectVersionName"),
    CRITICAL_VULNERABILITIES("criticalVulnerabilities"),
    HIGH_VULNERABILITIES("highVulnerabilities"),
    MEDIUM_VULNERABILITIES("mediumVulnerabilities"),
    LOW_VULNERABILITIES("lowVulnerabilities"),
    POLICY_STATUS("policyStatus"),
    POLICY_SEVERITY_TYPES("policySeverityTypes"),
    COMPONENT_VERSION_URL("componentVersionUrl"),
    @Deprecated
    COMPONENT_NAME_VERSION("componentNameVersion"),
    COMPONENT_VERSION_ID("componentVersionId"),
    PROJECT_VERSION_UI_URL("uiUrl"),
    OVERALL_POLICY_STATUS("overallPolicyStatus"),
    LAST_INSPECTION("lastInspection"),
    INSPECTION_STATUS("inspectionStatus"),
    INSPECTION_STATUS_MESSAGE("inspectionStatusMessage"),
    LAST_UPDATE("lastUpdate"),
    UPDATE_STATUS("updateStatus"),
    SCAN_TIME("scanTime"),
    SCAN_RESULT("scanResult"),
    SCAN_RESULT_MESSAGE("scanResultMessage"),
    POST_SCAN_ACTION_STATUS("postScanActionStatus"),
    POST_SCAN_PHASE("postScanPhase"),
    INSPECTION_RETRY_COUNT("inspectionRetryCount");

    private final String propertyName;
    private final String timeName;

    BlackDuckArtifactoryProperty(String suffix) {
        propertyName = "blackduck." + suffix;
        timeName = propertyName + ".converted";
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getTimeName() {
        return timeName;
    }
}
