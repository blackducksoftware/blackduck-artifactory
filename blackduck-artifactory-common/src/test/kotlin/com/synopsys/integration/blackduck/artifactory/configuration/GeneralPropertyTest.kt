/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.configuration

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.platform.commons.util.StringUtils

class GeneralPropertyTest {
    @ParameterizedTest
    @EnumSource(GeneralProperty::class)
    fun getKey(property: GeneralProperty) {
        assertNotNull(property.key, "A GeneralProperty must have a key.")
        assertTrue(StringUtils.isNotBlank(property.key), "A GeneralProperty key must not be blank.")
    }
}