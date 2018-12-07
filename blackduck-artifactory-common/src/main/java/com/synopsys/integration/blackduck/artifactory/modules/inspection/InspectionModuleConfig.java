/**
 * blackduck-artifactory-common
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
package com.synopsys.integration.blackduck.artifactory.modules.inspection;

import java.util.List;

import com.synopsys.integration.blackduck.artifactory.BlackDuckPropertyManager;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;

public class InspectionModuleConfig extends ModuleConfig {
    private final String blackDuckIdentifyArtifactsCron;
    private final String blackDuckPopulateMetadataCron;
    private final String blackDuckUpdateMetadataCron;
    private final String blackDuckAddPendingArtifactsCron;

    private final List<String> repoKeys;

    public InspectionModuleConfig(final boolean enabled, final String blackDuckIdentifyArtifactsCron, final String blackDuckPopulateMetadataCron, final String blackDuckUpdateMetadataCron,
        final String blackDuckAddPendingArtifactsCron, final List<String> repoKeys) {
        super(InspectionModule.class.getSimpleName(), enabled);
        this.blackDuckIdentifyArtifactsCron = blackDuckIdentifyArtifactsCron;
        this.blackDuckPopulateMetadataCron = blackDuckPopulateMetadataCron;
        this.blackDuckUpdateMetadataCron = blackDuckUpdateMetadataCron;
        this.blackDuckAddPendingArtifactsCron = blackDuckAddPendingArtifactsCron;
        this.repoKeys = repoKeys;
    }

    public static InspectionModuleConfig createFromProperties(final BlackDuckPropertyManager blackDuckPropertyManager, final List<String> repoKeys) {
        final boolean enabled = blackDuckPropertyManager.getBooleanProperty(InspectionModuleProperty.ENABLED);
        final String blackDuckIdentifyArtifactsCron = blackDuckPropertyManager.getProperty(InspectionModuleProperty.IDENTIFY_ARTIFACTS_CRON);
        final String blackDuckPopulateMetadataCron = blackDuckPropertyManager.getProperty(InspectionModuleProperty.POPULATE_METADATA_CRON);
        final String blackDuckUpdateMetadataCron = blackDuckPropertyManager.getProperty(InspectionModuleProperty.UPDATE_METADATA_CRON);
        final String blackDuckAddPendingArtifactsCron = blackDuckPropertyManager.getProperty(InspectionModuleProperty.ADD_PENDING_ARTIFACTS_CRON);

        return new InspectionModuleConfig(enabled, blackDuckIdentifyArtifactsCron, blackDuckPopulateMetadataCron, blackDuckUpdateMetadataCron, blackDuckAddPendingArtifactsCron, repoKeys);
    }

    public String getBlackDuckIdentifyArtifactsCron() {
        return blackDuckIdentifyArtifactsCron;
    }

    public String getBlackDuckPopulateMetadataCron() {
        return blackDuckPopulateMetadataCron;
    }

    public String getBlackDuckUpdateMetadataCron() {
        return blackDuckUpdateMetadataCron;
    }

    public String getBlackDuckAddPendingArtifactsCron() {
        return blackDuckAddPendingArtifactsCron;
    }

    public List<String> getRepoKeys() {
        return repoKeys;
    }
}
