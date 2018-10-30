package com.synopsys.integration.blackduck.artifactory;

import static com.synopsys.integration.blackduck.artifactory.util.TestUtil.getHubServerConfigFromEnvVar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionDistributionType;
import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionPhaseType;
import com.synopsys.integration.blackduck.api.generated.view.ProjectView;
import com.synopsys.integration.blackduck.artifactory.util.BlackDuckIntegrationTest;
import com.synopsys.integration.blackduck.artifactory.util.FileIO;
import com.synopsys.integration.blackduck.artifactory.util.TestUtil;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.ProjectRequestBuilder;
import com.synopsys.integration.exception.EncryptionException;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.model.Forge;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;
import com.synopsys.integration.phonehome.google.analytics.GoogleAnalyticsConstants;

// TODO: Replace with scenario https://github.com/junit-team/junit5/issues/48
@FileIO
@BlackDuckIntegrationTest
class BlackDuckConnectionServiceTest {
    private BlackDuckConnectionService blackDuckConnectionService;
    private String projectName;

    @BeforeEach
    void setUp() throws EncryptionException {
        final File versionFile = TestUtil.getResourceAsFile("/version.txt");
        final PluginConfig pluginConfig = new PluginConfig(null, null, null, versionFile, "1.2.3", TestUtil.DEFAULT_PROPERTIES_RESOURCE_PATH);
        final HubServerConfig hubServerConfig = getHubServerConfigFromEnvVar();
        blackDuckConnectionService = new BlackDuckConnectionService(pluginConfig, hubServerConfig, GoogleAnalyticsConstants.TEST_INTEGRATIONS_TRACKING_ID);

        final int randomNumber = new Random().nextInt();
        projectName = String.format("iarth-blackDuckConnectionService-%d", randomNumber);
    }

    @Test
    void phoneHome() {
        final Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("testKey1", "test1");
        metadataMap.put("testKey2", "test2");

        final boolean success = blackDuckConnectionService.phoneHome(metadataMap);
        assertTrue(success);
    }

    /**
     * Tests the uploading of a bdio document. Will occasionally fail because BlackDuck has not yet created a project.
     * If the test fails. Attempt to rerun a few seconds later while BlackDuck processes the bdio.
     * @throws IntegrationException
     */
    @Test
    void importBomFile() throws IntegrationException {
        final File bdioFile = TestUtil.getResourceAsFile("/test-project-bdio.jsonld");
        blackDuckConnectionService.importBomFile(bdioFile);

        final ProjectService projectService = blackDuckConnectionService.getHubServicesFactory().createProjectService();
        final ProjectView projectView = projectService.getProjectByName("test-project");

        assertNotNull(projectView);
        assertEquals("test-project", projectView.name);

        projectService.deleteHubProject(projectView);
    }

    @Test
    void addComponentToProjectVersion() throws IntegrationException {
        final ExternalId externalId = new ExternalId(Forge.MAVEN);
        externalId.group = "com.blackducksoftware.integration";
        externalId.name = "hub-common";
        externalId.version = "38.7.0";

        final ProjectService projectService = blackDuckConnectionService.getHubServicesFactory().createProjectService();
        final ProjectRequestBuilder projectRequestBuilder = new ProjectRequestBuilder();
        projectRequestBuilder.setProjectName(projectName);
        projectRequestBuilder.setVersionName("test-version");
        projectRequestBuilder.setDistribution(ProjectVersionDistributionType.OPENSOURCE);
        projectRequestBuilder.setPhase(ProjectVersionPhaseType.PRERELEASE);
        projectRequestBuilder.setReleasedOn(new Date().toString());

        projectRequestBuilder.createValidator().assertValid();

        projectService.createHubProject(projectRequestBuilder.buildObject());
        final ProjectView projectView = projectService.getProjectByName(projectName);

        assertNotNull(projectView);

        projectService.deleteHubProject(projectView);
    }

    @Test
    void getHubServicesFactory() {
        assertNotNull(blackDuckConnectionService.getHubServicesFactory());
    }
}