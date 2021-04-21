/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model;

import com.google.gson.annotations.SerializedName;

public class VersionSource {
    @SerializedName("reference")
    public String reference;

    public String getReference() {
        return reference;
    }
}
