package com.blackducksoftware.integration.hub.artifactory.inspect

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.api.project.ProjectRequestService
import com.blackducksoftware.integration.hub.api.project.version.ProjectVersionRequestService
import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperties
import com.blackducksoftware.integration.hub.dataservice.policystatus.PolicyStatusDataService
import com.blackducksoftware.integration.hub.dataservice.policystatus.PolicyStatusDescription
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.model.enumeration.VersionBomPolicyStatusOverallStatusEnum
import com.blackducksoftware.integration.hub.model.view.ProjectVersionView
import com.blackducksoftware.integration.hub.model.view.ProjectView
import com.blackducksoftware.integration.hub.model.view.VersionBomPolicyStatusView
import com.blackducksoftware.integration.hub.service.HubResponseService
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
        HubResponseService hubResponseService = hubServicesFactory.createHubResponseService()
        ProjectRequestService projectRequestService = hubServicesFactory.createProjectRequestService()
        ProjectVersionRequestService projectVersionRequestService = hubServicesFactory.createProjectVersionRequestService()
        HubServerConfig hubServerConfig = hubClient.createBuilder().build()

        ProjectView project = projectRequestService.getProjectByName(projectName)
        ProjectVersionView projectVersion = projectVersionRequestService.getProjectVersion(project, projectVersionName)
        String projectVersionUIUrl = hubResponseService.getFirstLinkSafely(projectVersion, "components")
        projectVersionUIUrl
    }
}
