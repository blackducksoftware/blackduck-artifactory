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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.model.BdioComponent
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalId

@Component
class JarExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(JarExtractor.class)

    boolean shouldAttemptExtract(String artifactName, Map jsonObject) {
        def extension = getExtension(artifactName)
        'jar' == extension
    }

    BdioComponent extract(String artifactName, Map jsonObject) {
        def jarPath = jsonObject.path
        if (jarPath.endsWith('-javadoc.jar') || jarPath.endsWith('-sources.jar')) {
            return null
        }

        def gavPieces = parseGav(jarPath)
        if (gavPieces == null) {
            return null
        }

        String group = gavPieces[0]
        String artifact = gavPieces[1]
        String version = gavPieces[2]

        ExternalId externalId = externalIdFactory.createMavenExternalId(group, artifact, version)
        BdioComponent bdioComponent = bdioNodeFactory.createComponent(artifact, version, externalId)

        bdioComponent
    }

    def parseGav(String path) {
        try {
            def pathPieces = path.tokenize('/')
            def version = pathPieces[-2]
            def artifact = pathPieces[-3]
            def group = pathPieces[0..-4].join('.')

            [group, artifact, version]
        } catch (Exception e) {
            logger.error("Couldn't parse the gav from ${path}")

            null
        }
    }
}
