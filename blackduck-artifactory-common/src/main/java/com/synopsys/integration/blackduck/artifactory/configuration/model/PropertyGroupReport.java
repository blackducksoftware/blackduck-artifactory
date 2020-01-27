/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.artifactory.configuration.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty;
import com.synopsys.integration.builder.BuilderStatus;

public class PropertyGroupReport {
    private final List<PropertyValidationResult> propertyReports = new ArrayList<>();

    private final String propertyGroupName;
    private final BuilderStatus builderStatus;

    public PropertyGroupReport(final String propertyGroupName, final BuilderStatus builderStatus) {
        this.propertyGroupName = propertyGroupName;
        this.builderStatus = builderStatus;
    }

    public void addPropertyValidationReport(final PropertyValidationResult propertyValidationResult) {
        propertyReports.add(propertyValidationResult);
    }

    public void addErrorMessage(final ConfigurationProperty configurationProperty, final String errorMessage) {
        final PropertyValidationResult propertyValidationResult = new PropertyValidationResult(configurationProperty, errorMessage);
        addPropertyValidationReport(propertyValidationResult);
    }

    public boolean hasError() {
        final boolean propertyReportErrorExists = propertyReports.stream()
                                                      .map(PropertyValidationResult::getErrorMessage)
                                                      .anyMatch(Optional::isPresent);

        return propertyReportErrorExists || !builderStatus.isValid();
    }

    public List<PropertyValidationResult> getPropertyReports() {
        return propertyReports;
    }

    public BuilderStatus getBuilderStatus() {
        return builderStatus;
    }

    public String getPropertyGroupName() {
        return propertyGroupName;
    }
}
