package com.synopsys.integration.blackduck.artifactory.configuration;

import java.time.format.DateTimeFormatter;

import com.synopsys.integration.blackduck.configuration.HubServerConfigBuilder;
import com.synopsys.integration.util.BuilderStatus;

public class PluginConfig extends ConfigurationValidator {
    private final String dateTimePattern;
    private final HubServerConfigBuilder hubServerConfigBuilder;

    public static PluginConfig createFromProperties(final ConfigurationPropertyManager configurationPropertyManager) {
        final String dateTimePattern = configurationPropertyManager.getProperty(GeneralProperty.DATE_TIME_PATTERN);
        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        hubServerConfigBuilder.setFromProperties(configurationPropertyManager.getProperties());

        return new PluginConfig(dateTimePattern, hubServerConfigBuilder);
    }

    public PluginConfig(final String dateTimePattern, final HubServerConfigBuilder hubServerConfigBuilder) {
        this.dateTimePattern = dateTimePattern;
        this.hubServerConfigBuilder = hubServerConfigBuilder;
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

        final BuilderStatus blackDuckServerConfigBuilderStatus = hubServerConfigBuilder.validateAndGetBuilderStatus();
        builderStatus.addAllErrorMessages(blackDuckServerConfigBuilderStatus.getErrorMessages());
    }

    public String getDateTimePattern() {
        return dateTimePattern;
    }

    public HubServerConfigBuilder getHubServerConfigBuilder() {
        return hubServerConfigBuilder;
    }
}
