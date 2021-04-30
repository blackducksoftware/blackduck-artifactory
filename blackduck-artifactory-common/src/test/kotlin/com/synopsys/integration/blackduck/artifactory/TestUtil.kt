/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory

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
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder
import org.artifactory.repo.RepoPath
import org.mockito.ArgumentMatchers.anyMap
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*

typealias PropertiesMap = MutableMap<String, String>

object TestUtil {
    private val GSON = GsonBuilder().setPrettyPrinting().create()

    private const val DEFAULT_PROPERTIES_RESOURCE_PATH = "/blackDuckPlugin.properties"
    private const val BLACKDUCK_CREDENTIALS_ENV_VAR = "BLACKDUCK_CREDENTIALS"

    fun getDefaultProperties(): Properties {
        return getResourceAsProperties(DEFAULT_PROPERTIES_RESOURCE_PATH)
    }

    fun getBlackDuckServerConfigBuilder(): BlackDuckServerConfigBuilder {
        val credentials = System.getenv(BLACKDUCK_CREDENTIALS_ENV_VAR)
        val type = object : TypeToken<Map<String, String>>() {

        }.type
        val properties = GSON.fromJson<Map<String, String>>(credentials, type)

        val blackDuckServerConfigBuilder = BlackDuckServerConfigBuilder()
        blackDuckServerConfigBuilder.setProperties(properties.entries)

        return blackDuckServerConfigBuilder
    }

    @Throws(IOException::class)
    fun getResourceAsProperties(resourcePath: String): Properties {
        val properties = Properties()
        getResourceAsStream(resourcePath).use { inputStream -> properties.load(inputStream) }

        return properties
    }

    fun getResourceAsFilePath(resourcePath: String): String {
        return TestUtil::class.java.getResource(resourcePath).file
    }

    fun getResourceAsFile(resourcePath: String): File {
        return File(getResourceAsFilePath(resourcePath))
    }

    fun getResourceAsStream(resourcePath: String): InputStream {
        return TestUtil::class.java.getResourceAsStream(resourcePath)
    }

    fun createSpoofedInspectionPropertyService(repoPathPropertyMap: MutableMap<RepoPath, PropertiesMap>): InspectionPropertyService {
        val dateTimeManager = DateTimeManager("yyyy-MM-dd'T'HH:mm:ss.SSS")
        val pluginRepoPathFactory = PluginRepoPathFactory(false);
        return InspectionPropertyService(createMockArtifactoryPAPIService(repoPathPropertyMap), dateTimeManager, pluginRepoPathFactory, 5);
    }

    fun createMockArtifactoryPAPIService(repoPathPropertyMap: MutableMap<RepoPath, PropertiesMap>): ArtifactoryPAPIService {
        val artifactoryPAPIService = mock<ArtifactoryPAPIService>()

        // Set property
        whenever(artifactoryPAPIService.setProperty(any(), any(), any())).then {
            val repoPath: RepoPath = it.getArgument(0)
            val propertyKey: String = it.getArgument(1)
            val propertyValue: String = it.getArgument(2)
            val properties = repoPathPropertyMap.getOrPut(repoPath, defaultValue = { mutableMapOf() })
            properties[propertyKey] = propertyValue
            repoPathPropertyMap.put(repoPath, properties)
        }

        // Get property
        whenever(artifactoryPAPIService.getProperty(any(), any())).doAnswer {
            val repoPath: RepoPath = it.getArgument(0)
            val propertyKey: String = it.getArgument(1)
            return@doAnswer Optional.ofNullable(repoPathPropertyMap[repoPath]?.get(propertyKey))
        }

        // Has property
        whenever(artifactoryPAPIService.hasProperty(any(), any())).doAnswer {
            val repoPath: RepoPath = it.getArgument(0)
            val propertyKey: String = it.getArgument(1)
            return@doAnswer repoPathPropertyMap[repoPath]?.containsKey(propertyKey)
        }

        // Delete property
        whenever(artifactoryPAPIService.deleteProperty(any(), any())).then {
            val repoPath: RepoPath = it.getArgument(0)
            val propertyKey: String = it.getArgument(1)
            repoPathPropertyMap[repoPath]?.remove(propertyKey)
        }

        // Search for artifact with property
        whenever(artifactoryPAPIService.itemsByProperties(anyMap(), any())).doAnswer { invocationOnMock ->
            val propertiesToLookFor = invocationOnMock.getArgument<Map<String, String>>(0)
            val repoKeys = invocationOnMock.getArgument<Array<String>>(1)

            val matchingRepoPaths = mutableListOf<RepoPath>()
            repoPathPropertyMap.entries
                .filter { repoKeys == null || repoKeys.contains(it.key.repoKey) }
                .forEach { entry ->
                    entry.value.entries.forEach { repoPathProperty ->
                        val property = propertiesToLookFor[repoPathProperty.key]
                        if (property != null && (property == "*" || repoPathProperty.value == property)) {
                            matchingRepoPaths.add(entry.key)
                        }
                    }
                }

            return@doAnswer listOf<RepoPath>()
        }

        return artifactoryPAPIService
    }

    fun createRepoPath(repoPath: String = "test"): RepoPath {
        return PluginRepoPathFactory(false).create(repoPath)
    }
}
