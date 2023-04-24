/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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
    CONDA("conda", Forge.ANACONDA, InspectionModuleProperty.PATTERNS_CONDA),
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

    SupportedPackageType(String artifactoryName, Forge forge, String artifactoryNameProperty, String artifactoryVersionProperty, InspectionModuleProperty patternProperty) {
        this.artifactoryName = artifactoryName;
        this.forge = forge;
        this.artifactoryNameProperty = artifactoryNameProperty;
        this.artifactoryVersionProperty = artifactoryVersionProperty;
        this.patternProperty = patternProperty;
    }

    SupportedPackageType(String artifactoryName, Forge forge, InspectionModuleProperty patternProperty) {
        this.artifactoryName = artifactoryName;
        this.forge = forge;
        this.artifactoryNameProperty = null;
        this.artifactoryVersionProperty = null;
        this.patternProperty = patternProperty;
    }

    public static Optional<SupportedPackageType> getAsSupportedPackageType(String packageType) {
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
