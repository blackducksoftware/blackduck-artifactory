package com.blackducksoftware.integration.hub.artifactory.inspect

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.api.bom.BomImportRequestService
import com.blackducksoftware.integration.hub.api.project.ProjectRequestService
import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperties
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder
import com.blackducksoftware.integration.hub.buildtool.BuildToolConstants
import com.blackducksoftware.integration.hub.dataservice.policystatus.PolicyStatusDataService
import com.blackducksoftware.integration.hub.dataservice.policystatus.PolicyStatusDescription
import com.blackducksoftware.integration.hub.dataservice.scan.ScanStatusDataService
import com.blackducksoftware.integration.hub.exception.HubIntegrationException
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.model.view.ProjectView
import com.blackducksoftware.integration.hub.model.view.VersionBomPolicyStatusView
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection
import com.blackducksoftware.integration.hub.service.HubServicesFactory
import com.blackducksoftware.integration.log.Slf4jIntLogger

@Component
class HubClient {
    private final Logger logger = LoggerFactory.getLogger(HubClient.class)

    @Autowired
    ConfigurationProperties configurationProperties

    @Autowired
    HubProjectDetails hubProjectDetails

    boolean isValid() {
        createBuilder().isValid()
    }

    void assertValid() throws IllegalStateException {
        createBuilder().build()
    }

    void testHubConnection() throws HubIntegrationException {
        HubServerConfig hubServerConfig = createBuilder().build()
        CredentialsRestConnection credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(logger))
        credentialsRestConnection.connect()
        logger.info('Successful connection to the Hub!')
    }

    void uploadBdioToHub(File bdioFile) {
        HubServerConfig hubServerConfig = createBuilder().build()
        CredentialsRestConnection credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(logger))
        HubServicesFactory hubServicesFactory = new HubServicesFactory(credentialsRestConnection)
        BomImportRequestService bomImportRequestService = hubServicesFactory.createBomImportRequestService()
        bomImportRequestService.importBomFile(bdioFile, BuildToolConstants.BDIO_FILE_MEDIA_TYPE)
    }

    private String getProjectIdByName(String projectName) {
        HubServerConfig hubServerConfig = createBuilder().build()
        CredentialsRestConnection credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(logger))
        HubServicesFactory hubServicesFactory = new HubServicesFactory(credentialsRestConnection)
        ProjectRequestService projectRequestService = hubServicesFactory.createProjectVersionRequestService(logger)
        ProjectView project = projectRequestService.getProjectByName(projectName)
    }

    private void getScanStatus(String projectName) {
        HubServerConfig hubServerConfig = createBuilder().build()
        CredentialsRestConnection credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(logger))
        HubServicesFactory hubServicesFactory = new HubServicesFactory(credentialsRestConnection)
        ScanStatusDataService scanStatusDataService = hubServicesFactory.createScanStatusDataService(logger, ScanStatusDataService.FIVE_SECONDS)
    }

    private String getPolicyStatus(String projectName, projectVersionName) {
        HubServerConfig hubServerConfig = createBuilder().build()
        CredentialsRestConnection credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(logger))
        HubServicesFactory hubServicesFactory = new HubServicesFactory(credentialsRestConnection)
        PolicyStatusDataService policyStatusDataService = hubServicesFactory.createPolicyStatusDataService(new Slf4jIntLogger(logger))
        ScanStatusDataService scanStatusDataService = hubServicesFactory.createScanStatusDataService(new Slf4jIntLogger(logger), ScanStatusDataService.DEFAULT_TIMEOUT)
        scanStatusDataService.assertBomImportScanStartedThenFinished(projectName, projectVersionName)
        VersionBomPolicyStatusView versionBomPolicyStatusView = policyStatusDataService.getPolicyStatusForProjectAndVersion(projectName, projectVersionName)
        PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(versionBomPolicyStatusView)
        policyStatusDescription.getPolicyStatusMessage()
    }

    private HubServerConfigBuilder createBuilder() {
        HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder()
        hubServerConfigBuilder.setHubUrl(configurationProperties.hubUrl)
        hubServerConfigBuilder.setUsername(configurationProperties.hubUsername)
        hubServerConfigBuilder.setPassword(configurationProperties.hubPassword)

        hubServerConfigBuilder.setTimeout(configurationProperties.hubTimeout)
        hubServerConfigBuilder.setProxyHost(configurationProperties.hubProxyHost)
        hubServerConfigBuilder.setProxyPort(configurationProperties.hubProxyPort)
        hubServerConfigBuilder.setProxyUsername(configurationProperties.hubProxyUsername)
        hubServerConfigBuilder.setProxyPassword(configurationProperties.hubProxyPassword)

        hubServerConfigBuilder
    }
}
