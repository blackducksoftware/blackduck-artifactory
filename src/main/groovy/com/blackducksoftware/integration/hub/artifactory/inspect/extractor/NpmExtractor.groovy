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
package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader
import com.blackducksoftware.integration.hub.bdio.model.BdioComponent
import com.blackducksoftware.integration.hub.bdio.model.Forge
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalId

import groovy.json.JsonSlurper

@Component
class NpmExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(NpmExtractor.class)

    @Autowired
    ArtifactoryDownloader artifactoryDownloader

    boolean shouldAttemptExtract(String artifactName, Map jsonObject) {
        def extension = getExtension(artifactName)
        'tgz' == extension || 'tar.gz' == extension
    }

    BdioComponent extract(String artifactName, Map jsonObject) {
        def tgzFile = artifactoryDownloader.download(jsonObject, artifactName)

        def tarArchiveInputStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(tgzFile)))
        BdioComponent bdioComponent = null
        try {
            def tarArchiveEntry
            while (null != (tarArchiveEntry = tarArchiveInputStream.getNextTarEntry())) {
                if ('package/package.json' == tarArchiveEntry.name) {
                    byte[] entryBuffer = decompressTarContents(logger, 'package/package.json', artifactName, tarArchiveInputStream, tarArchiveEntry)
                    String entryContent = new String(entryBuffer, StandardCharsets.UTF_8)

                    def npmPackageJson = new JsonSlurper().parseText(entryContent)
                    def packageName = npmPackageJson.name
                    def version = npmPackageJson.version

                    ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.NPM, packageName, version)
                    bdioComponent = bdioNodeFactory.createComponent(packageName, version, externalId)
                    return bdioComponent
                }
            }
        } finally {
            IOUtils.closeQuietly(tarArchiveInputStream)
        }
    }
}
