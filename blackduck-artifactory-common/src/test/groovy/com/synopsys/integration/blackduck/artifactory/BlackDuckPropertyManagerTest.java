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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.artifactory.util.FastTest;
import com.synopsys.integration.blackduck.artifactory.util.FileIO;
import com.synopsys.integration.blackduck.artifactory.util.TestUtil;

class BlackDuckPropertyManagerTest {
    private final ConfigurationProperty repositoryKeyListProperty = () -> "blackduck.artifactory.scan.repos";
    private final ConfigurationProperty repositoryKeyCsvProperty = () -> "blackduck.artifactory.scan.repos.csv.path";
    private final ConfigurationProperty isEnabledProperty = () -> "blackduck.artifactory.scan.enabled";
    private BlackDuckPropertyManager blackDuckPropertyManager;
    private Properties properties;

    @BeforeEach
    void init() throws IOException {
        properties = TestUtil.getDefaultProperties();
        blackDuckPropertyManager = new BlackDuckPropertyManager(properties);
    }

    @FastTest
    void getRepositoryKeysFromProperties() throws IOException {
        final List<String> repositoryKeysFromProperties = blackDuckPropertyManager.getRepositoryKeysFromProperties(repositoryKeyListProperty, repositoryKeyCsvProperty);
        assertAll("repo keys",
            () -> assertEquals(2, repositoryKeysFromProperties.size()),
            () -> assertTrue(repositoryKeysFromProperties.contains("ext-release-local")),
            () -> assertTrue(repositoryKeysFromProperties.contains("libs-release"))
        );

    }

    @Test
    @FileIO
    void getRepositoryKeysFromPropertiesCsv() throws IOException {
        blackDuckPropertyManager.getProperties().setProperty(repositoryKeyCsvProperty.getKey(), TestUtil.getResourceAsFilePath("/repoCSV"));
        final List<String> repositoryKeysFromProperties = blackDuckPropertyManager.getRepositoryKeysFromProperties(repositoryKeyListProperty, repositoryKeyCsvProperty);

        assertAll("repo keys",
            () -> assertEquals(7, repositoryKeysFromProperties.size()),
            () -> assertEquals("[test-repo1, test-repo2,  test-repo3, test-repo4 , test-repo5 ,  test-repo6, test-repo7]", Arrays.toString(repositoryKeysFromProperties.toArray()))
        );
    }

    @FastTest
    void getProperties() {
        assertEquals(properties, blackDuckPropertyManager.getProperties());
    }

    @FastTest
    void getProperty() {
        assertEquals("ext-release-local,libs-release", blackDuckPropertyManager.getProperty(repositoryKeyListProperty));
    }

    @FastTest
    void getBooleanProperty() {
        assertTrue(blackDuckPropertyManager.getBooleanProperty(isEnabledProperty));
    }
}