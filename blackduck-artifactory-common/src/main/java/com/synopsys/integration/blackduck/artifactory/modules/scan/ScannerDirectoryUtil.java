/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scan;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.blackduck.artifactory.configuration.DirectoryConfig;
import com.synopsys.integration.exception.IntegrationException;

public class ScannerDirectoryUtil {
    public static final String DEFAULT_BLACKDUCK_SCANNER_DIRECTORY = "synopsys-blackduck-scanner";

    private final File rootScannerDirectory;
    private final File scannerCliDirectory;
    private final File scannerOutputDirectory;
    private final File scannerTargetsDirectory;

    public static ScannerDirectoryUtil createFromConfigs(DirectoryConfig directoryConfig, ScanModuleConfig scanModuleConfig) {
        String binariesDirectoryPath = scanModuleConfig.getBinariesDirectoryPath();
        String directoryPath = StringUtils.defaultIfBlank(binariesDirectoryPath, DEFAULT_BLACKDUCK_SCANNER_DIRECTORY);
        File rootScannerDirectory = new File(directoryConfig.getEtcDirectory(), directoryPath);
        return createDefault(rootScannerDirectory);
    }

    public static ScannerDirectoryUtil createDefault(File rootScannerDirectory) {
        File scannerCliDirectory = new File(rootScannerDirectory, "cli");
        File scannerOutputDirectory = new File(rootScannerDirectory, "cli-output");
        File scannerTargetsDirectory = new File(rootScannerDirectory, "cli-targets");
        return new ScannerDirectoryUtil(rootScannerDirectory, scannerCliDirectory, scannerOutputDirectory, scannerTargetsDirectory);
    }

    public ScannerDirectoryUtil(File rootScannerDirectory, File scannerCliDirectory, File scannerOutputDirectory, File scannerTargetsDirectory) {
        this.rootScannerDirectory = rootScannerDirectory;
        this.scannerCliDirectory = scannerCliDirectory;
        this.scannerOutputDirectory = scannerOutputDirectory;
        this.scannerTargetsDirectory = scannerTargetsDirectory;
    }

    public void createDirectories() throws IntegrationException {
        createDirectory(getRootScannerDirectory());
        createDirectory(getScannerCliDirectory());
        createDirectory(getScannerOutputDirectory());
        createDirectory(getScannerTargetsDirectory());
    }

    public File getRootScannerDirectory() {
        return rootScannerDirectory;
    }

    public File getScannerCliDirectory() {
        return scannerCliDirectory;
    }

    public File getScannerOutputDirectory() {
        return scannerOutputDirectory;
    }

    public File getScannerTargetsDirectory() {
        return scannerTargetsDirectory;
    }

    private void createDirectory(File directory) throws IntegrationException {
        try {
            if (!directory.exists() && !directory.mkdir()) {
                throw new IntegrationException(String.format("Failed to create directory: %s", directory.getCanonicalPath()));
            }
        } catch (IOException | IntegrationException e) {
            throw new IntegrationException(String.format("An exception occurred while setting up scanner directory: %s", directory.getPath()), e);
        }
    }
}
