/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor;

import java.util.List;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;

public class ProcessedPolicyNotification {
    private final String componentName;
    private final String componentVersionName;
    private final String componentVersionId;
    private final PolicyStatusReport policyStatusReport;
    private final List<RepoPath> affectedRepoKeyPaths;

    public ProcessedPolicyNotification(String componentName, String componentVersionName, String componentVersionId, PolicyStatusReport policyStatusReport, List<RepoPath> affectedRepoKeyPaths) {
        this.componentName = componentName;
        this.componentVersionName = componentVersionName;
        this.componentVersionId = componentVersionId;
        this.policyStatusReport = policyStatusReport;
        this.affectedRepoKeyPaths = affectedRepoKeyPaths;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getComponentVersionName() {
        return componentVersionName;
    }

    public String getComponentVersionId() {
        return componentVersionId;
    }

    public PolicyStatusReport getPolicyStatusReport() {
        return policyStatusReport;
    }

    public List<RepoPath> getAffectedRepoKeyPaths() {
        return affectedRepoKeyPaths;
    }
}
