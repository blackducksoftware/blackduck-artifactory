package com.synopsys.integration.blackduck.artifactory

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BlackDuckArtifactoryPropertyTest {
    @ParameterizedTest
    @EnumSource(BlackDuckArtifactoryProperty::class)
    fun propertyHasProperPrefix(blackDuckArtifactoryProperty: BlackDuckArtifactoryProperty) {
        Assertions.assertTrue(blackDuckArtifactoryProperty.propertyName.startsWith("blackduck."), "A BlackDuckArtifactoryProperty must have a blackduck prefix.")
        Assertions.assertTrue(blackDuckArtifactoryProperty.timeName.startsWith("blackduck."), "A BlackDuckArtifactoryProperty must have a blackduck prefix.")
    }
}