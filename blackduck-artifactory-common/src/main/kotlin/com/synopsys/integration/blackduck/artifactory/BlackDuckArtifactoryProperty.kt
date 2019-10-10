/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2019 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.artifactory

enum class BlackDuckArtifactoryProperty(suffix: String) {
    BLACKDUCK_ORIGIN_ID("originId"),
    BLACKDUCK_FORGE("forge"),
    BLACKDUCK_PROJECT_NAME("projectName"),
    BLACKDUCK_PROJECT_VERSION_NAME("projectVersionName"),
    HIGH_VULNERABILITIES("highVulnerabilities"),
    MEDIUM_VULNERABILITIES("mediumVulnerabilities"),
    LOW_VULNERABILITIES("lowVulnerabilities"),
    POLICY_STATUS("policyStatus"),
    POLICY_SEVERITY_TYPES("policySeverityTypes"),
    COMPONENT_VERSION_URL("componentVersionUrl"),
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

    val propertyName: String = "blackduck.$suffix"
    val timeName: String = "$propertyName.converted"
}
