/*
 * hub-artifactory
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.blackducksoftware.integration.hub.artifactory.scan

import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.artifactory.Application
import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperties

@Component
class ArtifactoryScanConfigurer {
    private final Logger logger = LoggerFactory.getLogger(ArtifactoryScanConfigurer.class)

    @Autowired
    ConfigurationProperties configurationProperties

    void createScanPluginFile() {
        String path = Application.class.getProtectionDomain().getCodeSource().getLocation().getPath()
        String decodedPath = URLDecoder.decode(path, 'UTF-8')
        decodedPath = decodedPath.replace('!/BOOT-INF/classes!/', '')
        decodedPath = decodedPath.replaceFirst('file:', '')

        def jarFile = new File(decodedPath)
        def jarFileParentDirectory = new File(jarFile.parent)
        def scannerDirectory = new File(jarFileParentDirectory, 'scanner')
        def scriptFile = new File(scannerDirectory, 'blackDuckScanForHub.groovy')
        String scanFileText = scriptFile.text

        String stringPrefix = '@Field final String '
        String intPrefix = '@Field final int '
        String boolPrefix = '@Field final boolean '

        scanFileText = replaceText(scanFileText, stringPrefix, 'HUB_URL', configurationProperties.hubUrl)
        scanFileText = replaceValue(scanFileText, intPrefix, 'HUB_TIMEOUT', '' == configurationProperties.hubTimeout ? '120' : configurationProperties.hubTimeout)
        scanFileText = replaceText(scanFileText, stringPrefix, 'HUB_USERNAME', configurationProperties.hubUsername)
        scanFileText = replaceText(scanFileText, stringPrefix, 'HUB_PASSWORD', configurationProperties.hubPassword)
        scanFileText = replaceValue(scanFileText, boolPrefix, 'HUB_ALWAYS_TRUST_CERTS', "${Boolean.parseBoolean(configurationProperties.hubAlwaysTrustCerts)}")
        scanFileText = replaceText(scanFileText, stringPrefix, 'HUB_PROXY_HOST', configurationProperties.hubProxyHost)
        scanFileText = replaceValue(scanFileText, intPrefix, 'HUB_PROXY_PORT', '' == configurationProperties.hubProxyPort ? '0' : configurationProperties.hubProxyPort)
        scanFileText = replaceText(scanFileText, stringPrefix, 'HUB_PROXY_USERNAME', configurationProperties.hubProxyUsername)
        scanFileText = replaceText(scanFileText, stringPrefix, 'HUB_PROXY_PASSWORD', configurationProperties.hubProxyPassword)
        scanFileText = replaceText(scanFileText, stringPrefix, 'ARTIFACTORY_REPOS_TO_SEARCH', configurationProperties.hubArtifactoryScanReposToSearch)
        scanFileText = replaceText(scanFileText, stringPrefix, 'ARTIFACT_NAME_PATTERNS_TO_SCAN', configurationProperties.hubArtifactoryScanNamePatterns)
        scanFileText = replaceText(scanFileText, stringPrefix, 'BLACK_DUCK_SCAN_BINARIES_DIRECTORY_PATH', configurationProperties.hubArtifactoryScanBinariesDirectoryPath)

        constructScanZip(scanFileText, scannerDirectory)
    }

    private String replaceText(String s, String prefix, String label, String newValue) {
        s.replaceFirst("${prefix}${label}=.*\n", "${prefix}${label}=\"${newValue}\"\n")
    }

    private String replaceValue(String s, String prefix, String label, String newValue) {
        s.replaceFirst("${prefix}${label}=.*\n", "${prefix}${label}=${newValue}\n")
    }

    private void constructScanZip(String scanFileText, File scannerDirectory) {
        File zipFile = new File(configurationProperties.hubArtifactoryWorkingDirectoryPath, 'blackDuckScanForHub.zip')
        logger.info("Creating zip file: ${zipFile.absolutePath}")
        ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))

        InputStream scriptInputStream = new ByteArrayInputStream(scanFileText.getBytes(StandardCharsets.UTF_8))
        copyInputStream('blackDuckScanForHub.groovy', scriptInputStream, zipOutputStream)

        File libDirectory = new File(scannerDirectory, 'lib')
        for (File lib : libDirectory.listFiles()) {
            String libName = FilenameUtils.getName(lib.name)
            copyInputStream("lib/${libName}", new FileInputStream(lib), zipOutputStream)
        }

        IOUtils.closeQuietly(zipOutputStream)
        logger.info("Zip file: ${zipFile.absolutePath} created successfully.")
    }

    private void copyInputStream(String entryName, InputStream inputStream, ZipOutputStream zipOutputStream) {
        ZipEntry zipEntry = new ZipEntry(entryName)
        zipOutputStream.putNextEntry(zipEntry)
        IOUtils.copy(inputStream, zipOutputStream)
        IOUtils.closeQuietly(inputStream)
    }
}