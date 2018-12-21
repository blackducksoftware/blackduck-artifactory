/**
 * blackduck-artifactory-common
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
package com.synopsys.integration.blackduck.artifactory.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;

public class TestUtil {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String DEFAULT_PROPERTIES_RESOURCE_PATH = "/blackDuckPlugin.properties";
    public static final String BLACKDUCK_CREDENTIALS_ENV_VAR = BlackDuckServerConfigBuilder.BLACKDUCK_SERVER_CONFIG_ENVIRONMENT_VARIABLE_PREFIX + "CREDENTIALS";

    public static Properties getDefaultProperties() throws IOException {
        return getResourceAsProperties(DEFAULT_PROPERTIES_RESOURCE_PATH);
    }

    public static Properties getResourceAsProperties(final String resourcePath) throws IOException {
        final Properties properties = new Properties();
        try (final InputStream inputStream = getResourceAsStream(resourcePath)) {
            properties.load(inputStream);
        }

        return properties;
    }

    public static String getResourceAsFilePath(final String resourcePath) {
        return TestUtil.class.getResource(resourcePath).getFile();
    }

    public static File getResourceAsFile(final String resourcePath) {
        return new File(getResourceAsFilePath(resourcePath));
    }

    public static InputStream getResourceAsStream(final String resourcePath) {
        return TestUtil.class.getResourceAsStream(resourcePath);
    }

    public static BlackDuckServerConfig getHubServerConfigFromEnvVar() {
        final String credentials = System.getenv(BLACKDUCK_CREDENTIALS_ENV_VAR);
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> properties = GSON.fromJson(credentials, type);

        final BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = new BlackDuckServerConfigBuilder();
        blackDuckServerConfigBuilder.setFromProperties(properties);

        return blackDuckServerConfigBuilder.build();
    }
}
