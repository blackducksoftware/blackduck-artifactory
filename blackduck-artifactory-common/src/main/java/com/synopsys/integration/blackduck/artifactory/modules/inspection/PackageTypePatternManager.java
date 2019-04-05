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
package com.synopsys.integration.blackduck.artifactory.modules.inspection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;

public class PackageTypePatternManager {
    private final Map<SupportedPackageType, List<String>> patternMap;

    public static PackageTypePatternManager fromInspectionModuleConfig(final InspectionModuleConfig inspectionModuleConfig) {
        final Map<SupportedPackageType, List<String>> patternMap = new HashMap<>();
        patternMap.put(SupportedPackageType.GEMS, inspectionModuleConfig.getPatternsRubygems());
        patternMap.put(SupportedPackageType.MAVEN, inspectionModuleConfig.getPatternsMaven());
        patternMap.put(SupportedPackageType.GRADLE, inspectionModuleConfig.getPatternsGradle());
        patternMap.put(SupportedPackageType.PYPI, inspectionModuleConfig.getPatternsPypi());
        patternMap.put(SupportedPackageType.NUGET, inspectionModuleConfig.getPatternsNuget());
        patternMap.put(SupportedPackageType.NPM, inspectionModuleConfig.getPatternsNpm());

        return new PackageTypePatternManager(patternMap);
    }

    public PackageTypePatternManager(final Map<SupportedPackageType, List<String>> patternMap) {
        this.patternMap = patternMap;
    }

    public List<String> getPatternsForPackageType(final String packageType) {
        return SupportedPackageType.getAsSupportedPackageType(packageType)
                   .map(this::getPatternsForPackageType)
                   .orElse(Collections.emptyList());
    }

    public Optional<List<String>> getPatterns(final String packageType) {
        Optional<List<String>> pattern = Optional.empty();

        final Optional<SupportedPackageType> possiblySupportedPackageType = SupportedPackageType.getAsSupportedPackageType(packageType);
        if (possiblySupportedPackageType.isPresent()) {
            pattern = getPatterns(possiblySupportedPackageType.get());
        }

        return pattern;
    }

    private Optional<List<String>> getPatterns(final SupportedPackageType packageType) {
        return Optional.ofNullable(patternMap.get(packageType));
    }

    private List<String> getPatternsForPackageType(final SupportedPackageType packageType) {
        return Optional.ofNullable(patternMap.get(packageType)).orElse(new ArrayList<>());
    }
}
