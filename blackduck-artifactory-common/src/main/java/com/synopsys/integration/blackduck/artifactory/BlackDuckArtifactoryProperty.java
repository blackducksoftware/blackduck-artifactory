/**
 * hub-artifactory-common
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.synopsys.integration.blackduck.artifactory;

// TODO: Remove oldName in hub-artifactorty:7.0.0
public enum BlackDuckArtifactoryProperty {
    BLACKDUCK_ORIGIN_ID("originId", "hubOriginId"),
    BLACKDUCK_FORGE("forge", "hubForge"),
    BLACKDUCK_PROJECT_NAME("projectName", "hubProjectName"),
    BLACKDUCK_PROJECT_VERSION_NAME("projectVersionName", "hubProjectVersionName"),
    HIGH_VULNERABILITIES("highVulnerabilities"),
    MEDIUM_VULNERABILITIES("mediumVulnerabilities"),
    LOW_VULNERABILITIES("lowVulnerabilities"),
    POLICY_STATUS("policyStatus"),
    COMPONENT_VERSION_URL("componentVersionUrl"),
    PROJECT_VERSION_UI_URL("uiUrl"),
    OVERALL_POLICY_STATUS("overallPolicyStatus"),
    LAST_INSPECTION("lastInspection"),
    INSPECTION_STATUS("inspectionStatus"),
    LAST_UPDATE("lastUpdate"),
    UPDATE_STATUS("updateStatus"),
    SCAN_TIME("scanTime"),
    SCAN_RESULT("scanResult"),
    @Deprecated
    PROJECT_VERSION_URL(null, "apiUrl");

    public static final String PROPERTY_PREFIX = "blackduck.";

    private final String name;
    private final String oldName;

    BlackDuckArtifactoryProperty(final String name) {
        this.name = PROPERTY_PREFIX + name;
        this.oldName = null;
    }

    /**
     * If the property is deprecated, user this constructor.
     * @param name    The new name for the property. Set this to null if the property is being removed
     * @param oldName The old name of the property. This can be used to search for the property by its old
     *                name until the property is removed or renamed.
     */
    BlackDuckArtifactoryProperty(final String name, final String oldName) {
        this.name = name == null ? null : PROPERTY_PREFIX + name;
        this.oldName = PROPERTY_PREFIX + oldName;
    }

    public String getName() {
        return name;
    }

    public String getOldName() {
        return oldName;
    }
}
