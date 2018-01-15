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

import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperties
import com.blackducksoftware.integration.hub.artifactory.inspect.InspectionResults
import com.blackducksoftware.integration.hub.bdio.model.BdioComponent

@Component
class ComponentExtractor {
    @Autowired
    List<Extractor> extractors

    @Autowired
    ConfigurationProperties configurationProperties

    boolean shouldExtractComponent(String filename, Map jsonObject) {
        def lastUpdatedString = jsonObject.lastUpdated
        long lastUpdated = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(lastUpdatedString)).epochSecond
        long cutoffTime = ZonedDateTime.parse(configurationProperties.hubArtifactoryInspectLatestUpdatedCutoff, DateTimeFormatter.ofPattern(configurationProperties.hubArtifactoryDateTimePattern)).toEpochSecond()
        return lastUpdated >= cutoffTime
    }

    List<BdioComponent> extract(String artifactName, Map jsonObject, InspectionResults inspectionResults) {
        def components = []

        boolean extractAttempted = false
        for (Extractor extractor : extractors) {
            if (extractor.shouldAttemptExtract(artifactName, jsonObject)) {
                extractAttempted = true
                BdioComponent bdioComponent = extractor.extract(artifactName, jsonObject)
                if (bdioComponent != null) {
                    bdioComponent.id = jsonObject.downloadUri
                    components.add(bdioComponent)
                }
            }
        }

        if (extractAttempted) {
            inspectionResults.totalExtractAttempts++
        }

        return components
    }
}
