/**
 * hub-artifactory-common
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
package com.synopsys.integration.blackduck.artifactory.modules.scan;

import com.synopsys.integration.blackduck.artifactory.ConfigurationProperty;

public enum ScanModuleProperty implements ConfigurationProperty {
    ADD_POLICY_STATUS_CRON("add.policy.status.cron"),
    BINARIES_DIRECTORY_PATH("binaries.directory.path"),
    CUTOFF_DATE("cutoff.date"),
    DRY_RUN("dry.run"),
    ENABLED("enabled"),
    NAME_PATTERNS("name.patterns"),
    MEMORY("memory"),
    REPO_PATH_CODELOCATION("repo.path.codelocation"),
    REPOS("repos"),
    REPOS_CSV_PATH("repos.csv.path"),
    SCAN_CRON("cron");

    private final String key;

    ScanModuleProperty(final String key) {
        this.key = "blackduck.artifactory.scan." + key;
    }

    @Override
    public String getKey() {
        return key;
    }
}
