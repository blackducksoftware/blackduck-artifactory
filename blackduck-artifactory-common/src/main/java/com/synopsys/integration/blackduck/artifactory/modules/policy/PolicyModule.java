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
package com.synopsys.integration.blackduck.artifactory.modules.policy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.artifactory.exception.CancelException;
import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.api.enumeration.PolicySeverityType;
import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.FeatureAnalyticsCollector;

public class PolicyModule implements Module {
    private final PolicyModuleConfig policyModuleConfig;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final FeatureAnalyticsCollector featureAnalyticsCollector;

    public PolicyModule(final PolicyModuleConfig policyModuleConfig, final ArtifactoryPropertyService artifactoryPropertyService, final FeatureAnalyticsCollector featureAnalyticsCollector) {
        this.policyModuleConfig = policyModuleConfig;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.featureAnalyticsCollector = featureAnalyticsCollector;
    }

    public void handleBeforeDownloadEvent(final RepoPath repoPath) {
        String reason = null;
        final boolean shouldBlock = false;
        if (shouldCancelOnPolicyViolation(repoPath)) {
            reason = "because it violates a policy in Black Duck.";
        }

        featureAnalyticsCollector.logFeatureHit("handleBeforeDownloadEvent", String.format("blocked:%s", Boolean.toString(shouldBlock)));

        if (reason != null) {
            throw new CancelException(String.format("The Black Duck %s has prevented the download of %s %s", PolicyModule.class.getSimpleName(), repoPath.toPath(), reason), 403);
        }
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return Collections.singletonList(featureAnalyticsCollector);
    }

    @Override
    public PolicyModuleConfig getModuleConfig() {
        return policyModuleConfig;
    }

    private boolean shouldCancelOnPolicyViolation(final RepoPath repoPath) {
        if (artifactoryPropertyService.hasProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS)) {
            // TODO: Fix in 8.0.0
            // Currently scanned artifacts are not supported because POLICY_STATUS and OVERALL_POLICY_STATUS is used in scans and there is overlap
            // with inspection using just POLICY_STATUS. Additional work will need to be done to sync these values and a use case for blocking
            // scanned artifacts has yet to present itself. JM - 08/2019
            return false;
        }

        final Optional<PolicySummaryStatusType> inViolationProperty = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS)
                                                                          .map(PolicySummaryStatusType::valueOf)
                                                                          .filter(it -> it.equals(PolicySummaryStatusType.IN_VIOLATION));

        if (inViolationProperty.isPresent()) {
            final Optional<String> severityTypes = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_SEVERITY_TYPES);
            if (severityTypes.isPresent()) {
                final List<PolicySeverityType> severityTypesToBlock = policyModuleConfig.getPolicySeverityTypes();
                final List<PolicySeverityType> matchingSeverityTypes = Arrays.stream(severityTypes.get().split(","))
                                                                           .map(PolicySeverityType::valueOf)
                                                                           .filter(severityTypesToBlock::contains)
                                                                           .collect(Collectors.toList());
                return !matchingSeverityTypes.isEmpty();
            } else {
                // The plugin should populate the severity types on artifacts automatically. But if an artifact is somehow missed, we want to be on the safe side.
                return true;
            }
        } else {
            return false;
        }
    }
}
