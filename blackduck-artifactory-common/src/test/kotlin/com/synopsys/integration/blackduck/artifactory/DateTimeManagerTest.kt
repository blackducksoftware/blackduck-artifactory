/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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