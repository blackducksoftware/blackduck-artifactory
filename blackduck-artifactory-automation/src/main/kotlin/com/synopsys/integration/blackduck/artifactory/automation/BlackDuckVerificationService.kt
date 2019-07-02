package com.synopsys.integration.blackduck.artifactory.automation

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory
import org.junit.jupiter.api.Assertions

class BlackDuckVerificationService(private val blackDuckServicesFactory: BlackDuckServicesFactory) {
    fun verifyComponentsExists(projectVersionView: ProjectVersionView, testablePackages: List<TestablePackage>) {
        verifyComponentsExists(projectVersionView, *testablePackages.toTypedArray())
    }

    fun verifyComponentsExists(projectVersionView: ProjectVersionView, vararg testablePackages: TestablePackage) {
        val projectBomService = blackDuckServicesFactory.createProjectBomService()
        val versionBomComponentViews = projectBomService.getComponentsForProjectVersion(projectVersionView)
        val foundComponents = versionBomComponentViews.map { Component(it.componentName, it.componentVersion) }
        val expectedComponents = testablePackages.map { Component(it.externalId.name, it.externalId.version) }

        Assertions.assertTrue(foundComponents.containsAll(expectedComponents))
    }
}

data class Component(val name: String, val version: String) 