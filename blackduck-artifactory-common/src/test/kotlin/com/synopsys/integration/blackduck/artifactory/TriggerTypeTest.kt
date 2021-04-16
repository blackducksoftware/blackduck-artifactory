/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class TriggerTypeTest {
    @ParameterizedTest
    @EnumSource(TriggerType::class)
    fun triggerTypeHasLogName(triggerType: TriggerType) {
        Assertions.assertTrue(triggerType.logName.isNotBlank(), "A TriggerType must have a log name.")
    }
}