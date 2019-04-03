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

import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.blackduck.configuration.ConnectionResult;
import com.synopsys.integration.builder.BuilderStatus;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class PluginConfig extends ConfigurationValidator {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final String dateTimePattern;
    private final BlackDuckServerConfigBuilder blackDuckServerConfigBuilder;

    public static PluginConfig createFromProperties(final ConfigurationPropertyManager configurationPropertyManager) {
        final String dateTimePattern = configurationPropertyManager.getProperty(GeneralProperty.DATE_TIME_PATTERN);
        final BlackDuckServerConfigBuilder BlackDuckServerConfigBuilder = new BlackDuckServerConfigBuilder();
        BlackDuckServerConfigBuilder.setFromProperties(configurationPropertyManager.getProperties());

        return new PluginConfig(dateTimePattern, BlackDuckServerConfigBuilder);
    }

    public PluginConfig(final String dateTimePattern, final BlackDuckServerConfigBuilder BlackDuckServerConfigBuilder) {
        this.dateTimePattern = dateTimePattern;
        this.blackDuckServerConfigBuilder = BlackDuckServerConfigBuilder;
    }

    @Override
    public void validate(final BuilderStatus builderStatus) {
        final boolean dateTimePatternExists = validateNotBlank(builderStatus, GeneralProperty.DATE_TIME_PATTERN, dateTimePattern);

        if (dateTimePatternExists) {
            try {
                DateTimeFormatter.ofPattern(dateTimePattern);
            } catch (final IllegalArgumentException ignore) {
                builderStatus.addErrorMessage(String.format("Property %s has an invalid format", GeneralProperty.DATE_TIME_PATTERN.getKey()));
            }
        }

        final BuilderStatus blackDuckServerConfigBuilderStatus = blackDuckServerConfigBuilder.validateAndGetBuilderStatus();
        builderStatus.addAllErrorMessages(blackDuckServerConfigBuilderStatus.getErrorMessages());

        if (blackDuckServerConfigBuilderStatus.isValid()) {
            final BlackDuckServerConfig blackDuckServerConfig = blackDuckServerConfigBuilder.build();
            final ConnectionResult connectionResult = blackDuckServerConfig.attemptConnection(logger);

            if (connectionResult.isFailure()) {
                builderStatus.addErrorMessage("Failed to connect to Black Duck with provided configuration.");
                if (connectionResult.getFailureMessage().isPresent()) {
                    builderStatus.addErrorMessage(connectionResult.getFailureMessage().get());
                }
            }
        }
    }

    public String getDateTimePattern() {
        return dateTimePattern;
    }

    public BlackDuckServerConfigBuilder getBlackDuckServerConfigBuilder() {
        return blackDuckServerConfigBuilder;
    }
}
