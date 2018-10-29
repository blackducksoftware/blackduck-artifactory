package com.synopsys.integration.blackduck.artifactory.jupiter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.artifactory.DateTimeManager;

class DateTimeManagerTest {
    private final String dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private final String dateAsString = "2016-01-01T00:00:00.000";
    private final long dateAsMilliseconds = 1451606400000L;

    private DateTimeManager dateTimeManager;

    @BeforeEach
    void setUp() {
        dateTimeManager = new DateTimeManager(dateTimePattern);
    }

    @Test
    void getDateTimePattern() {
        assertEquals(dateTimePattern, dateTimeManager.getDateTimePattern());
    }

    @Test
    void getTimeFromString() {
        final long actualTime = dateTimeManager.getTimeFromString(dateAsString);
        assertEquals(dateAsMilliseconds, actualTime);
    }

    @Test
    void getStringFromDate() {
        final Date providedDate = new Date(dateAsMilliseconds);
        assertEquals(dateAsString, dateTimeManager.getStringFromDate(providedDate));
    }

    @Test
    void getDateFromString() {
        final Date actualDate = dateTimeManager.getDateFromString(dateAsString);
        final Date expectedDate = new Date(dateAsMilliseconds);
        assertEquals(expectedDate, actualDate);
    }
}