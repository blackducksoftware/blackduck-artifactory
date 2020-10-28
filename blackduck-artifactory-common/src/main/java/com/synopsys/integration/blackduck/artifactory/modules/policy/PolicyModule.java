/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2020 Synopsys, Inc.
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

import java.util.Collections;
import java.util.List;

import org.artifactory.exception.CancelException;
import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.FeatureAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.cancel.CancelDecider;

public class PolicyModule implements Module {
    private final PolicyModuleConfig policyModuleConfig;
    private final FeatureAnalyticsCollector featureAnalyticsCollector;
    private final CancelDecider cancelDecider;

    public PolicyModule(PolicyModuleConfig policyModuleConfig, FeatureAnalyticsCollector featureAnalyticsCollector, CancelDecider cancelDecider) {
        this.policyModuleConfig = policyModuleConfig;
        this.featureAnalyticsCollector = featureAnalyticsCollector;
        this.cancelDecider = cancelDecider;
    }

    public void handleBeforeDownloadEvent(RepoPath repoPath) {
        try {
            cancelDecider.handleBeforeDownloadEvent(repoPath);
            logFeatureHit(false);
        } catch (CancelException cancelException) {
            logFeatureHit(true);
            throw cancelException;
        }
    }

    private void logFeatureHit(boolean wasDownloadBlocked) {
        featureAnalyticsCollector.logFeatureHit("handleBeforeDownloadEvent", String.format("blocked:%s", wasDownloadBlocked));
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return Collections.singletonList(featureAnalyticsCollector);
    }

    @Override
    public PolicyModuleConfig getModuleConfig() {
        return policyModuleConfig;
    }
}
