package com.synopsys.integration.blackduck.artifactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumingThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.commons.util.StringUtils;

import com.synopsys.integration.blackduck.artifactory.util.Fast;

class BlackDuckArtifactoryPropertyTest {
    @Test
    void getName() {
    }

    @ParameterizedTest
    @Fast
    @EnumSource(BlackDuckArtifactoryProperty.class)
    void getOldName(final BlackDuckArtifactoryProperty property) {
        final String name = property.getName();
        final String oldName = property.getOldName();

        assumingThat(name == null, () -> {
            assertNotNull(oldName);
            assertTrue(StringUtils.isNotBlank(oldName));
        });

        assumingThat(name != null, () -> {
            assertTrue(StringUtils.isNotBlank(name));

            assumingThat(oldName != null, () -> {
                assertTrue(StringUtils.isNotBlank(oldName));
            });
        });
    }
}