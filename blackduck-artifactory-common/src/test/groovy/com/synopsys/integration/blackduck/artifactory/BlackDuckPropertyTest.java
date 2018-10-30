package com.synopsys.integration.blackduck.artifactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.commons.util.StringUtils;

import com.synopsys.integration.blackduck.artifactory.util.Fast;

class BlackDuckPropertyTest {
    @ParameterizedTest
    @Fast
    @EnumSource(BlackDuckProperty.class)
    void getKey(final BlackDuckProperty property) {
        assertNotNull(property.getKey());
        assertTrue(StringUtils.isNotBlank(property.getKey()));
    }
}