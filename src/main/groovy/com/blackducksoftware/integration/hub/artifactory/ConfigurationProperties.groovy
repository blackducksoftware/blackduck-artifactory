/*
 * hub-artifactory
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
package com.blackducksoftware.integration.hub.artifactory

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.PropertySources
import org.springframework.stereotype.Component

@Component
@PropertySources([
    @PropertySource('blackDuckCacheInspector.properties'),
    @PropertySource('blackDuckScanForHub.properties')
])
class ConfigurationProperties {
    @Value('${user.dir}')
    String currentUserDirectory

    @Value('${artifactory.url}')
    String artifactoryUrl

    @Value('${artifactory.api.key}')
    String artifactoryApiKey

    //Common
    @Value('${blackduck.hub.url}')
    String blackduckHubUrl

    @Value('${blackduck.hub.api.token}')
    String blackduckHubApiToken

    @Value('${blackduck.hub.timeout}')
    String blackduckHubTimeout

    @Value('${blackduck.hub.trust.cert}')
    String blackduckHubTrustCert

    @Value('${blackduck.hub.proxy.host}')
    String blackduckHubProxyHost

    @Value('${blackduck.hub.proxy.port}')
    String blackduckHubProxyPort

    @Value('${blackduck.hub.proxy.username}')
    String blackduckHubProxyUsername

    @Value('${blackduck.hub.proxy.password}')
    String blackduckHubProxyPassword

    //Inspector
    @Value('${hub.artifactory.inspect.repos}')
    String hubArtifactoryInspectRepositoriesList

    @Value('${hub.artifactory.inspect.repos.csv.path}')
    String hubArtifactoryInspectRepositoriesCsvPath

    @Value('${hub.artifactory.inspect.patterns.rubygems}')
    String hubArtifactoryInspectPatternsRubygems

    @Value('${hub.artifactory.inspect.patterns.maven}')
    String hubArtifactoryInspectPatternsMaven

    @Value('${hub.artifactory.inspect.patterns.gradle}')
    String hubArtifactoryInspectPatternsGradle

    @Value('${hub.artifactory.inspect.patterns.pypi}')
    String hubArtifactoryInspectPatternsPypi

    @Value('${hub.artifactory.inspect.patterns.nuget}')
    String hubArtifactoryInspectPatternsNuget

    @Value('${hub.artifactory.inspect.patterns.npm}')
    String hubArtifactoryInspectPatternsNpm

    @Value('${hub.artifactory.inspect.date.time.pattern}')
    String hubArtifactoryInspectDateTimePattern

    //Scanner
    @Value('${hub.artifactory.scan.repos}')
    String hubArtifactoryScanRepositoriesList

    @Value('${hub.artifactory.scan.repos.csv.path}')
    String hubArtifactoryScanRepositoriesCsvPath

    @Value('${hub.artifactory.scan.name.patterns}')
    String hubArtifactoryScanNamePatterns

    @Value('${hub.artifactory.scan.binaries.directory.path}')
    String hubArtifactoryScanBinariesDirectoryPath

    @Value('${hub.artifactory.scan.memory}')
    String hubArtifactoryScanMemory

    @Value('${hub.artifactory.scan.dry.run}')
    String hubArtifactoryScanDryRun

    @Value('${hub.artifactory.scan.date.time.pattern}')
    String hubArtifactoryScanDateTimePattern

    @Value('${hub.artifactory.scan.cutoff.date}')
    String hubArtifactoryScanCutoffDate
}
