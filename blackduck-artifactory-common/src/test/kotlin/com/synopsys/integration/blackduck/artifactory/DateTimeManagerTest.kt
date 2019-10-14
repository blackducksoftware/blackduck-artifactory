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
package com.synopsys.integration.blackduck.artifactory

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class DateTimeManagerTest {
    private val dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS"
    private val dateAsString = "2016-01-01T00:00:00.000"
    private val dateAsMilliseconds = 1451606400000L

    @Test
    fun getTimeFromString() {
        val dateTimeManager = DateTimeManager(dateTimePattern)
        val actualTime = dateTimeManager.getTimeFromString(dateAsString)
        assertEquals(dateAsMilliseconds, actualTime)
    }

    @Test
    fun getStringFromDate() {
        val dateTimeManager = DateTimeManager(dateTimePattern)
        val providedDate = Date(dateAsMilliseconds)
        assertEquals(dateAsString, dateTimeManager.getStringFromDate(providedDate))
    }

    @Test
    fun geStringFromDateWithTimeZone() {
        val dateTimeManager = DateTimeManager(dateTimePattern, "ETC")
        val providedDate = Date(dateAsMilliseconds)
        val value = dateTimeManager.geStringFromDateWithTimeZone(providedDate)
        assertTrue(value.isPresent)
        assertEquals(dateAsString, value.get())
    }

    @Test
    fun geStringFromDateWithTimeZone_NoTimeZone() {
        val dateTimeManager = DateTimeManager(dateTimePattern)
        val providedDate = Date(dateAsMilliseconds)
        assertFalse(dateTimeManager.geStringFromDateWithTimeZone(providedDate).isPresent)
    }

    @Test
    fun getDateFromString() {
        val dateTimeManager = DateTimeManager(dateTimePattern)
        val actualDate = dateTimeManager.getDateFromString(dateAsString)
        val expectedDate = Date(dateAsMilliseconds)
        assertEquals(expectedDate, actualDate)
    }
}