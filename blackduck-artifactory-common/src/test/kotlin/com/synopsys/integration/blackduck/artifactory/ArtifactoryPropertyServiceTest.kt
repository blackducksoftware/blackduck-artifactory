package com.synopsys.integration.blackduck.artifactory

import TestUtil.createMockArtifactoryPAPIService
import TestUtil.createRepoPath
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.synopsys.integration.log.Slf4jIntLogger
import org.artifactory.repo.RepoPath
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*

internal class ArtifactoryPropertyServiceTest {

    @Test
    fun hasProperty() {
        val repoPath = createRepoPath()
        val repoPathPropertyMap = mutableMapOf(
                repoPath to mutableMapOf(
                        BlackDuckArtifactoryProperty.LAST_UPDATE.propertyName to "last updated time"
                )
        )
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val artifactoryPropertyService = ArtifactoryPropertyService(artifactoryPAPIService, mock())

        Assertions.assertTrue(artifactoryPropertyService.hasProperty(repoPath, BlackDuckArtifactoryProperty.LAST_UPDATE), "The ${BlackDuckArtifactoryProperty.LAST_UPDATE.propertyName} should exist.")
        Assertions.assertFalse(artifactoryPropertyService.hasProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS), "The ${BlackDuckArtifactoryProperty.LAST_UPDATE.propertyName} should be the only property to exist.")
    }

    @Test
    fun getProperty() {
        val repoPath = createRepoPath()
        val expectedPropertyValue = "last updated time"
        val property = BlackDuckArtifactoryProperty.LAST_UPDATE
        val repoPathPropertyMap = mutableMapOf(
                repoPath to mutableMapOf(
                        property.propertyName to expectedPropertyValue
                )
        )
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val artifactoryPropertyService = ArtifactoryPropertyService(artifactoryPAPIService, mock())

        val actualPropertyValue = artifactoryPropertyService.getProperty(repoPath, property)
        Assertions.assertNotNull(actualPropertyValue, "The ${property.propertyName} property should exist.")
        Assertions.assertEquals(expectedPropertyValue, actualPropertyValue, "The retrieved property value should be $expectedPropertyValue.")

        val missingPropertyValue = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS)
        Assertions.assertNull(missingPropertyValue, "The retrieved property should be missing.")
    }

    @Test
    fun getPropertyAsInteger() {
        val repoPath = createRepoPath()
        val expectedPropertyValue = 3
        val property = BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT
        val repoPathPropertyMap = mutableMapOf(
                repoPath to mutableMapOf(
                        property.propertyName to expectedPropertyValue.toString()
                )
        )
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val artifactoryPropertyService = ArtifactoryPropertyService(artifactoryPAPIService, mock())

        val actualPropertyValue = artifactoryPropertyService.getPropertyAsInteger(repoPath, property)
        Assertions.assertNotNull(actualPropertyValue, "The ${property.propertyName} property should exist.")
        Assertions.assertEquals(expectedPropertyValue, actualPropertyValue, "The retrieved property value should be $expectedPropertyValue.")
    }

    @Test
    fun getDateFromProperty() {
        val repoPath = createRepoPath()
        val expectedPropertyValue = Date()
        val property = BlackDuckArtifactoryProperty.INSPECTION_RETRY_COUNT
        val repoPathPropertyMap = mutableMapOf(
                repoPath to mutableMapOf(
                        property.propertyName to expectedPropertyValue.toString()
                )
        )
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val dateTimeManager = mock<DateTimeManager>()
        whenever(dateTimeManager.getDateFromString(any())).doReturn(expectedPropertyValue)
        val artifactoryPropertyService = ArtifactoryPropertyService(artifactoryPAPIService, dateTimeManager)

        val actualPropertyValue = artifactoryPropertyService.getDateFromProperty(repoPath, property)
        Assertions.assertNotNull(actualPropertyValue, "The ${property.propertyName} property should exist.")
        Assertions.assertEquals(expectedPropertyValue, actualPropertyValue, "The retrieved property value should be $expectedPropertyValue.")
    }

    @Test
    fun setProperty() {
        val repoPath = createRepoPath()
        val expectedPropertyValue = "value"
        val property = BlackDuckArtifactoryProperty.LAST_UPDATE
        val repoPathPropertyMap = mutableMapOf(
                repoPath to mutableMapOf<String, String>()
        )
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val artifactoryPropertyService = ArtifactoryPropertyService(artifactoryPAPIService, mock())
        val logger = Slf4jIntLogger(org.slf4j.LoggerFactory.getLogger("ArtifactoryPropertyServiceTest::setProperty"))

        artifactoryPropertyService.setProperty(repoPath, property, expectedPropertyValue, logger)
        Assertions.assertNotNull(repoPathPropertyMap[repoPath]!![property.propertyName], "The ${property.propertyName} property should exist.")
        Assertions.assertEquals(expectedPropertyValue, repoPathPropertyMap[repoPath]!![property.propertyName], "The property value should be $expectedPropertyValue.")
    }

    @Test
    fun setPropertyFromDate() {
        val repoPath = createRepoPath()
        val date = Date(1000)
        val expectedPropertyValue = "1000T"
        val property = BlackDuckArtifactoryProperty.LAST_UPDATE
        val repoPathPropertyMap = mutableMapOf(
                repoPath to mutableMapOf<String, String>()
        )
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val dateTimeManager = mock<DateTimeManager>()
        whenever(dateTimeManager.getStringFromDate(date)).doReturn(expectedPropertyValue)
        val artifactoryPropertyService = ArtifactoryPropertyService(artifactoryPAPIService, mock())
        val logger = Slf4jIntLogger(org.slf4j.LoggerFactory.getLogger("ArtifactoryPropertyServiceTest::setProperty"))

        artifactoryPropertyService.setProperty(repoPath, property, expectedPropertyValue, logger)
        Assertions.assertNotNull(repoPathPropertyMap[repoPath]!![property.propertyName], "The ${property.propertyName} property should exist.")
        Assertions.assertEquals(expectedPropertyValue, repoPathPropertyMap[repoPath]!![property.propertyName], "The property value should be $expectedPropertyValue.")
    }

    @Test
    fun deleteProperty() {
        val repoPath = createRepoPath()
        val property = BlackDuckArtifactoryProperty.LAST_UPDATE
        val repoPathPropertyMap = mutableMapOf(
                repoPath to mutableMapOf(
                        property.propertyName to "value"
                )
        )
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        val artifactoryPropertyService = ArtifactoryPropertyService(artifactoryPAPIService, mock())
        val logger = Slf4jIntLogger(org.slf4j.LoggerFactory.getLogger("ArtifactoryPropertyServiceTest::deleteProperty"))

        artifactoryPropertyService.deleteProperty(repoPath, property, logger)
        Assertions.assertNull(repoPathPropertyMap[repoPath]!![property.propertyName], "The ${property.propertyName} property should not exist.")
    }

    @Test
    @Disabled("deleteAllBlackDuckPropertiesFromRepo: Disabled because SetMultimap is too difficult to mock. It would require the test to know the implementation.")
    fun deleteAllBlackDuckPropertiesFromRepo() {
        val repoPath1 = createRepoPath("test/repoPath1")
        val repoPath2 = createRepoPath("test/repoPath2")
        val pypiPropertyName = "pypi.name"
        val repoPathPropertyMap = mutableMapOf(
                repoPath1 to mutableMapOf(
                        BlackDuckArtifactoryProperty.LAST_UPDATE.propertyName to "r1-value1",
                        BlackDuckArtifactoryProperty.UPDATE_STATUS.propertyName to "r1-value2",
                        pypiPropertyName to "r1-value3"
                ),
                repoPath2 to mutableMapOf(
                        BlackDuckArtifactoryProperty.LAST_UPDATE.propertyName to "r2-value1",
                        BlackDuckArtifactoryProperty.UPDATE_STATUS.propertyName to "r2-value2",
                        pypiPropertyName to "r1-value3"
                )
        )
        val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
        whenever(artifactoryPAPIService.itemsByProperties(any(), repoPath1.repoKey)).doReturn(listOf(repoPath1, repoPath2))
        val artifactoryPropertyService = ArtifactoryPropertyService(artifactoryPAPIService, mock())
        val logger = Slf4jIntLogger(org.slf4j.LoggerFactory.getLogger("ArtifactoryPropertyServiceTest::deleteProperty"))

        artifactoryPropertyService.deleteAllBlackDuckPropertiesFromRepo(repoPath1.repoKey, mutableMapOf(), logger)

        fun testRepoPath(repoPath: RepoPath) {
            Assertions.assertNull(repoPathPropertyMap[repoPath]!![BlackDuckArtifactoryProperty.LAST_UPDATE.propertyName], "The ${BlackDuckArtifactoryProperty.LAST_UPDATE.propertyName} property should not exist on ${repoPath.toPath()}.")
            Assertions.assertNull(repoPathPropertyMap[repoPath]!![BlackDuckArtifactoryProperty.UPDATE_STATUS.propertyName], "The ${BlackDuckArtifactoryProperty.UPDATE_STATUS.propertyName} property should not exist on ${repoPath.toPath()}.")
            Assertions.assertNotNull(repoPathPropertyMap[repoPath]!![pypiPropertyName], "The pypi.name property should still exist on ${repoPath.toPath()}.")
        }

        testRepoPath(repoPath1)
        testRepoPath(repoPath2)
    }

    @Test
    @Disabled("deleteAllBlackDuckPropertiesFromRepoPath: Disabled because SetMultimap is too difficult to mock. It would require the test to know the implementation.")
    fun deleteAllBlackDuckPropertiesFromRepoPath() {
        // See disabled annotation.
    }

    @Test
    @Disabled("getItemsContainingProperties: Disabled because SetMultimap is too difficult to mock. It would require the test to know the implementation.")
    fun getItemsContainingProperties() {
        // See disabled annotation.
    }

    @Test
    @Disabled("getItemsContainingPropertiesAndValues: Disabled because SetMultimap is too difficult to mock. It would require the test to know the implementation.")
    fun getItemsContainingPropertiesAndValues() {
        // See disabled annotation.
    }

    @Test
    fun getProjectNameVersion() {
        val repoPath = createRepoPath()
        val expectedProjectName = "project name"
        val expectedProjectVersion = "project version"

        fun testScenario(includeName: Boolean, includeVersion: Boolean, expectNull: Boolean) {
            val repoPathPropertyMap = mutableMapOf(
                    repoPath to mutableMapOf<String, String>()
            )

            if (includeName) {
                repoPathPropertyMap[repoPath]!![BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME.propertyName] = expectedProjectName
            }
            if (includeVersion) {
                repoPathPropertyMap[repoPath]!![BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME.propertyName] = expectedProjectVersion
            }

            val artifactoryPAPIService = createMockArtifactoryPAPIService(repoPathPropertyMap)
            val artifactoryPropertyService = ArtifactoryPropertyService(artifactoryPAPIService, mock())

            val nameVersion = artifactoryPropertyService.getProjectNameVersion(repoPath)

            if (expectNull) {
                Assertions.assertNull(nameVersion, "The expected NameVersion should be null.")
            } else {
                Assertions.assertNotNull(nameVersion, "The project name and version should not missing.")

                Assertions.assertNotNull(nameVersion!!.name, "The project name should not be set to null.")
                Assertions.assertEquals(expectedProjectName, nameVersion.name, "The project name is not what was expected.")
                Assertions.assertNotNull(nameVersion.version, "The project name should not be set to null.")
                Assertions.assertEquals(expectedProjectVersion, nameVersion.version, "The project version is not what was expected.")
            }
        }

        testScenario(true, true, false)
        testScenario(true, false, true)
        testScenario(false, true, true)
        testScenario(false, false, true)
    }
}