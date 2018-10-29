package com.synopsys.integration.blackduck.artifactory.legacy;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import com.synopsys.integration.blackduck.configuration.HubServerConfigBuilder;

// TODO: Investigate the usefulness of this test
public class LoadHubServerConfigFromPropertiesTest {
    @Test
    public void loadFromProperties() throws IOException {
        final Properties properties = new Properties();
        try (final FileInputStream fileInputStream = new FileInputStream("src/test/resources/blackDuckCacheInspector.properties")) {
            properties.load(fileInputStream);
        }

        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        hubServerConfigBuilder.setFromProperties(properties);
    }
}
