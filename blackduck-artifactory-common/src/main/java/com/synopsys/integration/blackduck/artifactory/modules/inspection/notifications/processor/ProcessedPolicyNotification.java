package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.processor;

import java.util.List;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;

public class ProcessedPolicyNotification {
    private final String componentName;
    private final String componentVersionName;
    private final PolicyStatusReport policyStatusReport;
    private final List<RepoPath> affectedRepoKeyPaths;

    public ProcessedPolicyNotification(String componentName, String componentVersionName, PolicyStatusReport policyStatusReport, List<RepoPath> affectedRepoKeyPaths) {
        this.componentName = componentName;
        this.componentVersionName = componentVersionName;
        this.policyStatusReport = policyStatusReport;
        this.affectedRepoKeyPaths = affectedRepoKeyPaths;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getComponentVersionName() {
        return componentVersionName;
    }

    public PolicyStatusReport getPolicyStatusReport() {
        return policyStatusReport;
    }

    public List<RepoPath> getAffectedRepoKeyPaths() {
        return affectedRepoKeyPaths;
    }
}
