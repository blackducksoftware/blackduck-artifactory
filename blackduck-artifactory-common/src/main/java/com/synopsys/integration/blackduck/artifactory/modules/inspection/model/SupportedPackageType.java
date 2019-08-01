/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.model;

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleProperty;

public enum SupportedPackageType {
    BOWER("bower", Forge.NPMJS, "bower.name", "bower.version", InspectionModuleProperty.PATTERNS_BOWER),
    COCOAPODS("cocoapods", Forge.COCOAPODS, "pods.name", "pods.version", InspectionModuleProperty.PATTERNS_COCOAPODS),
    COMPOSER("composer", Forge.PACKAGIST, InspectionModuleProperty.PATTERNS_COMPOSER),
    CRAN("cran", Forge.CRAN, "cran.name", "cran.version", InspectionModuleProperty.PATTERNS_CRAN),
    GEMS("gems", Forge.RUBYGEMS, "gem.name", "gem.version", InspectionModuleProperty.PATTERNS_RUBYGEMS),
    GO("go", Forge.GOLANG, "go.name", "go.version", InspectionModuleProperty.PATTERNS_GO),
    GRADLE("gradle", Forge.MAVEN, InspectionModuleProperty.PATTERNS_GRADLE),
    MAVEN("maven", Forge.MAVEN, InspectionModuleProperty.PATTERNS_MAVEN),
    NPM("npm", Forge.NPMJS, "npm.name", "npm.version", InspectionModuleProperty.PATTERNS_NPM),
    NUGET("nuget", Forge.NUGET, "nuget.id", "nuget.version", InspectionModuleProperty.PATTERNS_NUGET),
    PYPI("pypi", Forge.PYPI, "pypi.name", "pypi.version", InspectionModuleProperty.PATTERNS_PYPI);

    private final String artifactoryName;
    private final Forge forge;
    private final String artifactoryNameProperty;
    private final String artifactoryVersionProperty;
    private final InspectionModuleProperty patternProperty;

    SupportedPackageType(final String artifactoryName, final Forge forge, final String artifactoryNameProperty, final String artifactoryVersionProperty, final InspectionModuleProperty patternProperty) {
        this.artifactoryName = artifactoryName;
        this.forge = forge;
        this.artifactoryNameProperty = artifactoryNameProperty;
        this.artifactoryVersionProperty = artifactoryVersionProperty;
        this.patternProperty = patternProperty;
    }

    SupportedPackageType(final String artifactoryName, final Forge forge, final InspectionModuleProperty patternProperty) {
        this.artifactoryName = artifactoryName;
        this.forge = forge;
        this.artifactoryNameProperty = null;
        this.artifactoryVersionProperty = null;
        this.patternProperty = patternProperty;
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

    public InspectionModuleProperty getPatternProperty() {
        return patternProperty;
    }

    public String getArtifactoryNameProperty() {
        return artifactoryNameProperty;
    }

    public String getArtifactoryVersionProperty() {
        return artifactoryVersionProperty;
    }

    public boolean hasNameVersionProperties() {
        return StringUtils.isNoneBlank(artifactoryNameProperty, artifactoryVersionProperty);
    }
}
