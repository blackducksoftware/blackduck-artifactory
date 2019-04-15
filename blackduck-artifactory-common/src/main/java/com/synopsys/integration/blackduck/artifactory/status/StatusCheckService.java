package com.synopsys.integration.blackduck.artifactory.status;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.configuration.PluginConfig;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleRegistry;
import com.synopsys.integration.builder.BuilderStatus;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class StatusCheckService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ModuleRegistry moduleRegistry;
    private final PluginConfig pluginConfig;
    private final File versionFile;

    public StatusCheckService(final ModuleRegistry moduleRegistry, final PluginConfig pluginConfig, final File versionFile) {
        this.moduleRegistry = moduleRegistry;
        this.pluginConfig = pluginConfig;
        this.versionFile = versionFile;
    }

    public String logStatusCheckMessage() {
        final String lineSeparator = System.lineSeparator();
        final String blockSeparator = lineSeparator + StringUtils.repeat("-", 100) + lineSeparator;

        final String pluginVersion = getPluginVersion();
        final StringBuilder statusCheckMessage = new StringBuilder(blockSeparator + String.format("Status Check: Plugin Version - %s", pluginVersion) + blockSeparator);

        statusCheckMessage.append("General Settings:").append(lineSeparator);
        final BuilderStatus generalBuilderStatus = new BuilderStatus();
        pluginConfig.validate(generalBuilderStatus);
        if (generalBuilderStatus.isValid()) {
            statusCheckMessage.append("General properties validated").append(lineSeparator);
            statusCheckMessage.append("Connection to BlackDuck successful");
        } else {
            statusCheckMessage.append(generalBuilderStatus.getFullErrorMessage(lineSeparator));
        }
        statusCheckMessage.append(blockSeparator);

        for (final ModuleConfig moduleConfig : moduleRegistry.getAllModuleConfigs()) {
            statusCheckMessage.append(String.format("Module Name: %s", moduleConfig.getModuleName())).append(lineSeparator);
            statusCheckMessage.append(String.format("Enabled: %b", moduleConfig.isEnabled())).append(lineSeparator);
            final BuilderStatus builderStatus = new BuilderStatus();
            moduleConfig.validate(builderStatus);

            if (builderStatus.isValid()) {
                statusCheckMessage.append(String.format("%s has passed validation", moduleConfig.getModuleName()));
            } else {
                statusCheckMessage.append(builderStatus.getFullErrorMessage(lineSeparator));
            }

            statusCheckMessage.append(blockSeparator);
        }

        final String finalMessage = statusCheckMessage.toString();
        logger.info(finalMessage);

        return finalMessage;
    }

    private String getPluginVersion() {
        String version = "Unknown";
        try {
            version = FileUtils.readFileToString(versionFile, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return version;
    }
}
