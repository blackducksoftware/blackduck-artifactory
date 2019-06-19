package com.synopsys.integration.blackduck.artifactory.automation.test

abstract class TestSequence {
    abstract fun setup()
    abstract fun tearDown()
}

data class TestResult(val name: String, val passed: Boolean, val message: String? = null)