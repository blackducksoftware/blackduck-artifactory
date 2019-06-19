package com.synopsys.integration.blackduck.artifactory.automation.test

class TestRunner(private val testSequences: List<TestSequence> = mutableListOf()) {
    fun runTests(): List<TestResult> {
        val testResults = mutableListOf<TestResult>()

        testSequences.forEach { testSequence ->
            testSequence.javaClass.methods
                .filter { it.isAnnotationPresent(Test::class.java) }
                .map { it(testSequence) }
                .forEach { testResults.add(it as TestResult) }
        }

        return testResults
    }
}
