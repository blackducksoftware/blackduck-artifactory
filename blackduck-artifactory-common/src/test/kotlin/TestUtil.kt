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

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*

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
        blackDuckServerConfigBuilder.setFromProperties(properties)

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
}
