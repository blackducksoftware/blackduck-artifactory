package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.conda

import com.synopsys.integration.bdio.model.externalid.ExternalId
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType
import org.artifactory.repo.RepoPath
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.util.*
import org.mockito.Mockito.`when` as mockWhen

class CondaExternalIdExtractorTest {
    private val defaultArchitecture = "linux-64"
    private val externalIdFactory = ExternalIdFactory()
    private val condaExternalIdExtractor = CondaExternalIdExtractor(externalIdFactory)
    private val supportedPackageType = SupportedPackageType.CONDA

    private fun assertValidExtraction(fileName: String, expectedName: String, expectedVersion: String, architecture: String = defaultArchitecture) {
        val actualExternalId = extractExternalId(fileName, architecture)
        val expectedExternalId = externalIdFactory.createNameVersionExternalId(supportedPackageType.forge, expectedName, expectedVersion)
        Assertions.assertEquals(expectedExternalId.createBdioId(), actualExternalId.get().createBdioId())
    }

    private fun assertInvalidExtraction(fileName: String, architecture: String = defaultArchitecture) {
        val actualExternalId = extractExternalId(fileName, architecture)
        Assertions.assertFalse(actualExternalId.isPresent)
    }

    private fun extractExternalId(fileName: String, architecture: String): Optional<ExternalId> {
        val parentRepoPath = mock(RepoPath::class.java)
        val repoPath: RepoPath = mock(RepoPath::class.java)
        mockWhen(repoPath.name).thenReturn(fileName)
        mockWhen(repoPath.parent).thenReturn(parentRepoPath)
        mockWhen(parentRepoPath.name).thenReturn(architecture)

        return condaExternalIdExtractor.extractExternalId(supportedPackageType, repoPath)
    }

    @Test
    fun extractValidExternalId() {
        assertValidExtraction(fileName = "numpy-1.13.1-py27_0.tar.bz2", expectedName = "numpy", expectedVersion = "1.13.1-py27_0-linux-64")
    }

    @Test
    fun extractValidExternalIdWithDots() {
        assertValidExtraction(fileName = "cudnn-7.1.2-cuda9.0_0.tar.bz2", expectedName = "cudnn", expectedVersion = "7.1.2-cuda9.0_0-linux-64")
    }

    @Test
    fun extractValidExternalIdWithExtraHyphens() {
        assertValidExtraction(fileName = "ca-certificates-2019.8.28-0.tar.bz2", expectedName = "ca-certificates", expectedVersion = "2019.8.28-0-linux-64")
    }

    @Test
    fun extractInvalidFormatExternalId() {
        assertInvalidExtraction("numpy-1.13.1-py27--_0.tar.bz2")
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