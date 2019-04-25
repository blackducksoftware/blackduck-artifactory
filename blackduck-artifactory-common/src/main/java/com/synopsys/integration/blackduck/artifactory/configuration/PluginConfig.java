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
package com.synopsys.integration.blackduck.artifactory.configuration;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyGroupReport;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.blackduck.configuration.ConnectionResult;
import com.synopsys.integration.builder.BuilderStatus;
import com.synopsys.integration.log.SilentIntLogger;

public class PluginConfig extends ConfigurationValidator {
    private final String dateTimePattern;
    private final String dateTimeZone;
    private final String blackDuckUrl;
    private final Integer timeout;
    private final Boolean trustCert;
    private final Set<Map.Entry<String, String>> properties;

    public static PluginConfig createFromProperties(final ConfigurationPropertyManager configurationPropertyManager) {
        final String dateTimePattern = configurationPropertyManager.getProperty(GeneralProperty.DATE_TIME_PATTERN);
        final String dateTimeZone = configurationPropertyManager.getProperty(GeneralProperty.DATE_TIME_ZONE);

        final String url = configurationPropertyManager.getProperty(GeneralProperty.URL);
        final Integer timeout = configurationPropertyManager.getIntegerProperty(GeneralProperty.TIMEOUT);
        final Boolean trustCert = configurationPropertyManager.getBooleanProperty(GeneralProperty.TRUST_CERT);
        final Set<Map.Entry<String, String>> properties = configurationPropertyManager.getPropertyEntries();

        return new PluginConfig(dateTimePattern, dateTimeZone, url, timeout, trustCert, properties);
    }

    public PluginConfig(final String dateTimePattern, final String dateTimeZone, final String blackDuckUrl, final Integer timeout, final Boolean trustCert, final Set<Map.Entry<String, String>> properties) {
        this.dateTimePattern = dateTimePattern;
        this.dateTimeZone = dateTimeZone;
        this.blackDuckUrl = blackDuckUrl;
        this.timeout = timeout;
        this.trustCert = trustCert;
        this.properties = properties;
    }

    @Override
    public void validate(final PropertyGroupReport propertyGroupReport) {
        final boolean dateTimePatternExists = validateNotBlank(propertyGroupReport, GeneralProperty.DATE_TIME_PATTERN, dateTimePattern);

        if (dateTimePatternExists) {
            try {
                DateTimeFormatter.ofPattern(dateTimePattern);
            } catch (final IllegalArgumentException e) {
                propertyGroupReport.addErrorMessage(GeneralProperty.DATE_TIME_PATTERN, "Invalid format. See logs for details");
            }
        }

        validateNotBlank(propertyGroupReport, GeneralProperty.URL, blackDuckUrl);

        validateInteger(propertyGroupReport, GeneralProperty.TIMEOUT, timeout);
        validateBoolean(propertyGroupReport, GeneralProperty.TRUST_CERT, trustCert);

        final BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = getBlackDuckServerConfigBuilder();
        final BuilderStatus blackDuckServerConfigBuilderStatus = blackDuckServerConfigBuilder.validateAndGetBuilderStatus();

        if (blackDuckServerConfigBuilderStatus.isValid()) {
            final BlackDuckServerConfig blackDuckServerConfig = blackDuckServerConfigBuilder.build();
            final ConnectionResult connectionResult = blackDuckServerConfig.attemptConnection(new SilentIntLogger());

            if (connectionResult.isFailure()) {
                blackDuckServerConfigBuilderStatus.addErrorMessage("Failed to connect to Black Duck.");
                if (connectionResult.getFailureMessage().isPresent()) {
                    blackDuckServerConfigBuilderStatus.addErrorMessage(connectionResult.getFailureMessage().get());
                }
            }
        }

        propertyGroupReport.getBuilderStatus().addAllErrorMessages(blackDuckServerConfigBuilderStatus.getErrorMessages());
    }

    public String getDateTimePattern() {
        return dateTimePattern;
    }

    public String getDateTimeZone() {
        return dateTimeZone;
    }

    public BlackDuckServerConfigBuilder getBlackDuckServerConfigBuilder() {
        final BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = new BlackDuckServerConfigBuilder();
        blackDuckServerConfigBuilder.setProperties(properties);
        return blackDuckServerConfigBuilder;
    }
}
