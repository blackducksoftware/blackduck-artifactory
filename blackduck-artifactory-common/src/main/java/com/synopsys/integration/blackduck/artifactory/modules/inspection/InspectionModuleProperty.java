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
    ENABLED("enabled"),
    CRON("cron"),
    REINSPECT_CRON("reinspect.cron"),
    METADATA_BLOCK("metadata.block"),
    PATTERNS_BOWER("patterns.bower"),
    PATTERNS_COCOAPODS("patterns.cocoapods"),
    PATTERNS_COMPOSER("patterns.composer"),
    PATTERNS_CONDA("patterns.conda"),
    PATTERNS_CRAN("patterns.cran"),
    PATTERNS_RUBYGEMS("patterns.rubygems"),
    PATTERNS_MAVEN("patterns.maven"),
    PATTERNS_GO("patterns.go"),
    PATTERNS_GRADLE("patterns.gradle"),
    PATTERNS_PYPI("patterns.pypi"),
    PATTERNS_NUGET("patterns.nuget"),
    PATTERNS_NPM("patterns.npm"),
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
