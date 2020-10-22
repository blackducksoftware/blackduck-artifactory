/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2020 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
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

        // TODO: Is checking the spaces here necessary?
        assertAll("repo keys",
                { assertEquals(7, repositoryKeysFromProperties.size) },
                { assertEquals(listOf("test-repo1", "test-repo2", " test-repo3", "test-repo4 ", "test-repo5 ", " test-repo6", "test-repo7"), repositoryKeysFromProperties) }
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