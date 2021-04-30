/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scan;

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty;

public enum ScanModuleProperty implements ConfigurationProperty {
    BINARIES_DIRECTORY_PATH("binaries.directory.path"),
    CODELOCATION_INCLUDE_HOSTNAME("repo.path.codelocation.include.hostname"),
    CRON("cron"),
    CUTOFF_DATE("cutoff.date"),
    DRY_RUN("dry.run"),
    ENABLED("enabled"),
    MEMORY("memory"),
    METADATA_BLOCK("metadata.block"),
    METADATA_BLOCK_REPOS("metadata.block.repos"),
    METADATA_BLOCK_REPOS_CSV_PATH("metadata.block.repos.csv.path"),
    NAME_PATTERNS("name.patterns"),
    POLICY_BLOCK("policy.block"),
    POLICY_REPOS("policy.repos"),
    POLICY_REPOS_CSV_PATH("policy.repos.csv.path"),
    POLICY_SEVERITY_TYPES("policy.severity.types"),
    REPOS("repos"),
    REPOS_CSV_PATH("repos.csv.path"),
    REPO_PATH_CODELOCATION("repo.path.codelocation");

    private final String key;

    ScanModuleProperty(String key) {
        this.key = "blackduck.artifactory.scan." + key;
    }

    @Override
    public String getKey() {
        return key;
    }
}

