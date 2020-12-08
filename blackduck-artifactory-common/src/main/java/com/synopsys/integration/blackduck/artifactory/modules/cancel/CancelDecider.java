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
package com.synopsys.integration.blackduck.artifactory.modules.cancel;

import org.apache.commons.lang.StringUtils;
import org.artifactory.exception.CancelException;
import org.artifactory.repo.RepoPath;

public interface CancelDecider {
    CancelDecision getCancelDecision(RepoPath repoPath);

    default void handleBeforeDownloadEvent(RepoPath repoPath) {
        CancelDecision cancelDecision = getCancelDecision(repoPath);
        if (cancelDecision.shouldCancelDownload()) {
            String cancelMessageSuffix = StringUtils.trimToEmpty(cancelDecision.getCancelReason());
            if (StringUtils.isNotBlank(cancelMessageSuffix)) {
                cancelMessageSuffix = ". " + cancelMessageSuffix;
            } else {
                cancelMessageSuffix = ".";
            }

            throw new CancelException(String.format("The Black Duck plugin has prevented the download of %s%s", repoPath.toPath(), cancelMessageSuffix), 403);
        }
    }
}
