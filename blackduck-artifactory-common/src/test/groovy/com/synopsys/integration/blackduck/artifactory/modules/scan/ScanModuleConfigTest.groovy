/*
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
package com.synopsys.integration.blackduck.artifactory.modules.scan

import com.synopsys.integration.blackduck.artifactory.DateTimeManager
import com.synopsys.integration.builder.BuilderStatus
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ScanModuleConfigTest {
    private ScanModuleConfig validScanModuleConfig
    private ScanModuleConfig invalidScanModuleConfig

    @BeforeEach
    void init() {
        validScanModuleConfig = new ScanModuleConfig(
            true,
            "0 0/1 * 1/1 * ?",
            "binaries/path",
            "2016-01-01T00:00:00.000",
            false,
            ["*.jar"],
            5000,
            false,
            ["repo1", "repo2"]
            ,
            File.createTempFile("artifactory-test", "ScanModuleConfigTest"),
            new DateTimeManager("yyyy-MM-dd'T'HH:mm:ss.SSS")
        )

        invalidScanModuleConfig = new ScanModuleConfig(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
            ,
            null,
            null
        )
    }

    @Test
    void validate() {
        final BuilderStatus validBuilderStatus = new BuilderStatus()
        validScanModuleConfig.validate(validBuilderStatus)
        Assert.assertEquals(0, validBuilderStatus.getErrorMessages().size())

        final BuilderStatus invalidBuilderStatus = new BuilderStatus()
        invalidScanModuleConfig.validate(invalidBuilderStatus)
        Assert.assertEquals(10, invalidBuilderStatus.getErrorMessages().size())
    }

    @Test
    void isEnabled() {
        Assert.assertTrue(validScanModuleConfig.isEnabled())
        Assert.assertFalse(invalidScanModuleConfig.isEnabled())
    }

    @Test
    void isEnabledUnverified() {
        Assert.assertTrue(validScanModuleConfig.isEnabledUnverified())
        Assert.assertNull(invalidScanModuleConfig.isEnabledUnverified())
    }

    @Test
    void getAddPolicyStatusCron() {
        Assert.assertNotNull(validScanModuleConfig.getCron())
        Assert.assertNull(invalidScanModuleConfig.getCron())
    }

    @Test
    void getBinariesDirectoryPath() {
        Assert.assertNotNull(validScanModuleConfig.getBinariesDirectoryPath())
        Assert.assertNull(invalidScanModuleConfig.getBinariesDirectoryPath())
    }

    @Test
    void getArtifactCutoffDate() {
        Assert.assertNotNull(validScanModuleConfig.getArtifactCutoffDate())
        Assert.assertNull(invalidScanModuleConfig.getArtifactCutoffDate())
    }

    @Test
    void getDryRun() {
        Assert.assertNotNull(validScanModuleConfig.getDryRun())
        Assert.assertNull(invalidScanModuleConfig.getDryRun())
    }

    @Test
    void getNamePatterns() {
        Assert.assertNotNull(validScanModuleConfig.getNamePatterns())
        Assert.assertNull(invalidScanModuleConfig.getNamePatterns())
    }

    @Test
    void getMemory() {
        Assert.assertNotNull(validScanModuleConfig.getMemory())
        Assert.assertNull(invalidScanModuleConfig.getMemory())
    }

    @Test
    void getRepoPathCodelocation() {
        Assert.assertFalse(validScanModuleConfig.getRepoPathCodelocation())
        Assert.assertNull(invalidScanModuleConfig.getRepoPathCodelocation())
    }

    @Test
    void getRepos() {
        Assert.assertNotNull(validScanModuleConfig.getRepos())
        Assert.assertNull(invalidScanModuleConfig.getRepos())
    }

    @Test
    void getScanCron() {
        Assert.assertNotNull(validScanModuleConfig.getCron())
        Assert.assertNull(invalidScanModuleConfig.getCron())
    }

    @Test
    void getCliDirectory() {
        Assert.assertNotNull(validScanModuleConfig.getCliDirectory())
        Assert.assertNull(invalidScanModuleConfig.getCliDirectory())
    }
}