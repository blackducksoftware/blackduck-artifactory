/*
 * hub-artifactory
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.blackducksoftware.integration.hub.artifactory

import org.artifactory.exception.CancelException
import org.artifactory.repo.RepoPath
import org.artifactory.request.Request

import com.blackducksoftware.integration.hub.api.generated.enumeration.PolicyStatusApprovalStatusType

download {
    beforeDownload { Request request, RepoPath repoPath ->
        def policyStatus = repositories.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS.getName())
        if (PolicyStatusApprovalStatusType.IN_VIOLATION.name().equals(policyStatus)) {
            throw new CancelException("Black Duck Policy Enforcer has prevented the download of ${repoPath.toPath()} because it violates a policy in your Black Duck Hub.", 403)
        }
    }
}
