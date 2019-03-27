package com.synopsys.integration.blackduck.artifactory.modules.analytics;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.builder.BuilderStatus;

class AnalyticsModuleConfigTest {
    private AnalyticsModuleConfig validAnalyticsModuleConfig;
    private AnalyticsModuleConfig invalidAnalyticsModuleConfig;

    @BeforeEach
    void init() {
        validAnalyticsModuleConfig = new AnalyticsModuleConfig(true);
        invalidAnalyticsModuleConfig = new AnalyticsModuleConfig(null);
    }

    @Test
    void validate() {
        final BuilderStatus validBuilderStatus = new BuilderStatus();
        validAnalyticsModuleConfig.validate(validBuilderStatus);
        Assert.assertEquals(0, validBuilderStatus.getErrorMessages().size());

        final BuilderStatus invalidBuilderStatus = new BuilderStatus();
        invalidAnalyticsModuleConfig.validate(invalidBuilderStatus);
        Assert.assertEquals(1, invalidBuilderStatus.getErrorMessages().size());
    }

    @Test
    void isEnabled() {
        Assert.assertTrue(validAnalyticsModuleConfig.isEnabled());
        Assert.assertFalse(invalidAnalyticsModuleConfig.isEnabled());
    }

    @Test
    void isEnabledUnverified() {
        Assert.assertTrue(validAnalyticsModuleConfig.isEnabledUnverified());
        Assert.assertNull(invalidAnalyticsModuleConfig.isEnabledUnverified());
    }
}