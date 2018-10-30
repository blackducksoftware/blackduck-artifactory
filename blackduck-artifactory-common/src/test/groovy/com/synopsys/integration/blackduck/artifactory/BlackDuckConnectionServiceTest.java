package com.synopsys.integration.blackduck.artifactory;

import static com.synopsys.integration.blackduck.artifactory.util.TestUtil.getHubServerConfigFromEnvVar;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionDistributionType;
import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionPhaseType;
import com.synopsys.integration.blackduck.api.generated.view.CodeLocationView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.artifactory.util.BlackDuckIntegrationTest;
import com.synopsys.integration.blackduck.artifactory.util.FileIO;
import com.synopsys.integration.blackduck.artifactory.util.TestUtil;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.exception.DoesNotExistException;
import com.synopsys.integration.blackduck.service.CodeLocationService;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.ProjectRequestBuilder;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.EncryptionException;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.BdioReader;
import com.synopsys.integration.hub.bdio.BdioWriter;
import com.synopsys.integration.hub.bdio.model.Forge;
import com.synopsys.integration.hub.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;
import com.synopsys.integration.phonehome.google.analytics.GoogleAnalyticsConstants;

// TODO: Replace with scenario https://github.com/junit-team/junit5/issues/48
@FileIO
@BlackDuckIntegrationTest
class BlackDuckConnectionServiceTest {
    private BlackDuckConnectionService blackDuckConnectionService;
    private String generatedProjectName;

    @BeforeEach
    void setUp() throws EncryptionException {
        final File versionFile = TestUtil.getResourceAsFile("/version.txt");
        final PluginConfig pluginConfig = new PluginConfig(null, null, null, versionFile, null, null);
        final HubServerConfig hubServerConfig = getHubServerConfigFromEnvVar();
        blackDuckConnectionService = new BlackDuckConnectionService(pluginConfig, hubServerConfig, GoogleAnalyticsConstants.TEST_INTEGRATIONS_TRACKING_ID);

        final int randomNumber = new Random().nextInt();
        generatedProjectName = String.format("IARTH-BlackDuckConnectionService-%d", randomNumber);
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
     */
    @Test
    void importBomFile() throws IntegrationException, IOException {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final String codelocationName = String.format("%s-codelocation", generatedProjectName);
        final InputStream inputStream = TestUtil.getResourceAsStream("/test-project-bdio.jsonld");
        final BdioReader bdioReader = new BdioReader(gson, inputStream);
        final SimpleBdioDocument simpleBdioDocument = bdioReader.readSimpleBdioDocument();
        simpleBdioDocument.project.name = generatedProjectName;
        simpleBdioDocument.billOfMaterials.spdxName = codelocationName;

        final File bdioFile = File.createTempFile("iarth", null);
        final OutputStream outputStream = new FileOutputStream(bdioFile);
        final BdioWriter bdioWriter = new BdioWriter(gson, outputStream);
        bdioWriter.writeSimpleBdioDocument(simpleBdioDocument);
        bdioWriter.close();

        final CodeLocationService codeLocationService = blackDuckConnectionService.getHubServicesFactory().createCodeLocationService();
        final ProjectService projectService = blackDuckConnectionService.getHubServicesFactory().createProjectService();

        blackDuckConnectionService.importBomFile(bdioFile);
        assertTimeout(Duration.ofSeconds(5), () -> waitForProjectCreation(projectService, generatedProjectName, 500));

        final ProjectView projectView = projectService.getProjectByName(generatedProjectName);
        assertNotNull(generatedProjectName);

        final CodeLocationView codeLocationView = codeLocationService.getCodeLocationByName(codelocationName);
        assertNotNull(codeLocationView);

        codeLocationService.deleteCodeLocation(codeLocationView);
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
        projectRequestBuilder.setProjectName(generatedProjectName);
        final String projectVersionName = "test-version";
        projectRequestBuilder.setVersionName(projectVersionName);
        projectRequestBuilder.setDistribution(ProjectVersionDistributionType.OPENSOURCE);
        projectRequestBuilder.setPhase(ProjectVersionPhaseType.PRERELEASE);
        projectRequestBuilder.setReleasedOn(new Date().toString());
        projectRequestBuilder.createValidator().assertValid();

        projectService.createHubProject(projectRequestBuilder.buildObject());
        final ProjectView projectView = projectService.getProjectByName(generatedProjectName);

        assertNotNull(projectView);

        blackDuckConnectionService.addComponentToProjectVersion(externalId, generatedProjectName, projectVersionName);

        final ProjectVersionWrapper projectVersionWrapper = projectService.getProjectVersion(generatedProjectName, projectVersionName);
        final List<VersionBomComponentView> componentsForProjectVersion = projectService.getComponentsForProjectVersion(projectVersionWrapper.getProjectVersionView());

        System.out.println(String.format("Temp project name: %s", generatedProjectName));
        assertAll("component added", () -> {
            assertEquals(1, componentsForProjectVersion.size(), "too many components in project");
            assertTrue(componentsForProjectVersion.stream()
                           .map(versionBomComponentView -> versionBomComponentView.origins)
                           .flatMap(List::stream)
                           .map(versionBomOriginView -> versionBomOriginView.externalId)
                           .anyMatch(foundExternalId -> externalId.createHubOriginId().equals(foundExternalId)), "components did not match");
        });

        projectService.deleteHubProject(projectView);
    }

    @Test
    void getHubServicesFactory() {
        assertNotNull(blackDuckConnectionService.getHubServicesFactory());
    }

    private void waitForProjectCreation(final ProjectService projectService, final String projectName, final long period) throws IntegrationException {
        long elapsedTime = 0L;

        while (elapsedTime < 300000) {
            try {
                projectService.getProjectByName(projectName);
                break; // Retrieving the code location succeeded
            } catch (final DoesNotExistException ignored) {
                // Retrieving the code location failed
                elapsedTime += period;
            }
        }
    }
}