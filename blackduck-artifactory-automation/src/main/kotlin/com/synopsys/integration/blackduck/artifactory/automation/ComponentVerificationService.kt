package com.synopsys.integration.blackduck.artifactory.automation

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.RepositoryManager
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.Repository
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.artifacts.PropertiesApiService
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.searches.ArtifactSearchesAPIService
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionStatus
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory
import com.synopsys.integration.exception.IntegrationException
import com.synopsys.integration.log.Slf4jIntLogger
import org.junit.jupiter.api.Assertions
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class ComponentVerificationService(private val blackDuckServicesFactory: BlackDuckServicesFactory, private val propertiesApiService: PropertiesApiService, private val artifactSearchesAPIService: ArtifactSearchesAPIService) {
    private val logger = Slf4jIntLogger(LoggerFactory.getLogger(this.javaClass))

    fun waitForComponentInspection(repository: Repository, testablePackage: TestablePackage, expectedInspectionStatus: InspectionStatus = InspectionStatus.SUCCESS, maxRetryCount: Int = 5, waitTime: Long = 1,
        waitTimeUnit: TimeUnit = TimeUnit.MINUTES) {
        val repoKey = RepositoryManager.determineRepositoryKey(repository)
        val artifact = artifactSearchesAPIService.exactArtifactSearch(testablePackage.artifactoryFileName, repoKey)
        waitForComponentInspection(repository.key + artifact.path, expectedInspectionStatus, maxRetryCount, waitTime, waitTimeUnit, 0)
    }

    private fun waitForComponentInspection(repoPath: String, expectedInspectionStatus: InspectionStatus, maxRetryCount: Int, waitTime: Long, waitTimeUnit: TimeUnit, currentRetryCount: Int) {
        val itemProperties = propertiesApiService.getProperties(repoPath)

        fun retry() {
            logger.info("Waiting for inspection. Waiting for $waitTime ${waitTimeUnit.name.toLowerCase()}.")
            Thread.sleep(waitTimeUnit.toMillis(waitTime))
            waitForComponentInspection(repoPath, expectedInspectionStatus, maxRetryCount, waitTime, waitTimeUnit, currentRetryCount + 1)
        }

        if (itemProperties == null && currentRetryCount < maxRetryCount) {
            retry()
        } else {
            val inspectionStatusPropertyKey = BlackDuckArtifactoryProperty.INSPECTION_STATUS.getName()
            val inspectionStatus = itemProperties?.properties?.get(inspectionStatusPropertyKey)?.first()

            if (inspectionStatus == null) {
                retry()
            } else if (inspectionStatus != expectedInspectionStatus.name) {
                val retryCountPropertyKey = BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT.getName()
                val retryCount = itemProperties.properties[retryCountPropertyKey]?.first()?.toInt() ?: throw MissingPropertyException(retryCountPropertyKey, repoPath)

                when {
                    retryCount == maxRetryCount -> throw FailedInspectionException(repoPath)
                    retryCount >= maxRetryCount -> throw FailedInspectionException(repoPath, "Retry count exceeded maximum of $maxRetryCount on $repoPath.")
                    currentRetryCount < maxRetryCount -> retry()
                }
            }
        }
    }

    fun verifyComponentExistsInBOM(projectVersionView: ProjectVersionView, testablePackage: TestablePackage) {
        verifyComponentsExistInBOM(projectVersionView, listOf(testablePackage))
    }

    fun verifyComponentsExistInBOM(projectVersionView: ProjectVersionView, testablePackages: List<TestablePackage>) {
        val projectBomService = blackDuckServicesFactory.createProjectBomService()
        val versionBomComponentViews = projectBomService.getComponentsForProjectVersion(projectVersionView)
        val foundComponents = versionBomComponentViews.flatMap { it.origins }.map { it.externalId }
        val expectedComponents = testablePackages.map { it.externalId.createExternalId() }

        Assertions.assertTrue(foundComponents.containsAll(expectedComponents), "Not all expected components were found. Expected: $expectedComponents Found: $foundComponents")
    }
}

class FailedInspectionException(
    private val repoPath: String,
    override val message: String = "The plugin failed to inspect $repoPath."
) : IntegrationException()

class NoPropertiesException(
    private val repoPath: String,
    override var message: String = "Failed to find any properties on $repoPath."
) : IntegrationException()

class MissingPropertyException(
    private val propertyKey: String,
    private val repoPath: String,
    override val message: String = "Failed to find the property $propertyKey at $repoPath."
) : IntegrationException()

data class Component(val name: String, val version: String) 