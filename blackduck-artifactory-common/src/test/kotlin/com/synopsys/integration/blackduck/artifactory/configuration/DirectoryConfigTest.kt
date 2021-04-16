/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.configuration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class DirectoryConfigTest {
    private var directoryConfig: DirectoryConfig? = null

    @BeforeEach
    internal fun setUp() {
        val homeDirectory = File("home")
        val etcDirectory = File("etc")
        val pluginsDirectory = File("plugins")

        directoryConfig = DirectoryConfig.createDefault(homeDirectory, etcDirectory, pluginsDirectory, "1.2.3", "path/to/properties/file")
    }

    @Test
    fun getHomeDirectory() {
        assertEquals(File("home"), directoryConfig!!.homeDirectory)
    }

    @Test
    fun getEtcDirectory() {
        assertEquals(File("etc"), directoryConfig!!.etcDirectory)
    }

    @Test
    fun getPluginsLibDirectory() {
        assertEquals(File("plugins", "lib"), directoryConfig!!.pluginsLibDirectory)
    }

    @Test
    fun getVersionFile() {
        assertEquals(File(directoryConfig!!.pluginsLibDirectory, "version.txt"), directoryConfig!!.versionFile)
    }

    @Test
    fun getThirdPartyVersion() {
        assertEquals("1.2.3", directoryConfig!!.thirdPartyVersion)
    }

    @Test
    fun getPropertiesFilePathOverride() {
        assertEquals("path/to/properties/file", directoryConfig!!.propertiesFilePathOverride)
    }
}