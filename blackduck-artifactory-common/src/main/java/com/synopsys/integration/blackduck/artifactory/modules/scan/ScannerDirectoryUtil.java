/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scan;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import com.synopsys.integration.blackduck.artifactory.configuration.DirectoryConfig;
import com.synopsys.integration.exception.IntegrationException;

public class ScannerDirectoryUtil {
    private static final String DEFAULT_BLACKDUCK_SCANNER_DIRECTORY = "synopsys-blackduck-scanner";

    private final DirectoryConfig directoryConfig;
    private final ScanModuleConfig scanModuleConfig;

    public ScannerDirectoryUtil(DirectoryConfig directoryConfig, ScanModuleConfig scanModuleConfig) {
        this.directoryConfig = directoryConfig;
        this.scanModuleConfig = scanModuleConfig;
    }

    public File createScannerDirectory() throws IntegrationException {
        File directory = determineScannerDirectory();
        return createDirectory(directory);
    }

    public File createScannerCliInstallDirectory() throws IntegrationException {
        File scannerDirectory = determineScannerDirectory();
        File directory = new File(scannerDirectory, "cli");
        return createDirectory(directory);
    }

    public File createScannerOutputDirectory() throws IntegrationException {
        File scannerDirectory = determineScannerDirectory();
        File directory = new File(scannerDirectory, "cli-output");
        return createDirectory(directory);
    }

    public File determineScannerTargetDirectory() {
        File scannerDirectory = determineScannerDirectory();
        return new File(scannerDirectory, "targets");
    }

    public File createScannerTargetsDirectory() throws IntegrationException {
        File directory = determineScannerTargetDirectory();
        return createDirectory(directory);
    }

    public File determineScannerDirectory() {
        String binariesDirectoryPath = scanModuleConfig.getBinariesDirectoryPath();
        return determineScannerDirectory(directoryConfig.getEtcDirectory(), binariesDirectoryPath);
    }

    private File determineScannerDirectory(File artifactoryEtcDirectory, @Nullable String overridePath) {
        String directoryPath = StringUtils.defaultIfBlank(overridePath, DEFAULT_BLACKDUCK_SCANNER_DIRECTORY);
        return new File(artifactoryEtcDirectory, directoryPath);
    }

    private File createDirectory(File directory) throws IntegrationException {
        try {
            if (!directory.exists() && !directory.mkdir()) {
                throw new IntegrationException(String.format("Failed to create directory: %s", directory.getCanonicalPath()));
            }
            return directory;
        } catch (IOException | IntegrationException e) {
            throw new IntegrationException(String.format("An exception occurred while setting up the %s directory", directory.getPath()), e);
        }
    }
}
