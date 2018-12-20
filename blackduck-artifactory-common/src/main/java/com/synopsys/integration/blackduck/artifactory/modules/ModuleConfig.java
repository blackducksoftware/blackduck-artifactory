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
package com.synopsys.integration.blackduck.artifactory.modules;

import java.time.format.DateTimeParseException;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.blackduck.artifactory.ConfigurationProperty;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.util.BuilderStatus;

public abstract class ModuleConfig {
    private final String moduleName;
    private final Boolean enabledProperty;
    private boolean enabled;

    public ModuleConfig(final String moduleName, final Boolean enabled) {
        this.moduleName = moduleName;
        this.enabledProperty = enabled;
        this.enabled = BooleanUtils.toBoolean(enabledProperty);
    }

    public abstract void validate(final BuilderStatus builderStatus);

    public String getModuleName() {
        return moduleName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * For validation
     */
    public Boolean isEnabledUnverified() {
        return enabledProperty;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    protected void validateDate(final BuilderStatus builderStatus, final ConfigurationProperty property, final String date, final DateTimeManager dateTimeManager) {
        if (StringUtils.isBlank(date)) {
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

    protected void validateNotBlank(final BuilderStatus builderStatus, final ConfigurationProperty property, final String value, final String errorMessage) {
        if (StringUtils.isBlank(value)) {
            builderStatus.addErrorMessage(String.format("Property %s is blank. %s", property.getKey(), errorMessage));
        }
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
