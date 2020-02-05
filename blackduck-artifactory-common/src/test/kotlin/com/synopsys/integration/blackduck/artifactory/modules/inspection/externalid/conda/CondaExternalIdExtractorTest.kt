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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.conda

import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType
import org.artifactory.repo.RepoPath
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as mockWhen

class CondaExternalIdExtractorTest {
    private val externalIdFactory = ExternalIdFactory()
    private val condaExternalIdExtractor = CondaExternalIdExtractor(externalIdFactory)
    private val supportedPackageType = SupportedPackageType.CONDA

    @Test
    fun extractValidExternalId() {
        val parentRepoPath = mock(RepoPath::class.java)
        val repoPath: RepoPath = mock(RepoPath::class.java)
        mockWhen(repoPath.name).thenReturn("numpy-1.13.1-py27_0.tar.bz2")
        mockWhen(repoPath.parent).thenReturn(parentRepoPath)
        mockWhen(parentRepoPath.name).thenReturn("linux-64")

        val actualExternalId = condaExternalIdExtractor.extractExternalId(supportedPackageType, repoPath)
        val expectedExternalId = externalIdFactory.createNameVersionExternalId(supportedPackageType.forge, "numpy", "1.13.1-py27_0-linux-64")
        Assertions.assertEquals(expectedExternalId.createBdioId(), actualExternalId.get().createBdioId())
    }

    @Test
    fun extractValidExternalIdWithExtraHyphens() {
        val parentRepoPath = mock(RepoPath::class.java)
        val repoPath: RepoPath = mock(RepoPath::class.java)
        mockWhen(repoPath.name).thenReturn("ca-certificates-2019.8.28-0.tar.bz2")
        mockWhen(repoPath.parent).thenReturn(parentRepoPath)
        mockWhen(parentRepoPath.name).thenReturn("linux-64")

        val actualExternalId = condaExternalIdExtractor.extractExternalId(supportedPackageType, repoPath)
        val expectedExternalId = externalIdFactory.createNameVersionExternalId(supportedPackageType.forge, "ca-certificates", "2019.8.28-0-linux-64")
        Assertions.assertEquals(expectedExternalId.createBdioId(), actualExternalId.get().createBdioId())
    }

    @Test
    fun extractInvalidFormatExternalId() {
        val parentRepoPath = mock(RepoPath::class.java)
        val repoPath: RepoPath = mock(RepoPath::class.java)
        mockWhen(repoPath.name).thenReturn("numpy-1.13.1-py27--_0.tar.bz2")
        mockWhen(repoPath.parent).thenReturn(parentRepoPath)
        mockWhen(parentRepoPath.name).thenReturn("linux-64")

        val externalId = condaExternalIdExtractor.extractExternalId(supportedPackageType, repoPath)
        Assertions.assertFalse(externalId.isPresent)
    }

    @Test
    fun extractNoParentRepoPath() {
        val repoPath: RepoPath = mock(RepoPath::class.java)
        mockWhen(repoPath.name).thenReturn("numpy-1.13.1-py27_0.tar.bz2")
        mockWhen(repoPath.parent).thenReturn(null)

        val externalId = condaExternalIdExtractor.extractExternalId(supportedPackageType, repoPath)
        Assertions.assertFalse(externalId.isPresent)
    }
}