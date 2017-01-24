package com.blackducksoftware.integration.hub.artifactory.inspect

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.api.bom.BomImportRequestService
import com.blackducksoftware.integration.hub.artifactory.ConfigurationManager
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder
import com.blackducksoftware.integration.hub.buildtool.BuildToolConstants
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection
import com.blackducksoftware.integration.hub.service.HubServicesFactory

@Component
class HubUploader {
    private final Logger logger = LoggerFactory.getLogger(HubUploader.class)

    @Autowired
    ConfigurationManager configurationManager

    boolean isValid() {
        try {
            createBuilder().build()
        } catch (Exception e){
            logger.warn("Hub Server Config is not valid: ${e.message}")
            return false
        }

        true
    }

    void uploadBdioToHub(File bdioFile) {
        HubServerConfig hubServerConfig = createBuilder().build()

        CredentialsRestConnection credentialsRestConnection = new CredentialsRestConnection(hubServerConfig)
        HubServicesFactory hubServicesFactory = new HubServicesFactory(credentialsRestConnection)
        BomImportRequestService bomImportRequestService = hubServicesFactory.createBomImportRequestService()
        bomImportRequestService.importBomFile(bdioFile, BuildToolConstants.BDIO_FILE_MEDIA_TYPE)
    }

    private HubServerConfigBuilder createBuilder() {
        HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder()
        hubServerConfigBuilder.setHubUrl(configurationManager.hubUrl)
        hubServerConfigBuilder.setUsername(configurationManager.hubUsername)
        hubServerConfigBuilder.setPassword(configurationManager.hubPassword)
        hubServerConfigBuilder.setTimeout(configurationManager.hubTimeout)
        hubServerConfigBuilder.setProxyHost(configurationManager.hubProxyHost)
        hubServerConfigBuilder.setProxyPort(configurationManager.hubProxyPort)
        hubServerConfigBuilder.setIgnoredProxyHosts(configurationManager.hubProxyIgnoredProxyHosts)
        hubServerConfigBuilder.setProxyUsername(configurationManager.hubProxyUsername)
        hubServerConfigBuilder.setProxyPassword(configurationManager.hubProxyPassword)

        hubServerConfigBuilder
    }
}
