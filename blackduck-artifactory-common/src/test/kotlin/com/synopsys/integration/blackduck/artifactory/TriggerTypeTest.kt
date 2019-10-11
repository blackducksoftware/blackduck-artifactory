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