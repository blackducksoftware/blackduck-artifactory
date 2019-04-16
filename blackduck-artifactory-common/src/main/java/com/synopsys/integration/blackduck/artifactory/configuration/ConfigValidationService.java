package com.synopsys.integration.blackduck.artifactory.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.blackduck.artifactory.configuration.model.ConfigValidationReport;
import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyGroupReport;
import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyValidationResult;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleRegistry;
import com.synopsys.integration.builder.BuilderStatus;

public class ConfigValidationService {
    private final int LINE_CHARACTER_LIMIT = 100;
    private final String LINE_SEPARATOR = System.lineSeparator();
    private final String BLOCK_SEPARATOR = LINE_SEPARATOR + StringUtils.repeat("-", LINE_CHARACTER_LIMIT) + LINE_SEPARATOR;

    private final ModuleRegistry moduleRegistry;
    private final PluginConfig pluginConfig;
    private final File versionFile;

    public ConfigValidationService(final ModuleRegistry moduleRegistry, final PluginConfig pluginConfig, final File versionFile) {
        this.moduleRegistry = moduleRegistry;
        this.pluginConfig = pluginConfig;
        this.versionFile = versionFile;
    }

    public ConfigValidationReport validateConfig() {
        final BuilderStatus generalBuilderStatus = new BuilderStatus();
        final PropertyGroupReport generalPropertyReport = new PropertyGroupReport("General Settings", generalBuilderStatus);
        pluginConfig.validate(generalPropertyReport);

        final List<PropertyGroupReport> propertyGroupReports = new ArrayList<>();
        for (final ModuleConfig moduleConfig : moduleRegistry.getAllModuleConfigs()) {
            final BuilderStatus propertyGroupBuilderStatus = new BuilderStatus();
            final PropertyGroupReport propertyGroupReport = new PropertyGroupReport(moduleConfig.getModuleName(), propertyGroupBuilderStatus);
            moduleConfig.validate(propertyGroupReport);
            propertyGroupReports.add(propertyGroupReport);
        }

        return new ConfigValidationReport(generalPropertyReport, propertyGroupReports);
    }

    public String generateStatusCheckMessage(final ConfigValidationReport configValidationReport) {
        final String pluginVersion = getPluginVersion();
        final StringBuilder statusCheckMessage = new StringBuilder(BLOCK_SEPARATOR + String.format("Status Check: Plugin Version - %s", pluginVersion) + BLOCK_SEPARATOR);

        final PropertyGroupReport generalPropertyReport = configValidationReport.getGeneralPropertyReport();
        final String configErrorMessage = generalPropertyReport.hasError() ? "CONFIGURATION ERROR" : "";
        statusCheckMessage.append(String.format("General Settings: %s", configErrorMessage)).append(LINE_SEPARATOR);
        appendPropertyGroupReport(statusCheckMessage, generalPropertyReport);
        statusCheckMessage.append(BLOCK_SEPARATOR);

        for (final PropertyGroupReport modulePropertyReport : configValidationReport.getModulePropertyReports()) {
            final Optional<ModuleConfig> moduleConfigsByName = moduleRegistry.getFirstModuleConfigByName(modulePropertyReport.getPropertyGroupName());
            final boolean enabled = moduleConfigsByName.isPresent() && moduleConfigsByName.get().isEnabled();
            appendPropertyReportForModule(statusCheckMessage, modulePropertyReport, enabled);
            statusCheckMessage.append(BLOCK_SEPARATOR);
        }

        return statusCheckMessage.toString();
    }

    private void appendPropertyReportForModule(final StringBuilder statusCheckMessage, final PropertyGroupReport propertyGroupReport, final boolean enabled) {
        final String moduleName = propertyGroupReport.getPropertyGroupName();
        final String state = enabled ? "Enabled" : "Disabled";
        final String configErrorMessage = propertyGroupReport.hasError() ? "CONFIGURATION ERROR" : "";
        final String moduleLine = String.format("%s [%s] %s", moduleName, state, configErrorMessage);
        statusCheckMessage.append(moduleLine).append(LINE_SEPARATOR);

        appendPropertyGroupReport(statusCheckMessage, propertyGroupReport);
    }

    private void appendPropertyGroupReport(final StringBuilder statusCheckMessage, final PropertyGroupReport propertyGroupReport) {
        for (final PropertyValidationResult propertyReport : propertyGroupReport.getPropertyReports()) {
            final Optional<String> errorMessage = propertyReport.getErrorMessage();

            final String mark = errorMessage.isPresent() ? "X" : "✔";
            final String property = propertyReport.getConfigurationProperty().getKey();
            final String reportSuffix = errorMessage.isPresent() ? String.format(LINE_SEPARATOR + "      * %s", errorMessage.get()) : "";
            final String reportLine = String.format("[%s] - %s %s", mark, property, reportSuffix);
            statusCheckMessage.append(wrapLine(reportLine)).append(LINE_SEPARATOR);
        }

        if (!propertyGroupReport.getBuilderStatus().isValid()) {
            final String otherMessages = wrapLine("Other Messages: " + propertyGroupReport.getBuilderStatus().getFullErrorMessage());
            statusCheckMessage.append(otherMessages).append(LINE_SEPARATOR);
        }
    }

    private String wrapLine(final String line) {
        return WordUtils.wrap(line, LINE_CHARACTER_LIMIT, LINE_SEPARATOR + "        ", true);
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
