/**
 * blackduck-artifactory-common
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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