/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2019 Synopsys, Inc.
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

import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

open class DateTimeManager(val dateTimePattern: String, private val dateTimeZone: String? = null) {
    open fun getTimeFromString(dateTimeString: String): Long {
        return getDateFromString(dateTimeString).time
    }

    open fun getStringFromDate(date: Date): String {
        return getStringFromDate(date, ZoneOffset.UTC)
    }

    open fun geStringFromDateWithTimeZone(date: Date): Optional<String> {
        if (dateTimeZone.isNullOrBlank()) {
            return Optional.empty()
        }

        val timeZone = TimeZone.getTimeZone(dateTimeZone)
        val zoneId = timeZone.toZoneId()

        return Optional.of(getStringFromDate(date, zoneId))
    }

    private fun getStringFromDate(date: Date, zoneId: ZoneId): String {
        val dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern).withZone(zoneId)
        return date.toInstant().atZone(zoneId).format(dateTimeFormatter)
    }

    open fun getDateFromString(dateTimeString: String): Date {
        val dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern).withZone(ZoneOffset.UTC)
        return Date.from(ZonedDateTime.from(dateTimeFormatter.parse(dateTimeString)).toInstant())
    }
}
