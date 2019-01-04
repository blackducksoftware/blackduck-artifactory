package com.synopsys.integration.blackduck.artifactory.modules.policy;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.util.BuilderStatus;

class PolicyModuleConfigTest {
    private PolicyModuleConfig validPolicyModuleConfig;
    private PolicyModuleConfig invalidPolicyModuleConfig;

    @BeforeEach
    void init() {
        validPolicyModuleConfig = new PolicyModuleConfig(true, false);
        invalidPolicyModuleConfig = new PolicyModuleConfig(null, null);
    }

    @Test
    void validate() {
        final BuilderStatus validBuilderStatus = new BuilderStatus();
        validPolicyModuleConfig.validate(validBuilderStatus);
        Assert.assertEquals(1, validBuilderStatus.getErrorMessages().size()); // TODO: This set expected value to zero when the metadata block feature is re-enabled or removed

        final BuilderStatus invalidBuilderStatus = new BuilderStatus();
        invalidPolicyModuleConfig.validate(invalidBuilderStatus);
        Assert.assertEquals(1, invalidBuilderStatus.getErrorMessages().size()); // TODO: This set expected value to two when the metadata block feature is re-enabled or removed
    }

    @Test
    void isEnabled() {
        Assert.assertTrue(validPolicyModuleConfig.isEnabled());
        Assert.assertFalse(invalidPolicyModuleConfig.isEnabled());
    }

    @Test
    void isEnabledUnverified() {
        Assert.assertTrue(validPolicyModuleConfig.isEnabledUnverified());
        Assert.assertNull(invalidPolicyModuleConfig.isEnabledUnverified());
    }

    @Test
    void isMetadataBlockEnabled() {
        Assert.assertFalse(validPolicyModuleConfig.isMetadataBlockEnabled());
        Assert.assertNull(invalidPolicyModuleConfig.isMetadataBlockEnabled());
    }
}