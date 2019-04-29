package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model;

import com.google.gson.annotations.SerializedName;

public class VersionSource {
    @SerializedName("reference")
    private String reference;

    public String getReference() {
        return reference;
    }
}
