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
package com.synopsys.integration.blackduck.artifactory

import org.artifactory.repo.Repositories
import org.artifactory.repo.RepositoryConfiguration
import org.artifactory.search.Searches
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as mockWhen

class ArtifactoryPAPIServiceTest {
    @Test
    fun getPackageType() {
        val repositoryConfiguration = mock(RepositoryConfiguration::class.java)
        mockWhen(repositoryConfiguration.packageType).thenReturn("maven")

        val repositories = mock(Repositories::class.java)
        mockWhen(repositories.getRepositoryConfiguration("maven-local")).thenReturn(repositoryConfiguration)

        val searches = mock(Searches::class.java)

        val artifactoryPAPIService = ArtifactoryPAPIService(PluginRepoPathFactory(false), repositories, searches)
        val packageType = artifactoryPAPIService.getPackageType("maven-local")

        Assertions.assertNotNull(packageType)
        Assertions.assertEquals("maven", packageType)
    }

    @Test
    fun getArtifactCount() {
        val pluginRepoPathFactory = PluginRepoPathFactory(false)
        val repoKeyPath = pluginRepoPathFactory.create("maven-local-3")
        val repoKeyPath2 = pluginRepoPathFactory.create("maven-local-6")

        val mockPluginRepoPathFactory = mock(PluginRepoPathFactory::class.java)
        mockWhen(mockPluginRepoPathFactory.create(repoKeyPath.repoKey)).thenReturn(repoKeyPath)
        mockWhen(mockPluginRepoPathFactory.create(repoKeyPath2.repoKey)).thenReturn(repoKeyPath2)

        val repositories = mock(Repositories::class.java)
        mockWhen(repositories.getArtifactsCount(repoKeyPath)).thenReturn(3L)
        mockWhen(repositories.getArtifactsCount(repoKeyPath2)).thenReturn(6L)

        val searches = mock(Searches::class.java)

        val artifactoryPAPIService = ArtifactoryPAPIService(mockPluginRepoPathFactory, repositories, searches)
        val artifactCount3 = artifactoryPAPIService.getArtifactCount(listOf(repoKeyPath.repoKey))
        val artifactCount6 = artifactoryPAPIService.getArtifactCount(listOf(repoKeyPath2.repoKey))
        val artifactCount9 = artifactoryPAPIService.getArtifactCount(listOf(repoKeyPath.repoKey, repoKeyPath2.repoKey))

        Assertions.assertEquals(3L, artifactCount3)
        Assertions.assertEquals(6L, artifactCount6)
        Assertions.assertEquals(9L, artifactCount9)
    }

    @Test
    fun isValidRepository_Valid() {
        val pluginRepoPathFactory = PluginRepoPathFactory(false)
        val repoKeyPath = pluginRepoPathFactory.create("maven-local")

        val mockPluginRepoPathFactory = mock(PluginRepoPathFactory::class.java)
        mockWhen(mockPluginRepoPathFactory.create(repoKeyPath.repoKey)).thenReturn(repoKeyPath)

        val repositoryConfiguration = mock(RepositoryConfiguration::class.java)
        val repositories = mock(Repositories::class.java)
        mockWhen(repositories.getRepositoryConfiguration("maven-local")).thenReturn(repositoryConfiguration)
        mockWhen(repositories.exists(repoKeyPath)).thenReturn(true)

        val searches = mock(Searches::class.java)

        val artifactoryPAPIService = ArtifactoryPAPIService(mockPluginRepoPathFactory, repositories, searches)
        val isValid = artifactoryPAPIService.isValidRepository("maven-local")

        Assertions.assertTrue(isValid)
    }

    @Test
    fun isValidRepository_Invalid_Nonexistent() {
        val pluginRepoPathFactory = PluginRepoPathFactory(false)
        val repoKeyPath = pluginRepoPathFactory.create("maven-local")

        val mockPluginRepoPathFactory = mock(PluginRepoPathFactory::class.java)
        mockWhen(mockPluginRepoPathFactory.create(repoKeyPath.repoKey)).thenReturn(repoKeyPath)

        val repositories = mock(Repositories::class.java)
        mockWhen(repositories.exists(repoKeyPath)).thenReturn(false)

        val searches = mock(Searches::class.java)

        val artifactoryPAPIService = ArtifactoryPAPIService(mockPluginRepoPathFactory, repositories, searches)
        val isValid = artifactoryPAPIService.isValidRepository("maven-local")

        Assertions.assertFalse(isValid)
    }

    @Test
    fun isValidRepository_Invalid_NoRepoKey() {
        val mockPluginRepoPathFactory = mock(PluginRepoPathFactory::class.java)
        val repositories = mock(Repositories::class.java)
        val searches = mock(Searches::class.java)

        val artifactoryPAPIService = ArtifactoryPAPIService(mockPluginRepoPathFactory, repositories, searches)
        val isValidEmpty = artifactoryPAPIService.isValidRepository(" ")

        Assertions.assertFalse(isValidEmpty)
    }

    @Test
    fun isValidRepository_Invalid_NoRepositoryConfiguration() {
        val pluginRepoPathFactory = PluginRepoPathFactory(false)
        val repoKeyPath = pluginRepoPathFactory.create("maven-local")

        val mockPluginRepoPathFactory = mock(PluginRepoPathFactory::class.java)
        mockWhen(mockPluginRepoPathFactory.create(repoKeyPath.repoKey)).thenReturn(repoKeyPath)

        val repositories = mock(Repositories::class.java)
        mockWhen(repositories.getRepositoryConfiguration("maven-local")).thenReturn(null)
        mockWhen(repositories.exists(repoKeyPath)).thenReturn(true)

        val searches = mock(Searches::class.java)

        val artifactoryPAPIService = ArtifactoryPAPIService(mockPluginRepoPathFactory, repositories, searches)
        val isValid = artifactoryPAPIService.isValidRepository("maven-local")

        Assertions.assertFalse(isValid)
    }

    @Test
    fun searchForArtifactsByPatterns() {
        val pluginRepoPathFactory = PluginRepoPathFactory(false)
        val artifact1 = pluginRepoPathFactory.create("maven-local/artifact1.jar")
        val artifact2 = pluginRepoPathFactory.create("maven-local/artifact1.tar.gz")

        val repositories = mock(Repositories::class.java)

        val searches = mock(Searches::class.java)
        mockWhen(searches.artifactsByName("*.jar", "maven-local")).thenReturn(listOf(artifact1))
        mockWhen(searches.artifactsByName("*.tar.gz", "maven-local")).thenReturn(listOf(artifact2))

        val artifactoryPAPIService = ArtifactoryPAPIService(pluginRepoPathFactory, repositories, searches)
        val artifacts = artifactoryPAPIService.searchForArtifactsByPatterns("maven-local", listOf("*.jar", "*.tar.gz"))

        Assertions.assertEquals(2, artifacts.size)
    }

    @Test
    fun searchForArtifactsByPatterns_NoArtifacts() {
        val pluginRepoPathFactory = PluginRepoPathFactory(false)
        val repositories = mock(Repositories::class.java)

        val searches = mock(Searches::class.java)
        mockWhen(searches.artifactsByName("*.jar", "maven-local")).thenReturn(listOf())
        mockWhen(searches.artifactsByName("*.tar.gz", "maven-local")).thenReturn(listOf())

        val artifactoryPAPIService = ArtifactoryPAPIService(pluginRepoPathFactory, repositories, searches)
        val artifacts = artifactoryPAPIService.searchForArtifactsByPatterns("maven-local", listOf("*.jar", "*.tar.gz"))

        Assertions.assertEquals(0, artifacts.size)
    }
}