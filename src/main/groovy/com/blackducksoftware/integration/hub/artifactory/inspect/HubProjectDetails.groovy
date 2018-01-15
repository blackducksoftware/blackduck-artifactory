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
package com.blackducksoftware.integration.hub.artifactory.inspect

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.api.project.ProjectService
import com.blackducksoftware.integration.hub.api.project.version.ProjectVersionService
import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperties
import com.blackducksoftware.integration.hub.dataservice.policystatus.PolicyStatusDataService
import com.blackducksoftware.integration.hub.dataservice.policystatus.PolicyStatusDescription
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.model.enumeration.VersionBomPolicyStatusOverallStatusEnum
import com.blackducksoftware.integration.hub.model.view.ProjectVersionView
import com.blackducksoftware.integration.hub.model.view.ProjectView
import com.blackducksoftware.integration.hub.model.view.VersionBomPolicyStatusView
import com.blackducksoftware.integration.hub.service.HubServicesFactory

@Component
class HubProjectDetails {
    private final Logger logger = LoggerFactory.getLogger(HubProjectDetails.class)

    @Autowired
    ConfigurationProperties configurationProperties

    @Autowired
    HubClient hubClient

    String getHubProjectName() {
        if (StringUtils.isBlank(configurationProperties.hubArtifactoryProjectName)) {
            return configurationProperties.hubArtifactoryInspectRepoKey
        } else {
            return configurationProperties.hubArtifactoryProjectName
        }
    }

    String getHubProjectVersionName() {
        if (StringUtils.isBlank(configurationProperties.hubArtifactoryProjectVersionName)) {
            return DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now())
        } else {
            return configurationProperties.hubArtifactoryProjectVersionName
        }
    }

    VersionBomPolicyStatusOverallStatusEnum getHubProjectOverallPolicyStatus(String projectName, String projectVersionName){
        HubServicesFactory hubServicesFactory = hubClient.getHubServicesFactory()
        PolicyStatusDataService policyStatusDataService = hubServicesFactory.createPolicyStatusDataService()
        VersionBomPolicyStatusView versionBomPolicyStatusView = policyStatusDataService.getPolicyStatusForProjectAndVersion(projectName, projectVersionName)
        versionBomPolicyStatusView.overallStatus
    }

    String getHubProjectPolicyStatus(String projectName, String projectVersionName){
        HubServicesFactory hubServicesFactory = hubClient.getHubServicesFactory()
        PolicyStatusDataService policyStatusDataService = hubServicesFactory.createPolicyStatusDataService()
        VersionBomPolicyStatusView versionBomPolicyStatusView = policyStatusDataService.getPolicyStatusForProjectAndVersion(projectName, projectVersionName)
        PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(versionBomPolicyStatusView)
        policyStatusDescription.getPolicyStatusMessage()
    }

    String getHubProjectVersionUIUrl(String projectName, String projectVersionName){
        HubServicesFactory hubServicesFactory = hubClient.getHubServicesFactory()
        ProjectService projectService = hubServicesFactory.createProjectService()
        ProjectVersionService projectVersionService = hubServicesFactory.createProjectVersionService()
        HubServerConfig hubServerConfig = hubClient.createBuilder().build()

        ProjectView project = projectService.getProjectByName(projectName)
        ProjectVersionView projectVersion = projectVersionService.getProjectVersion(project, projectVersionName)
        String projectVersionUIUrl = projectService.getFirstLinkSafely(projectVersion, "components")
        projectVersionUIUrl
    }
}
