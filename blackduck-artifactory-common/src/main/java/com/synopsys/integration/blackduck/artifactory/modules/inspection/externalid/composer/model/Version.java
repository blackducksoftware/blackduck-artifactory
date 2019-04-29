package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model;

import com.google.gson.annotations.SerializedName;

public class Version {
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
