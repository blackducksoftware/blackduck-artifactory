/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.configuration

import com.synopsys.integration.blackduck.artifactory.TestUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.util.*

class ConfigurationPropertyManagerTest {
    private val timeoutProperty = ConfigurationProperty { "blackduck.timeout" }
    private val scanNamePatternsProperty = ConfigurationProperty { "blackduck.artifactory.scan.name.patterns" }
    private val repositoryKeyListProperty = ConfigurationProperty { "blackduck.artifactory.scan.repos" }
    private val repositoryKeyCsvProperty = ConfigurationProperty { "blackduck.artifactory.scan.repos.csv.path" }
    private val isEnabledProperty = ConfigurationProperty { "blackduck.artifactory.scan.enabled" }
    private var configurationPropertyManager: ConfigurationPropertyManager? = null
    private var properties: Properties? = null

    @BeforeEach
    fun init() {
        properties = TestUtil.getDefaultProperties()
        configurationPropertyManager = ConfigurationPropertyManager(properties)
    }

    @Test
    fun getPropertiesThatWereNotSet() {
        val emptyProperties = Properties()
        val emptyConfigurationPropertyManager = ConfigurationPropertyManager(emptyProperties)

        assertDoesNotThrow {
            val repositoryKeysFromProperties = emptyConfigurationPropertyManager.getRepositoryKeysFromProperties(repositoryKeyListProperty, repositoryKeyCsvProperty)
            assertAll("repo keys",
                { assertEquals(0, repositoryKeysFromProperties.size) },
                {}
            )
        }
    }

    @Test
    fun getRepositoryKeysFromProperties() {
        val repositoryKeysFromProperties = configurationPropertyManager!!.getRepositoryKeysFromProperties(repositoryKeyListProperty, repositoryKeyCsvProperty)
        assertAll("repo keys",
            { assertEquals(2, repositoryKeysFromProperties.size) },
            { assertTrue(repositoryKeysFromProperties.contains("ext-release-local")) },
            { assertTrue(repositoryKeysFromProperties.contains("libs-release")) }
        )
    }

    @Test
    fun getRepositoryKeysFromPropertiesCsv() {
        configurationPropertyManager!!.properties.setProperty(repositoryKeyCsvProperty.key, TestUtil.getResourceAsFilePath("/repoCSV"))
        val repositoryKeysFromProperties = configurationPropertyManager!!.getRepositoryKeysFromProperties(repositoryKeyListProperty, repositoryKeyCsvProperty)

        assertAll("repo keys",
            { assertEquals(4, repositoryKeysFromProperties.size) },
            { assertEquals(listOf("test-repo1", "test-repo2 ", " test-repo3", "test-repo4"), repositoryKeysFromProperties) }
        )
    }

    @Test
    fun getProperties() {
        assertEquals(properties, configurationPropertyManager!!.properties)
    }

    @Test
    fun getProperty() {
        assertEquals("ext-release-local,libs-release", configurationPropertyManager!!.getProperty(repositoryKeyListProperty))
    }

    @Test
    fun getBooleanProperty() {
        assertTrue(configurationPropertyManager!!.getBooleanProperty(isEnabledProperty)!!)
    }

    @Test
    fun getIntegerProperty() {
        val expectedTimeout = 120
        val actualTimeout = configurationPropertyManager!!.getIntegerProperty(timeoutProperty)
        assertEquals(expectedTimeout, actualTimeout)
    }

    @Test
    fun getPropertyAsList() {
        assertArrayEquals("*.war,*.zip,*.tar.gz,*.hpi".split(",".toRegex()).toTypedArray(), configurationPropertyManager!!.getPropertyAsList(scanNamePatternsProperty).toTypedArray())
    }
}
