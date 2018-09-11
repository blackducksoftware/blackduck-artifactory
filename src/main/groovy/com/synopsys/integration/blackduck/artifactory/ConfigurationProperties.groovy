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
package com.synopsys.integration.blackduck.artifactory

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
    @Value('${blackduck.url}')
    String blackduckHubUrl

    @Value('${blackduck.username}')
    String blackduckHubUsername

    @Value('${blackduck.password}')
    String blackduckHubPassword

    @Value('${blackduck.api.token}')
    String blackduckHubApiToken

    @Value('${blackduck.timeout}')
    String blackduckHubTimeout

    @Value('${blackduck.trust.cert}')
    String blackduckHubTrustCert

    @Value('${blackduck.proxy.host}')
    String blackduckHubProxyHost

    @Value('${blackduck.proxy.port}')
    String blackduckHubProxyPort

    @Value('${blackduck.proxy.username}')
    String blackduckHubProxyUsername

    @Value('${blackduck.proxy.password}')
    String blackduckHubProxyPassword

    //Inspector
    @Value('${blackduck.artifactory.inspect.repos}')
    String hubArtifactoryInspectRepositoriesList

    @Value('${blackduck.artifactory.inspect.repos.csv.path}')
    String hubArtifactoryInspectRepositoriesCsvPath

    @Value('${blackduck.artifactory.inspect.patterns.rubygems}')
    String hubArtifactoryInspectPatternsRubygems

    @Value('${blackduck.artifactory.inspect.patterns.maven}')
    String hubArtifactoryInspectPatternsMaven

    @Value('${blackduck.artifactory.inspect.patterns.gradle}')
    String hubArtifactoryInspectPatternsGradle

    @Value('${blackduck.artifactory.inspect.patterns.pypi}')
    String hubArtifactoryInspectPatternsPypi

    @Value('${blackduck.artifactory.inspect.patterns.nuget}')
    String hubArtifactoryInspectPatternsNuget

    @Value('${blackduck.artifactory.inspect.patterns.npm}')
    String hubArtifactoryInspectPatternsNpm

    @Value('${blackduck.artifactory.inspect.date.time.pattern}')
    String hubArtifactoryInspectDateTimePattern

    @Value('${blackduck.artifactory.inspect.identify.artifacts.cron}')
    String hubArtifactoryInspectIdentifyArtifactsCron

    @Value('${blackduck.artifactory.inspect.populate.metadata.cron}')
    String hubArtifactoryInspectPopulateMetadataCron

    @Value('${blackduck.artifactory.inspect.update.metadata.cron}')
    String hubArtifactoryInspectUpdateMetadataCron

    @Value('${blackduck.artifactory.inspect.add.pending.artifacts.cron}')
    String hubArtifactoryInspectAddPendingArtifactsCron

    //Scanner
    @Value('${blackduck.artifactory.scan.repos}')
    String hubArtifactoryScanRepositoriesList

    @Value('${blackduck.artifactory.scan.repos.csv.path}')
    String hubArtifactoryScanRepositoriesCsvPath

    @Value('${blackduck.artifactory.scan.name.patterns}')
    String hubArtifactoryScanNamePatterns

    @Value('${blackduck.artifactory.scan.binaries.directory.path}')
    String hubArtifactoryScanBinariesDirectoryPath

    @Value('${blackduck.artifactory.scan.memory}')
    String hubArtifactoryScanMemory

    @Value('${blackduck.artifactory.scan.dry.run}')
    String hubArtifactoryScanDryRun

    @Value('${blackduck.artifactory.scan.date.time.pattern}')
    String hubArtifactoryScanDateTimePattern

    @Value('${blackduck.artifactory.scan.cutoff.date}')
    String hubArtifactoryScanCutoffDate

    @Value('${blackduck.artifactory.scan.cron')
    String hubArtifactoryScanCron

    @Value('${blackduck.artifactory.scan.add.policy.status.cron')
    String hubArtifactoryScanAddPolicyStatusCron

    @Value('${blackduck.artifactory.scan.repo.path.codelocation}')
    String hubArtifactoryScanRepoPathCodelocation

}
