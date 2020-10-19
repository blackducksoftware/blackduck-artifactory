package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model;

import java.util.List;

import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.PolicyStatusReport;

public class PolicyAffectedArtifact {
    private final List<RepoPath> affectedArtifacts;
    private final PolicyStatusReport policyStatusReport;

    public PolicyAffectedArtifact(List<RepoPath> foundArtifacts, PolicyStatusReport policyStatusReport) {
        this.affectedArtifacts = foundArtifacts;
        this.policyStatusReport = policyStatusReport;
    }

    public List<RepoPath> getAffectedArtifacts() {
        return affectedArtifacts;
    }

    public PolicyStatusReport getPolicyStatusReport() {
        return policyStatusReport;
    }
}
