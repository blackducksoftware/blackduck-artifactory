/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
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

import org.jetbrains.annotations.Nullable;

public class CancelDecision {
    private final boolean shouldCancelDownload;
    @Nullable
    private final String cancelReason;

    public static CancelDecision CANCEL_DOWNLOAD(String cancelReason) {
        return new CancelDecision(true, cancelReason);
    }

    public static CancelDecision NO_CANCELLATION() {
        return new CancelDecision(false, null);
    }

    private CancelDecision(boolean shouldCancelDownload, @Nullable String cancelReason) {
        this.shouldCancelDownload = shouldCancelDownload;
        this.cancelReason = cancelReason;
    }

    public boolean shouldCancelDownload() {
        return shouldCancelDownload;
    }

    @Nullable // null when CancelDecision::shouldCancelDownload is false
    public String getCancelReason() {
        return cancelReason;
    }
}
