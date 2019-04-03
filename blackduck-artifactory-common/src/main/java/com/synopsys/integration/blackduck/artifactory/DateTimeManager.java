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
package com.synopsys.integration.blackduck.artifactory;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;

public class DateTimeManager {
    private final String dateTimePattern;
    private final String dateTimeZone;

    public DateTimeManager(final String dateTimePattern, final String dateTimeZone) {
        this.dateTimePattern = dateTimePattern;
        this.dateTimeZone = dateTimeZone;
    }

    public String getDateTimePattern() {
        return dateTimePattern;
    }

    public long getTimeFromString(final String dateTimeString) {
        return getDateFromString(dateTimeString).getTime();
    }

    public String getStringFromDate(final Date date) {
        return getStringFromDate(date, ZoneOffset.UTC);
    }

    public Optional<String> geStringFromDateWithTimeZone(final Date date) {
        if (StringUtils.isBlank(dateTimeZone)) {
            return Optional.empty();
        }

        final TimeZone timeZone = TimeZone.getTimeZone(dateTimeZone);
        final ZoneId zoneId = timeZone.toZoneId();

        return Optional.of(getStringFromDate(date, zoneId));
    }

    private String getStringFromDate(final Date date, final ZoneId zoneId) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern).withZone(zoneId);
        return date.toInstant().atZone(zoneId).format(dateTimeFormatter);
    }

    public Date getDateFromString(final String dateTimeString) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern).withZone(ZoneOffset.UTC);
        return Date.from(ZonedDateTime.from(dateTimeFormatter.parse(dateTimeString)).toInstant());
    }

}
