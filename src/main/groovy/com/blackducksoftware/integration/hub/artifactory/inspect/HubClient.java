package com.blackducksoftware.integration.hub.artifactory.inspect;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.bom.BomImportService;
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryRestClient;
import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperties;
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.dataservice.phonehome.PhoneHomeDataService;
import com.blackducksoftware.integration.hub.dataservice.scan.ScanStatusDataService;
import com.blackducksoftware.integration.hub.exception.HubTimeoutExceededException;
import com.blackducksoftware.integration.hub.global.HubServerConfig;
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection;
import com.blackducksoftware.integration.hub.service.HubServicesFactory;
import com.blackducksoftware.integration.log.Slf4jIntLogger;
import com.blackducksoftware.integration.phonehome.PhoneHomeRequestBody;
import com.blackducksoftware.integration.phonehome.PhoneHomeRequestBodyBuilder;
import com.blackducksoftware.integration.phonehome.enums.ThirdPartyName;

@Component
public class HubClient {
    private final Logger logger = LoggerFactory.getLogger(HubClient.class);

    @Autowired
    private ConfigurationProperties configurationProperties;

    @Autowired
    private ArtifactoryRestClient artifactoryRestClient;

    public boolean isValid() {
        return createBuilder().isValid();
    }

    public void assertValid() throws IllegalStateException {
        createBuilder().build();
    }

    public void testHubConnection() throws IntegrationException {
        final HubServerConfig hubServerConfig = createBuilder().build();
        final CredentialsRestConnection credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(logger));
        credentialsRestConnection.connect();
        logger.info("Successful connection to the Hub!");
    }

    public void uploadBdioToHub(final File bdioFile) throws IntegrationException {
        final BomImportService bomImportService = getHubServicesFactory().createBomImportService();
        bomImportService.importBomFile(bdioFile);
    }

    public void waitForBomCalculation(final String projectName, final String projectVersionName) throws IntegrationException {
        final ScanStatusDataService scanStatusDataService = getHubServicesFactory().createScanStatusDataService(300000L);
        try {
            scanStatusDataService.assertBomImportScanStartedThenFinished(projectName, projectVersionName);
        } catch (final HubTimeoutExceededException e) {
            logger.info(e.getMessage());
            logger.info("Checking project in the Hub to ensure it has no pending scans");
            scanStatusDataService.assertScansFinished(projectName, projectVersionName);
        }
    }

    public HubServicesFactory getHubServicesFactory() throws EncryptionException {
        final HubServerConfig hubServerConfig = this.createBuilder().build();
        final CredentialsRestConnection credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(logger));
        return new HubServicesFactory(credentialsRestConnection);
    }

    private HubServerConfigBuilder createBuilder() {
        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        hubServerConfigBuilder.setHubUrl(configurationProperties.getHubUrl());
        hubServerConfigBuilder.setUsername(configurationProperties.getHubUsername());
        hubServerConfigBuilder.setPassword(configurationProperties.getHubPassword());

        hubServerConfigBuilder.setTimeout(configurationProperties.getHubTimeout());
        hubServerConfigBuilder.setProxyHost(configurationProperties.getHubProxyHost());
        hubServerConfigBuilder.setProxyPort(configurationProperties.getHubProxyPort());
        hubServerConfigBuilder.setProxyUsername(configurationProperties.getHubProxyUsername());
        hubServerConfigBuilder.setProxyPassword(configurationProperties.getHubProxyPassword());

        hubServerConfigBuilder.setAlwaysTrustServerCertificate(Boolean.parseBoolean(configurationProperties.getHubAlwaysTrustCerts()));

        return hubServerConfigBuilder;
    }

    public void phoneHome() throws EncryptionException {
        final PhoneHomeDataService phoneHomeDataService = getHubServicesFactory().createPhoneHomeDataService();
        PhoneHomeRequestBody phoneHomeRequestBody = PhoneHomeRequestBody.DO_NOT_PHONE_HOME;
        try {
            final PhoneHomeRequestBodyBuilder phoneHomeRequestBodyBuilder = phoneHomeDataService.createInitialPhoneHomeRequestBodyBuilder();
            phoneHomeRequestBodyBuilder.setThirdPartyName(ThirdPartyName.ARTIFACTORY);
            phoneHomeRequestBodyBuilder.setThirdPartyVersion(artifactoryRestClient.getVersionInfoForArtifactory());
            final String pluginVersion = IOUtils.toString(getClass().getResourceAsStream("version.txt"), StandardCharsets.UTF_8);
            phoneHomeRequestBodyBuilder.setPluginVersion(pluginVersion);
            phoneHomeRequestBodyBuilder.addToMetaDataMap("mode", "inspector");
            phoneHomeRequestBody = phoneHomeRequestBodyBuilder.build();
        } catch (final Exception e) {
        }
        phoneHomeDataService.phoneHome(phoneHomeRequestBody);
    }

}
