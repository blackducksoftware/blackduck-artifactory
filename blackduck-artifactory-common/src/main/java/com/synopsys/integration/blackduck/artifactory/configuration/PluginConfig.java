package com.synopsys.integration.blackduck.artifactory.configuration;

import java.time.format.DateTimeFormatter;

import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.BuilderStatus;

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
            final boolean canConnect = blackDuckServerConfig.canConnect(logger); // TODO: Improvements for this in blackduck-common:40.1.0

            if (!canConnect) {
                builderStatus.addErrorMessage("Failed to connect to Black Duck with provided configuration");
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
