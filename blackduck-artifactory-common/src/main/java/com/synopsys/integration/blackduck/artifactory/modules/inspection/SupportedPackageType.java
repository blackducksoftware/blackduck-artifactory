/**
 * blackduck-artifactory-common
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
package com.synopsys.integration.blackduck.artifactory.modules.inspection;

import java.util.Arrays;
import java.util.Optional;

import com.synopsys.integration.hub.bdio.model.Forge;

public enum SupportedPackageType {
    GEMS("gems", Forge.RUBYGEMS, "gem.name", "gem.version"),
    MAVEN("maven", Forge.MAVEN),
    GRADLE("gradle", Forge.MAVEN),
    PYPI("pypi", Forge.PYPI, "pypi.name", "pypi.version"),
    NUGET("nuget", Forge.NUGET, "nuget.id", "nuget.version"),
    NPM("npm", Forge.NPM, "npm.name", "npm.version");

    private final String artifactoryName;
    private final Forge forge;
    private final String artifactoryNameProperty;
    private final String artifactoryVersionProperty;
    private final boolean hasNameVersionProperties;

    SupportedPackageType(final String artifactoryName, final Forge forge, final String artifactoryNameProperty, final String artifactoryVersionProperty) {
        this.artifactoryName = artifactoryName;
        this.forge = forge;
        this.hasNameVersionProperties = true;
        this.artifactoryNameProperty = artifactoryNameProperty;
        this.artifactoryVersionProperty = artifactoryVersionProperty;
    }

    SupportedPackageType(final String artifactoryName, final Forge forge) {
        this.artifactoryName = artifactoryName;
        this.forge = forge;
        this.hasNameVersionProperties = false;
        this.artifactoryNameProperty = null;
        this.artifactoryVersionProperty = null;
    }

    public static Optional<SupportedPackageType> getAsSupportedPackageType(final String packageType) {
        return Arrays.stream(SupportedPackageType.values())
                   .filter(supportedPackageType -> supportedPackageType.getArtifactoryName().equalsIgnoreCase(packageType))
                   .findFirst();
    }

    public String getArtifactoryName() {
        return artifactoryName;
    }

    public Forge getForge() {
        return forge;
    }

    public String getArtifactoryNameProperty() {
        return artifactoryNameProperty;
    }

    public String getArtifactoryVersionProperty() {
        return artifactoryVersionProperty;
    }

    public boolean hasNameVersionProperties() {
        return hasNameVersionProperties;
    }
}
