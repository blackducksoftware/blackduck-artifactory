package com.synopsys.integration.blackduck.artifactory.jupiter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.artifactory.PluginConfig;

class PluginConfigTest {
    private PluginConfig pluginConfig;

    @BeforeEach
    void setUp() {
        final File homeDirectory = new File("home");
        final File etcDirectory = new File("etc");
        final File pluginsDirectory = new File("plugins");

        pluginConfig = PluginConfig.createDefault(homeDirectory, etcDirectory, pluginsDirectory, "1.2.3", "path/to/properties/file");
    }

    @Test
    void getHomeDirectory() {
        assertEquals(new File("home"), pluginConfig.getHomeDirectory());
    }

    @Test
    void getEtcDirectory() {
        assertEquals(new File("etc"), pluginConfig.getEtcDirectory());
    }

    @Test
    void getPluginsLibDirectory() {
        assertEquals(new File("plugins", "lib"), pluginConfig.getPluginsLibDirectory());
    }

    @Test
    void getVersionFile() {
        assertEquals(new File(pluginConfig.getPluginsLibDirectory(), "version.txt"), pluginConfig.getVersionFile());
    }

    @Test
    void getThirdPartyVersion() {
        assertEquals("1.2.3", pluginConfig.getThirdPartyVersion());
    }

    @Test
    void getPropertiesFilePathOverride() {
        assertEquals("path/to/properties/file", pluginConfig.getPropertiesFilePathOverride());
    }
}