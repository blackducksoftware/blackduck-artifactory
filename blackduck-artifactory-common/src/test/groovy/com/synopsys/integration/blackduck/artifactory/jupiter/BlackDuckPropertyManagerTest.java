package com.synopsys.integration.blackduck.artifactory.jupiter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.artifactory.BlackDuckPropertyManager;
import com.synopsys.integration.blackduck.artifactory.ConfigurationProperty;
import com.synopsys.integration.blackduck.artifactory.TestUtil;
import com.synopsys.integration.blackduck.artifactory.jupiter.annotations.FileIO;

class BlackDuckPropertyManagerTest {
    private final ConfigurationProperty repositoryKeyListProperty = () -> "blackduck.artifactory.scan.repos";
    private final ConfigurationProperty repositoryKeyCsvProperty = () -> "blackduck.artifactory.scan.repos.csv.path";
    private final ConfigurationProperty isEnabledProperty = () -> "blackduck.artifactory.scan.enabled";
    private BlackDuckPropertyManager blackDuckPropertyManager;
    private Properties properties;

    @BeforeEach
    void init() throws IOException {
        properties = TestUtil.getDefaultProperties();
        blackDuckPropertyManager = new BlackDuckPropertyManager(properties);
    }

    @Test
    void getRepositoryKeysFromProperties() throws IOException {
        final List<String> repositoryKeysFromProperties = blackDuckPropertyManager.getRepositoryKeysFromProperties(repositoryKeyListProperty, repositoryKeyCsvProperty);
        assertAll("repo keys",
            () -> assertEquals(2, repositoryKeysFromProperties.size()),
            () -> assertTrue(repositoryKeysFromProperties.contains("ext-release-local")),
            () -> assertTrue(repositoryKeysFromProperties.contains("libs-release"))
        );

    }

    @Test
    @FileIO
    void getRepositoryKeysFromPropertiesCsv() throws IOException {
        blackDuckPropertyManager.getProperties().setProperty(repositoryKeyCsvProperty.getKey(), TestUtil.getResourceAsFilePath("/repoCSV"));
        final List<String> repositoryKeysFromProperties = blackDuckPropertyManager.getRepositoryKeysFromProperties(repositoryKeyListProperty, repositoryKeyCsvProperty);

        assertAll("repo keys",
            () -> assertEquals(7, repositoryKeysFromProperties.size()),
            () -> assertEquals("[test-repo1, test-repo2,  test-repo3, test-repo4 , test-repo5 ,  test-repo6, test-repo7]", Arrays.toString(repositoryKeysFromProperties.toArray()))
        );
    }

    @Test
    void getProperties() {
        assertEquals(properties, blackDuckPropertyManager.getProperties());
    }

    @Test
    void getProperty() {
        assertEquals("ext-release-local,libs-release", blackDuckPropertyManager.getProperty(repositoryKeyListProperty));
    }

    @Test
    void getBooleanProperty() {
        assertTrue(blackDuckPropertyManager.getBooleanProperty(isEnabledProperty));
    }
}