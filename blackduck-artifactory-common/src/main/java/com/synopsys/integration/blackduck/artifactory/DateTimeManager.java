/**
 * hub-artifactory-common
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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateTimeManager {
    private final String dateTimePattern;

    public DateTimeManager(final String dateTimePattern) {
        this.dateTimePattern = dateTimePattern;
    }

    public String getDateTimePattern() {
        return dateTimePattern;
    }

    public long getTimeFromString(final String dateTimeString) {
        return getDateFromString(dateTimeString).getTime();
    }

    public String getStringFromDate(final Date date) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern).withZone(ZoneOffset.UTC);
        return date.toInstant().atZone(ZoneOffset.UTC).format(dateTimeFormatter);
    }

    public Date getDateFromString(final String dateTimeString) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern).withZone(ZoneOffset.UTC);
        return Date.from(ZonedDateTime.from(dateTimeFormatter.parse(dateTimeString)).toInstant());
    }

}
