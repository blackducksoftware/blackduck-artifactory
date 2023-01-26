/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model;

import com.google.gson.annotations.SerializedName;

public class ComposerVersion {
    @SerializedName("name")
    public String name;

    @SerializedName("version")
    public String version;

    @SerializedName("source")
    public VersionSource versionSource;

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public VersionSource getVersionSource() {
        return versionSource;
    }
}
