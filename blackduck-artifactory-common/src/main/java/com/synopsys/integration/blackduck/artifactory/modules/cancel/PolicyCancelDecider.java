/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionComponentPolicyStatusType;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;

public class PolicyCancelDecider implements CancelDecider {
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final Boolean policyBlockedEnabled;
    private final List<String> policyRepos;
    private final List<PolicyRuleSeverityType> policySeverityTypes;

    public PolicyCancelDecider(ArtifactoryPropertyService artifactoryPropertyService, Boolean policyBlockedEnabled, List<String> policyRepos, List<PolicyRuleSeverityType> policySeverityTypes) {
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.policyBlockedEnabled = policyBlockedEnabled;
        this.policyRepos = policyRepos;
        this.policySeverityTypes = policySeverityTypes;
    }

    @Override
    public CancelDecision getCancelDecision(RepoPath repoPath) {
        if (Boolean.FALSE.equals(policyBlockedEnabled)) {
            return CancelDecision.NO_CANCELLATION();
        }

        if (!policyRepos.contains(repoPath.getRepoKey())) {
            return CancelDecision.NO_CANCELLATION();
        }

        BlackDuckArtifactoryProperty policyStatusProperty = BlackDuckArtifactoryProperty.POLICY_STATUS;
        if (artifactoryPropertyService.hasProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_TIME)) {
            if (!artifactoryPropertyService.hasProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS)) {
                return CancelDecision.CANCEL_DOWNLOAD("This artifact is still being scanned. The artifact will not be able to be downloaded until the scan and post-scan actions have completed.");
            }
            policyStatusProperty = BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS;
        }

        Optional<ProjectVersionComponentPolicyStatusType> inViolationProperty = getPolicyStatus(repoPath, policyStatusProperty);
        if (inViolationProperty.isPresent() && ProjectVersionComponentPolicyStatusType.IN_VIOLATION.equals(inViolationProperty.get())) {
            return getDecisionBasedOnSeverity(repoPath);
        }

        return CancelDecision.NO_CANCELLATION();
    }

    private CancelDecision getDecisionBasedOnSeverity(RepoPath repoPath) {
        Optional<String> severityTypes = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES);
        if (severityTypes.isPresent()) {
            List<PolicyRuleSeverityType> matchingSeverityTypes = Arrays.stream(severityTypes.get().split(","))
                                                                     .map(PolicyRuleSeverityType::valueOf)
                                                                     .filter(policySeverityTypes::contains)
                                                                     .collect(Collectors.toList());
            if (!matchingSeverityTypes.isEmpty()) {
                String matchingSeverityTypeMessage = StringUtils.join(matchingSeverityTypes, ",");
                return CancelDecision.CANCEL_DOWNLOAD(String.format("The artifact has policy severities (%s) that are blocked by the plugin.", matchingSeverityTypeMessage));
            }
        } else {
            // The plugin should populate the severity types on artifacts automatically. But if an artifact is somehow missed, we want to err on the side of caution.
            return CancelDecision.CANCEL_DOWNLOAD(String.format("The plugin cannot find the %s property.", BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES.getPropertyName()));
        }

        return CancelDecision.NO_CANCELLATION();
    }

    private Optional<ProjectVersionComponentPolicyStatusType> getPolicyStatus(RepoPath repoPath, BlackDuckArtifactoryProperty policyStatusProperty) {
        return artifactoryPropertyService.getProperty(repoPath, policyStatusProperty)
                   .map(ProjectVersionComponentPolicyStatusType::valueOf)
                   .filter(it -> it.equals(ProjectVersionComponentPolicyStatusType.IN_VIOLATION));
    }
}
