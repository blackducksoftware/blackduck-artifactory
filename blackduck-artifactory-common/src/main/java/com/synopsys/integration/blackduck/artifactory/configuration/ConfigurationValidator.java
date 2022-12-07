/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.configuration;

import java.time.format.DateTimeParseException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyGroupReport;
import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyValidationResult;

public abstract class ConfigurationValidator {
    public abstract void validate(PropertyGroupReport propertyGroupReport, List<String> enabledModuleNames);

    protected void validateDate(PropertyGroupReport statusReport, ConfigurationProperty property, String date, DateTimeManager dateTimeManager) {
        if (StringUtils.isNotBlank(date) && dateTimeManager != null) {
            boolean passed = true;
            try {
                dateTimeManager.getDateFromString(date);
            } catch (DateTimeParseException ignored) {
                statusReport.addErrorMessage(property, String.format("Property %s is set to %s which does not match the format %s", property.getKey(), date, dateTimeManager.getDateTimePattern()));
                passed = false;
            }
            if (passed) {
                statusReport.addPropertyValidationReport(new PropertyValidationResult(property));
            }
        } else if (StringUtils.isBlank(date)) {
            statusReport.addErrorMessage(property, String.format("Property %s is not set", property.getKey()));
        } else {
            statusReport.addErrorMessage(property, "DateTimeManager not set");
        }
    }

    // TODO: Perhaps ensure that the cron expression is valid within artifactory
    protected void validateCronExpression(PropertyGroupReport statusReport, ConfigurationProperty property, String cronExpression) {
        validateNotBlank(statusReport, property, cronExpression, "Please set it to a valid quartz cron expression.");
    }

    protected boolean validateNotBlank(PropertyGroupReport statusReport, ConfigurationProperty property, String value) {
        return validateNotBlank(statusReport, property, value, "This property is required to be set.");
    }

    protected boolean validateNotBlank(PropertyGroupReport statusReport, ConfigurationProperty property, String value, String errorMessage) {
        return validateNotNull(statusReport, property, StringUtils.stripToNull(value), errorMessage);
    }

    protected boolean validateNotNull(PropertyGroupReport statusReport, ConfigurationProperty property, Object value) {
        return validateNotNull(statusReport, property, value, "This property is required to be set.");
    }

    private boolean validateNotNull(PropertyGroupReport statusReport, ConfigurationProperty property, Object value, String errorMessage) {
        if (value == null) {
            String message = String.format("Property %s not set. %s", property.getKey(), errorMessage);
            PropertyValidationResult propertyValidationResult = new PropertyValidationResult(property, message);
            statusReport.addPropertyValidationReport(propertyValidationResult);
            return false;
        } else {
            statusReport.addPropertyValidationReport(new PropertyValidationResult(property));
        }

        return true;
    }

    protected void validateList(PropertyGroupReport statusReport, ConfigurationProperty property, List<?> list) {
        validateList(statusReport, property, list, "Please provide a comma separated list of values.");
    }

    protected void validateList(PropertyGroupReport statusReport, ConfigurationProperty property, List<?> list, String errorMessage) {
        if (list != null && list.isEmpty()) {
            PropertyValidationResult propertyValidationResult = new PropertyValidationResult(property, String.format("Property %s is empty. %s", property.getKey(), errorMessage));
            statusReport.addPropertyValidationReport(propertyValidationResult);
        } else {
            validateNotNull(statusReport, property, list);
        }
    }

    protected void validateBoolean(PropertyGroupReport statusReport, ConfigurationProperty property, Boolean value) {
        validateNotNull(statusReport, property, value, "Please set to either 'true' or 'false'.");
    }

    protected void validateInteger(PropertyGroupReport statusReport, ConfigurationProperty property, Integer value) {
        validateNotNull(statusReport, property, value, "Please specify a valid integer.");
    }

    protected void validateInteger(PropertyGroupReport statusReport, ConfigurationProperty property, Integer value, Integer min, Integer max) {
        validateInteger(statusReport, property, value);
        if (value != null && (value < min || value > max)) {
            statusReport.addErrorMessage(property, String.format("Please specify a valid integer between the range of %s and %s.", min.toString(), max.toString()));
        }
    }
}
