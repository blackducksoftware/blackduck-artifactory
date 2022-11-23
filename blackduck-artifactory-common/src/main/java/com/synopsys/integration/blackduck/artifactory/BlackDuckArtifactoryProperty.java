/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory;

import java.util.Set;

import org.apache.commons.collections.set.UnmodifiableSet;

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
    INSPECTION_RETRY_COUNT("inspectionRetryCount"),
    SCAAAS_SCAN_STATUS("scaaas.scanStatus"),
    SCAAAS_POLICY_STATUS("scaaas.policyStatus"),
    SCAAAS_FAILED_COUNT("scaaas.scanFailedCount"),
    SCAAAS_LAST_UPDATE("scaaas.lastUpdate"),
    SCAAAS_RESULTS_URL("scaaas.resultsUrl"),
    SCAAAS_SCAN_FAILURE_MESSAGE("scaaas.scanFailureMessage"),
    SCAAAS_VIOLATING_POLICY_RULES("scaaas.violatingPolicyRules");

    private final String propertyName;
    private final String timeName;

    private static final Set<BlackDuckArtifactoryProperty> scaaasProperties = Set.of(
            SCAAAS_SCAN_STATUS,
            SCAAAS_POLICY_STATUS,
            SCAAAS_FAILED_COUNT,
            SCAAAS_LAST_UPDATE,
            SCAAAS_RESULTS_URL,
            SCAAAS_SCAN_FAILURE_MESSAGE,
            SCAAAS_VIOLATING_POLICY_RULES);

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

    public static Set<BlackDuckArtifactoryProperty> getScanAsAServiceProperties() {
        return scaaasProperties;
    }
}
