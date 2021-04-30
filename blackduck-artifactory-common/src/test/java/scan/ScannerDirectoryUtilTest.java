/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package scan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.blackduck.artifactory.modules.scan.ScannerDirectoryUtil;

class ScannerDirectoryUtilTest {
    @Test
    void createDirectoriesTest() throws IOException {
        File tempDirectory = Files.createTempDirectory("test-scanner-directory").toFile();
        File rootScannerDirectory = new File(tempDirectory, ScannerDirectoryUtil.DEFAULT_BLACKDUCK_SCANNER_DIRECTORY);
        ScannerDirectoryUtil scannerDirectoryUtil = ScannerDirectoryUtil.createDefault(rootScannerDirectory);

        Assertions.assertDoesNotThrow(() -> {
            scannerDirectoryUtil.createDirectories();
            testDirectoryExists(scannerDirectoryUtil.getRootScannerDirectory());
            testDirectoryExists(scannerDirectoryUtil.getScannerCliDirectory());
            testDirectoryExists(scannerDirectoryUtil.getScannerOutputDirectory());
            testDirectoryExists(scannerDirectoryUtil.getScannerTargetsDirectory());
        });

        FileUtils.deleteDirectory(tempDirectory);
    }

    private void testDirectoryExists(File directory) {
        Assertions.assertTrue(directory.exists(), String.format("The created directory does not exist: %s", directory.getAbsolutePath()));
    }
}
