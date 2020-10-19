package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.model;

import java.util.List;

import org.artifactory.repo.RepoPath;

// TODO: Rename
public class VulnerablityAffectedArtifact {
    private final List<RepoPath> affectedArtifacts;
    private final VulnerabilityAggregate vulnerabilityAggregate;

    public VulnerablityAffectedArtifact(List<RepoPath> foundArtifacts, VulnerabilityAggregate vulnerabilityAggregate) {
        this.affectedArtifacts = foundArtifacts;
        this.vulnerabilityAggregate = vulnerabilityAggregate;
    }

    public List<RepoPath> getAffectedArtifacts() {
        return affectedArtifacts;
    }

    public VulnerabilityAggregate getVulnerabilityAggregate() {
        return vulnerabilityAggregate;
    }
}
