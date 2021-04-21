/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model;

public class FileNamePieces {
    private final String componentName;
    private final String hash;
    private final String extension;

    public FileNamePieces(String componentName, String hash, String extension) {
        this.componentName = componentName;
        this.hash = hash;
        this.extension = extension;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getHash() {
        return hash;
    }

    public String getExtension() {
        return extension;
    }
}
