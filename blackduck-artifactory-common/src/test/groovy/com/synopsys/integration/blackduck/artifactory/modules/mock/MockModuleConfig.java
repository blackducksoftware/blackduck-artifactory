package com.synopsys.integration.blackduck.artifactory.modules.mock;

import org.junit.platform.commons.util.StringUtils;

import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;
import com.synopsys.integration.util.BuilderStatus;

public class MockModuleConfig extends ModuleConfig {
    private final String testField;

    public MockModuleConfig(final String moduleName, final Boolean enabled, final String testField) {
        super(moduleName, enabled);
        this.testField = testField;
    }

    @Override
    public void validate(final BuilderStatus builderStatus) {
        if (StringUtils.isBlank(testField)) {
            builderStatus.addErrorMessage("testField is blank");
        }
    }
}
