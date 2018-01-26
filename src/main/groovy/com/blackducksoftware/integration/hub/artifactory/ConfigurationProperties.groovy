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
import org.springframework.stereotype.Component

@Component
class ConfigurationProperties {
    @Value('${user.dir}')
    String currentUserDirectory

    @Value('${hub.url}')
    String hubUrl

    @Value('${hub.timeout}')
    String hubTimeout

    @Value('${hub.username}')
    String hubUsername

    @Value('${hub.password}')
    String hubPassword

    @Value('${hub.trust.cert}')
    String hubAlwaysTrustCerts

    @Value('${hub.proxy.host}')
    String hubProxyHost

    @Value('${hub.proxy.port}')
    String hubProxyPort

    @Value('${hub.proxy.username}')
    String hubProxyUsername

    @Value('${hub.proxy.password}')
    String hubProxyPassword

    @Value('${artifactory.username}')
    String artifactoryUsername

    @Value('${artifactory.password}')
    String artifactoryPassword

    @Value('${artifactory.url}')
    String artifactoryUrl

    @Value('${hub.artifactory.working.directory.path}')
    String hubArtifactoryWorkingDirectoryPath

    @Value('${hub.artifactory.project.name}')
    String hubArtifactoryProjectName

    @Value('${hub.artifactory.project.version.name}')
    String hubArtifactoryProjectVersionName

    @Value('${hub.artifactory.date.time.pattern}')
    String hubArtifactoryDateTimePattern

    @Value('${hub.artifactory.inspect.repo.key}')
    String hubArtifactoryInspectRepoKey

    @Value('${hub.artifactory.inspect.latest.updated.cutoff}')
    String hubArtifactoryInspectLatestUpdatedCutoff

    @Value('${hub.artifactory.inspect.skip.bom.calculation}')
    String hubArtifactoryInspectSkipBomCalculation

    @Value('${hub.artifactory.scan.repos.to.search}')
    String hubArtifactoryScanReposToSearch

    @Value('${hub.artifactory.scan.name.patterns}')
    String hubArtifactoryScanNamePatterns

    @Value('${hub.artifactory.scan.binaries.directory.path}')
    String hubArtifactoryScanBinariesDirectoryPath
}
