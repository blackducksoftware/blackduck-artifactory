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
