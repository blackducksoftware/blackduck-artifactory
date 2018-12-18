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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;

import com.synopsys.integration.blackduck.artifactory.util.FastTest;

class DateTimeManagerTest {
    private final String dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private final String dateAsString = "2016-01-01T00:00:00.000";
    private final long dateAsMilliseconds = 1451606400000L;

    private DateTimeManager dateTimeManager;

    @BeforeEach
    void setUp() {
        dateTimeManager = new DateTimeManager(dateTimePattern);
    }

    @FastTest
    void getDateTimePattern() {
        assertEquals(dateTimePattern, dateTimeManager.getDateTimePattern());
    }

    @FastTest
    void getTimeFromString() {
        final long actualTime = dateTimeManager.getTimeFromString(dateAsString);
        assertEquals(dateAsMilliseconds, actualTime);
    }

    @FastTest
    void getStringFromDate() {
        final Date providedDate = new Date(dateAsMilliseconds);
        assertEquals(dateAsString, dateTimeManager.getStringFromDate(providedDate));
    }

    @FastTest
    void getDateFromString() {
        final Date actualDate = dateTimeManager.getDateFromString(dateAsString);
        final Date expectedDate = new Date(dateAsMilliseconds);
        assertEquals(expectedDate, actualDate);
    }
}