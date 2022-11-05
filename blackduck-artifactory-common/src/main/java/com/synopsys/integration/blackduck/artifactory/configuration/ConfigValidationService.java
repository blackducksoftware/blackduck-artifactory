/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
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
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.configuration.model.ConfigValidationReport;
import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyGroupReport;
import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyValidationResult;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleManager;
import com.synopsys.integration.blackduck.artifactory.modules.scaas.ScanAsAServiceModule;
import com.synopsys.integration.builder.BuilderStatus;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ConfigValidationService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private static final int LINE_CHARACTER_LIMIT = 100;
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final String BLOCK_SEPARATOR = LINE_SEPARATOR + StringUtils.repeat("-", LINE_CHARACTER_LIMIT) + LINE_SEPARATOR;

    private final ModuleManager moduleManager;
    private final PluginConfig pluginConfig;
    private final File versionFile;

    public ConfigValidationService(ModuleManager moduleManager, PluginConfig pluginConfig, File versionFile) {
        this.moduleManager = moduleManager;
        this.pluginConfig = pluginConfig;
        this.versionFile = versionFile;
    }

    public ConfigValidationReport validateConfig() {
        BuilderStatus generalBuilderStatus = new BuilderStatus();
        PropertyGroupReport generalPropertyReport = new PropertyGroupReport("General Settings", generalBuilderStatus);
        List<String> enabedModules = moduleManager.getAllEnabledModules();
        pluginConfig.validate(generalPropertyReport, enabedModules);

        List<PropertyGroupReport> propertyGroupReports = new ArrayList<>();
        for (ModuleConfig moduleConfig : moduleManager.getAllModuleConfigs()) {
            BuilderStatus propertyGroupBuilderStatus = new BuilderStatus();
            PropertyGroupReport propertyGroupReport = new PropertyGroupReport(moduleConfig.getModuleName(), propertyGroupBuilderStatus);
            moduleConfig.validate(propertyGroupReport, enabedModules);
            propertyGroupReports.add(propertyGroupReport);
        }

        return new ConfigValidationReport(generalPropertyReport, propertyGroupReports);
    }

    public String generateStatusCheckMessage(ConfigValidationReport configValidationReport, boolean includeValid) {
        String pluginVersion = getPluginVersion();
        StringBuilder statusCheckMessage = new StringBuilder(BLOCK_SEPARATOR + String.format("Status Check: Plugin Version - %s", pluginVersion) + BLOCK_SEPARATOR);

        PropertyGroupReport generalPropertyReport = configValidationReport.getGeneralPropertyReport();
        String configErrorMessage = generalPropertyReport.hasError() ? "CONFIGURATION ERROR" : "";
        statusCheckMessage.append(String.format("General Settings: %s", configErrorMessage)).append(LINE_SEPARATOR);
        appendPropertyGroupReport(statusCheckMessage, generalPropertyReport, includeValid);
        statusCheckMessage.append(BLOCK_SEPARATOR);

        for (PropertyGroupReport modulePropertyReport : configValidationReport.getModulePropertyReports()) {
            Optional<ModuleConfig> moduleConfigsByName = moduleManager.getFirstModuleConfigByName(modulePropertyReport.getPropertyGroupName());
            boolean enabled = moduleConfigsByName.isPresent() && moduleConfigsByName.get().isEnabled();
            appendPropertyReportForModule(statusCheckMessage, modulePropertyReport, enabled, includeValid);
            statusCheckMessage.append(BLOCK_SEPARATOR);
        }

        return statusCheckMessage.toString();
    }

    private void appendPropertyReportForModule(StringBuilder statusCheckMessage, PropertyGroupReport propertyGroupReport, boolean enabled, boolean includeValid) {
        String moduleName = propertyGroupReport.getPropertyGroupName();
        String state = enabled ? "Enabled" : "Disabled";
        String configErrorMessage = propertyGroupReport.hasError() ? "CONFIGURATION ERROR" : "";
        String moduleLine = String.format("%s [%s] %s", moduleName, state, configErrorMessage);
        statusCheckMessage.append(moduleLine).append(LINE_SEPARATOR);

        appendPropertyGroupReport(statusCheckMessage, propertyGroupReport, includeValid);
    }

    private void appendPropertyGroupReport(StringBuilder statusCheckMessage, PropertyGroupReport propertyGroupReport, boolean includeValid) {
        for (PropertyValidationResult propertyReport : propertyGroupReport.getPropertyReports()) {
            Optional<String> errorMessage = propertyReport.getErrorMessage();

            String mark = errorMessage.isPresent() ? "X" : "✔";
            String property = propertyReport.getConfigurationProperty().getKey();
            String reportSuffix = errorMessage.isPresent() ? String.format(LINE_SEPARATOR + "      * %s", errorMessage.get()) : "";
            String reportLine = String.format("[%s] - %s %s", mark, property, reportSuffix);

            if (includeValid || errorMessage.isPresent()) {
                statusCheckMessage.append(wrapLine(reportLine)).append(LINE_SEPARATOR);
            }
        }

        if (!propertyGroupReport.getBuilderStatus().isValid()) {
            String otherMessages = wrapLine(String.format("Other Messages: %s", propertyGroupReport.getBuilderStatus().getFullErrorMessage()));
            statusCheckMessage.append(otherMessages).append(LINE_SEPARATOR);
        }
    }

    private String wrapLine(String line) {
        return WordUtils.wrap(line, LINE_CHARACTER_LIMIT, LINE_SEPARATOR + "        ", false);
    }

    private String getPluginVersion() {
        String version = "Unknown";
        try {
            version = FileUtils.readFileToString(versionFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.debug("Failed to load plugin version.", e);
        }

        return version;
    }
}
