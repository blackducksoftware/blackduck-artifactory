/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection;

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty;

public enum InspectionModuleProperty implements ConfigurationProperty {
    CRON("cron"),
    ENABLED("enabled"),
    METADATA_BLOCK("metadata.block"),
    METADATA_BLOCK_REPOS("metadata.block.repos"),
    METADATA_BLOCK_REPOS_CSV_PATH("metadata.block.repos.csv.path"),
    PATTERNS_BOWER("patterns.bower"),
    PATTERNS_COCOAPODS("patterns.cocoapods"),
    PATTERNS_COMPOSER("patterns.composer"),
    PATTERNS_CONDA("patterns.conda"),
    PATTERNS_CRAN("patterns.cran"),
    PATTERNS_GO("patterns.go"),
    PATTERNS_GRADLE("patterns.gradle"),
    PATTERNS_MAVEN("patterns.maven"),
    PATTERNS_NPM("patterns.npm"),
    PATTERNS_NUGET("patterns.nuget"),
    PATTERNS_PYPI("patterns.pypi"),
    PATTERNS_RUBYGEMS("patterns.rubygems"),
    POLICY_BLOCK("policy.block"),
    POLICY_REPOS("policy.repos"),
    POLICY_REPOS_CSV_PATH("policy.repos.csv.path"),
    POLICY_SEVERITY_TYPES("policy.severity.types"),
    REINSPECT_CRON("reinspect.cron"),
    REPOS("repos"),
    REPOS_CSV_PATH("repos.csv.path"),
    RETRY_COUNT("retry.count");

    private final String key;

    InspectionModuleProperty(String key) {
        this.key = "blackduck.artifactory.inspect." + key;
    }

    @Override
    public String getKey() {
        return key;
    }
}
