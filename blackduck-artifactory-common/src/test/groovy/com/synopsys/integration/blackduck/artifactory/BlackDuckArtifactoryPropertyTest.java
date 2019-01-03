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
package com.synopsys.integration.blackduck.artifactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumingThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.commons.util.StringUtils;

import com.synopsys.integration.blackduck.artifactory.util.Fast;

class BlackDuckArtifactoryPropertyTest {
    @Test
    void getName() {
    }

    @ParameterizedTest
    @Fast
    @EnumSource(BlackDuckArtifactoryProperty.class)
    void getOldName(final BlackDuckArtifactoryProperty property) {
        final String name = property.getName();
        final String oldName = property.getOldName();

        assumingThat(name == null, () -> {
            assertNotNull(oldName);
            assertTrue(StringUtils.isNotBlank(oldName));
        });

        assumingThat(name != null, () -> {
            assertTrue(StringUtils.isNotBlank(name));

            assumingThat(oldName != null, () -> {
                assertTrue(StringUtils.isNotBlank(oldName));
            });
        });
    }
}