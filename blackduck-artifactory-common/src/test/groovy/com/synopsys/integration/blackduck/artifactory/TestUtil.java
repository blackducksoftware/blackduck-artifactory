package com.synopsys.integration.blackduck.artifactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TestUtil {
    public static Properties getDefaultProperties() throws IOException {
        return getResourceAsProperties("/blackDuckPlugin.properties");
    }

    public static Properties getResourceAsProperties(final String resourcePath) throws IOException {
        final Properties properties = new Properties();
        try (final InputStream inputStream = TestUtil.class.getResourceAsStream(resourcePath)) {
            properties.load(inputStream);
        }

        return properties;
    }

    public static String getResourceAsFilePath(final String resourcePath) {
        return TestUtil.class.getResource(resourcePath).getFile();
    }

    public static File getResourceAsFile(final String resourcePath) {
        return new File(getResourceAsFilePath(resourcePath));
    }
}
