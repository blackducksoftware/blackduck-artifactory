package com.blackducksoftware.integration.hub.artifactory.inspect

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.api.item.MetaService
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
import com.blackducksoftware.integration.hub.service.HubServicesFactory
import com.blackducksoftware.integration.log.Slf4jIntLogger

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
        PolicyStatusDataService policyStatusDataService = hubServicesFactory.createPolicyStatusDataService(new Slf4jIntLogger(logger))
        VersionBomPolicyStatusView versionBomPolicyStatusView = policyStatusDataService.getPolicyStatusForProjectAndVersion(projectName, projectVersionName)
        versionBomPolicyStatusView.overallStatus
    }

    String getHubProjectPolicyStatus(String projectName, String projectVersionName){
        HubServicesFactory hubServicesFactory = hubClient.getHubServicesFactory()
        PolicyStatusDataService policyStatusDataService = hubServicesFactory.createPolicyStatusDataService(new Slf4jIntLogger(logger))
        VersionBomPolicyStatusView versionBomPolicyStatusView = policyStatusDataService.getPolicyStatusForProjectAndVersion(projectName, projectVersionName)
        PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(versionBomPolicyStatusView)
        policyStatusDescription.getPolicyStatusMessage()
    }

    String getHubProjectVersionUIUrl(String projectName, String projectVersionName){
        HubServicesFactory hubServicesFactory = hubClient.getHubServicesFactory()
        MetaService metaService = hubServicesFactory.createMetaService(new Slf4jIntLogger(logger))
        ProjectRequestService projectRequestService = hubServicesFactory.createProjectRequestService(new Slf4jIntLogger(logger))
        ProjectVersionRequestService projectVersionRequestService = hubServicesFactory.createProjectVersionRequestService(new Slf4jIntLogger(logger))
        HubServerConfig hubServerConfig = hubClient.createBuilder().build()

        ProjectView project = projectRequestService.getProjectByName(projectName)
        ProjectVersionView projectVersion = projectVersionRequestService.getProjectVersion(project, projectVersionName)
        String projectVersionUIUrl = ""
        try{
            String projectVersionUrl = metaService.getHref(projectVersion)
            String hubUrl = hubServerConfig.getHubUrl().toString()
            String versionId = projectVersionUrl.substring(projectVersionUrl.indexOf("/versions/") + "/versions/".length())
            projectVersionUIUrl = "${hubUrl}/ui/versions/id:${versionId}/view:bom"
        } catch(Exception e){
            logger.debug(e.getMessage())
        }
        projectVersionUIUrl
    }

}
