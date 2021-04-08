/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public class DateTimeManager {
    private final String dateTimePattern;

    @Nullable
    private final String dateTimeZone;

    public DateTimeManager(String dateTimePattern) {
        this(dateTimePattern, null);
    }

    public DateTimeManager(String dateTimePattern, @Nullable String dateTimeZone) {
        this.dateTimePattern = dateTimePattern;
        this.dateTimeZone = dateTimeZone;
    }

    public Long getTimeFromString(String dateTimeString) {
        return getDateFromString(dateTimeString).getTime();
    }

    public String getStringFromDate(Date date) {
        return getStringFromDate(date, ZoneOffset.UTC);
    }

    public Optional<String> geStringFromDateWithTimeZone(Date date) {
        if (StringUtils.isBlank(dateTimeZone)) {
            return Optional.empty();
        }

        TimeZone timeZone = TimeZone.getTimeZone(dateTimeZone);
        ZoneId zoneId = timeZone.toZoneId();

        return Optional.of(getStringFromDate(date, zoneId));
    }

    public String getStringFromDate(Date date, ZoneId zoneId) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern).withZone(zoneId);
        return date.toInstant().atZone(zoneId).format(dateTimeFormatter);
    }

    public Date getDateFromString(String dateTimeString) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern).withZone(ZoneOffset.UTC);
        return Date.from(ZonedDateTime.from(dateTimeFormatter.parse(dateTimeString)).toInstant());
    }

    public String getDateTimePattern() {
        return dateTimePattern;
    }
}
