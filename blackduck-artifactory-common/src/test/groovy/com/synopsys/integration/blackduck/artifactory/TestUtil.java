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
package com.synopsys.integration.blackduck.artifactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TestUtil {
    public static Properties getDefaultProperties() throws IOException {
        return getResourceAsProperties("/blackDuckPlugin.properties");
    }

    public static Properties getResourceAsProperties(final String resourcePath) throws IOException {
        final Properties properties = new Properties();
        try (final InputStream inputStream = TestUtil.class.getResourceAsStream(resourcePath)) {
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
}
