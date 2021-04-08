/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scan;

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty;

public enum ScanModuleProperty implements ConfigurationProperty {
    CRON("cron"),
    BINARIES_DIRECTORY_PATH("binaries.directory.path"),
    CUTOFF_DATE("cutoff.date"),
    DRY_RUN("dry.run"),
    ENABLED("enabled"),
    NAME_PATTERNS("name.patterns"),
    MEMORY("memory"),
    REPO_PATH_CODELOCATION("repo.path.codelocation"),
    REPOS("repos"),
    REPOS_CSV_PATH("repos.csv.path"),
    CODELOCATION_INCLUDE_HOSTNAME("repo.path.codelocation.include.hostname"),
    METADATA_BLOCK("metadata.block");

    private final String key;

    ScanModuleProperty(String key) {
        this.key = "blackduck.artifactory.scan." + key;
    }

    @Override
    public String getKey() {
        return key;
    }
}

