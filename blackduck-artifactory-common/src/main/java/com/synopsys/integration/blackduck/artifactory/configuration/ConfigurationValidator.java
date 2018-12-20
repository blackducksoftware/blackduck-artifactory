package com.synopsys.integration.blackduck.artifactory.configuration;

import java.time.format.DateTimeParseException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.util.BuilderStatus;

public abstract class ConfigurationValidator {
    public abstract void validate(final BuilderStatus builderStatus);

    protected void validateDate(final BuilderStatus builderStatus, final ConfigurationProperty property, final String date, final DateTimeManager dateTimeManager) {
        if (StringUtils.isNotBlank(date)) {
            try {
                dateTimeManager.getDateFromString(date);
            } catch (final DateTimeParseException ignored) {
                builderStatus.addErrorMessage(String.format("Property %s is set to %s which does not match the format %s", property.getKey(), date, dateTimeManager.getDateTimePattern()));
            }
        }
    }

    // TODO: Perhaps ensure that the cron expression is valid within artifactory
    protected void validateCronExpression(final BuilderStatus builderStatus, final ConfigurationProperty property, final String cronExpression) {
        validateNotBlank(builderStatus, property, cronExpression, "Please set it to a valid quartz cron expression");
    }

    protected boolean validateNotBlank(final BuilderStatus builderStatus, final ConfigurationProperty property, final String value) {
        return validateNotBlank(builderStatus, property, value, "It must be set.");
    }

    protected boolean validateNotBlank(final BuilderStatus builderStatus, final ConfigurationProperty property, final String value, final String errorMessage) {
        if (StringUtils.isBlank(value)) {
            builderStatus.addErrorMessage(String.format("Property %s is blank. %s", property.getKey(), errorMessage));
            return false;
        }
        return true;
    }

    protected boolean validateNotNull(final BuilderStatus builderStatus, final ConfigurationProperty property, final Object value) {
        return validateNotNull(builderStatus, property, value, "");
    }

    protected boolean validateNotNull(final BuilderStatus builderStatus, final ConfigurationProperty property, final Object value, final String errorMessage) {
        if (value == null) {
            builderStatus.addErrorMessage(String.format("Property %s not set. %s", property.getKey(), errorMessage));
            return false;
        }
        return true;
    }

    protected void validateList(final BuilderStatus builderStatus, final ConfigurationProperty property, final List<?> list, final String errorMessage) {
        final boolean notNull = validateNotNull(builderStatus, property, list);
        if (notNull && list.isEmpty()) {
            builderStatus.addErrorMessage(String.format("Property %s is empty. %s", property.getKey(), errorMessage));
        }
    }

    protected void validateBoolean(final BuilderStatus builderStatus, final ConfigurationProperty property, final Boolean value) {
        validateNotNull(builderStatus, property, value, "Please set to either 'true' or 'false'");
    }

    protected void validateInteger(final BuilderStatus builderStatus, final ConfigurationProperty property, final Integer value) {
        validateNotNull(builderStatus, property, value, "Please specify a valid integer");
    }
}
