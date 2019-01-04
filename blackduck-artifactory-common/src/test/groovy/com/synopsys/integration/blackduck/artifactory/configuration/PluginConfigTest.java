package com.synopsys.integration.blackduck.artifactory.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.artifactory.util.BlackDuckIntegrationTest;
import com.synopsys.integration.blackduck.artifactory.util.TestUtil;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.util.BuilderStatus;

class PluginConfigTest {
    @BlackDuckIntegrationTest
    void validate() {
        final BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = TestUtil.getBlackDuckServerConfigBuilderFromEnvVar();

        final PluginConfig validPluginConfig = new PluginConfig("yyyy-MM-dd'T'HH:mm:ss.SSS", blackDuckServerConfigBuilder);
        final BuilderStatus validBuilderStatus = new BuilderStatus();
        validPluginConfig.validate(validBuilderStatus);
        assertEquals(0, validBuilderStatus.getErrorMessages().size());

        final PluginConfig invalidPluginConfig = new PluginConfig(null, new BlackDuckServerConfigBuilder());
        final BuilderStatus invalidBuilderStatus = new BuilderStatus();
        invalidPluginConfig.validate(invalidBuilderStatus);
        assertEquals(3, invalidBuilderStatus.getErrorMessages().size());

        final PluginConfig invalidPluginConfig2 = new PluginConfig("this is an invalid time format", new BlackDuckServerConfigBuilder());
        final BuilderStatus invalidBuilderStatus2 = new BuilderStatus();
        invalidPluginConfig2.validate(invalidBuilderStatus2);
        assertEquals(3, invalidBuilderStatus2.getErrorMessages().size());
    }

    @Test
    void getDateTimePattern() {
        final PluginConfig pluginConfig = new PluginConfig("yyyy-MM-dd'T'HH:mm:ss.SSS", null);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSS", pluginConfig.getDateTimePattern());
    }

    @Test
    void getBlackDuckServerConfigBuilder() {
        final PluginConfig pluginConfig = new PluginConfig(null, null);
        assertNull(pluginConfig.getBlackDuckServerConfigBuilder());
    }
}