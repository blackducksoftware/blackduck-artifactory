/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.model;

// TODO: This class is overloaded. There should be different statuses for inspected artifacts and the status of the project (repo).
public enum InspectionStatus {
    SUCCESS,
    FAILURE,
    PENDING
}
