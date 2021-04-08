/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
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
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyStatusType;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.policy.PolicyModuleConfig;

public class PolicyCancelDecider implements CancelDecider {
    private final PolicyModuleConfig policyModuleConfig;
    private final ArtifactoryPropertyService artifactoryPropertyService;

    public PolicyCancelDecider(PolicyModuleConfig policyModuleConfig, ArtifactoryPropertyService artifactoryPropertyService) {
        this.policyModuleConfig = policyModuleConfig;
        this.artifactoryPropertyService = artifactoryPropertyService;
    }

    @Override
    public CancelDecision getCancelDecision(RepoPath repoPath) {
        if (artifactoryPropertyService.hasProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS)) {
            // TODO: Fix in 8.0.0
            // Currently scanned artifacts are not supported because POLICY_STATUS and OVERALL_POLICY_STATUS is used in scans and there is overlap
            // with inspection using just POLICY_STATUS. Additional work will need to be done to sync these values and a use case for blocking
            // scanned artifacts has yet to present itself. JM - 08/2019
            return CancelDecision.NO_CANCELLATION();
        }

        Optional<PolicyStatusType> inViolationProperty = getPolicyStatus(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS);
        if (inViolationProperty.isPresent() && PolicyStatusType.IN_VIOLATION.equals(inViolationProperty.get())) {
            return getDecisionBasedOnSeverity(repoPath);
        }

        return CancelDecision.NO_CANCELLATION();
    }

    private CancelDecision getDecisionBasedOnSeverity(RepoPath repoPath) {
        Optional<String> severityTypes = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES);
        if (severityTypes.isPresent()) {
            List<PolicyRuleSeverityType> severityTypesToBlock = policyModuleConfig.getPolicySeverityTypes();
            List<PolicyRuleSeverityType> matchingSeverityTypes = Arrays.stream(severityTypes.get().split(","))
                                                                     .map(PolicyRuleSeverityType::valueOf)
                                                                     .filter(severityTypesToBlock::contains)
                                                                     .collect(Collectors.toList());
            if (!matchingSeverityTypes.isEmpty()) {
                String matchingSeverityTypeMessage = StringUtils.join(matchingSeverityTypes, ",");
                return CancelDecision.CANCEL_DOWNLOAD(String.format("The artifact has policy severities (%s) that are blocked by the plugin.", matchingSeverityTypeMessage));
            }
        } else {
            // The plugin should populate the severity types on artifacts automatically. But if an artifact is somehow missed, we want to be on the safe side.
            return CancelDecision.CANCEL_DOWNLOAD(String.format("The plugin cannot find the %s property.", BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES.getPropertyName()));
        }

        return CancelDecision.NO_CANCELLATION();
    }

    private Optional<PolicyStatusType> getPolicyStatus(RepoPath repoPath, BlackDuckArtifactoryProperty policyStatusProperty) {
        return artifactoryPropertyService.getProperty(repoPath, policyStatusProperty)
                   .map(PolicyStatusType::valueOf)
                   .filter(it -> it.equals(PolicyStatusType.IN_VIOLATION));
    }
}
