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

import java.util.zip.ZipFile

import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader
import com.blackducksoftware.integration.hub.bdio.model.BdioComponent
import com.blackducksoftware.integration.hub.bdio.model.Forge
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalId

@Component
class NugetExtractor extends Extractor {
    @Autowired
    ArtifactoryDownloader artifactoryDownloader

    boolean shouldAttemptExtract(String artifactName, Map jsonObject) {
        def extension = getExtension(artifactName)
        'nupkg' == extension
    }

    BdioComponent extract(String artifactName, Map jsonObject) {
        def file = artifactoryDownloader.download(jsonObject, artifactName)

        BdioComponent bdioComponent = null
        def nupkgFile = new ZipFile(file)
        try {
            def nuspecZipEntry = nupkgFile.entries().find {
                it.name.endsWith('.nuspec')
            }

            def nuspecPackage = new XmlSlurper().parse(nupkgFile.getInputStream(nuspecZipEntry))
            def packageName = nuspecPackage.metadata.id.toString()
            def version = nuspecPackage.metadata.version.toString()

            ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.NUGET, packageName, version)
            bdioComponent = bdioNodeFactory.createComponent(packageName, version, externalId)
            return bdioComponent
        } finally {
            IOUtils.closeQuietly(nupkgFile)
        }
    }
}
