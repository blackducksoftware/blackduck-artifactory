/*
 * Copyright (C) 2022 Synopsys Inc.
 * http://www.synopsys.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Synopsys ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Synopsys.
 */

package com.synopsys.integration.blackduck.artifactory.modules.scaas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationPropertyManager;
import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyGroupReport;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;

public class ScanAsAServiceModuleConfig extends ModuleConfig {
    private static final Logger logger = LoggerFactory.getLogger(ScanAsAServiceModuleConfig.class);

    private final ScanAsAServiceBlockingStrategy blockingStrategy;

    private final List<String> repos;


    protected ScanAsAServiceModuleConfig(Builder builder) {
        super(ScanAsAServiceModule.class.getSimpleName(), builder.enabled);
        this.blockingStrategy = builder.blockingStrategy;
        this.repos = new ArrayList<>(builder.repos);
    }

    public static ScanAsAServiceModuleConfig createFromProperties(ConfigurationPropertyManager configurationPropertyManager, ArtifactoryPAPIService artifactoryPAPIService)
            throws IOException {
        Boolean enabled = configurationPropertyManager.getBooleanProperty(ScanAsAServiceModuleProperty.ENABLED);
        ScanAsAServiceModuleConfig.Builder moduleConfig = ScanAsAServiceModuleConfig.Builder.newInstance(enabled);
        logger.debug("blackDuckPlugin: ScanAsAService: property: {}; value: {}", ScanAsAServiceModuleProperty.ENABLED.getKey(), enabled);
        if (Boolean.TRUE.equals(enabled)) {
            // Only parse properties if the module is enabled
            ScanAsAServiceBlockingStrategy blockingStrategy = null;
            String blockingStrategyAsString = null;
            try {
                blockingStrategyAsString = configurationPropertyManager.getProperty(ScanAsAServiceModuleProperty.BLOCKING_STRATEGY);
                blockingStrategy = ScanAsAServiceBlockingStrategy.valueOf(blockingStrategyAsString);
            } catch (IllegalArgumentException e) {
                logger.warn("blackDuckPlugin: ScanAsAService: Unknown value provided for {}: {}", ScanAsAServiceModuleProperty.BLOCKING_STRATEGY.getKey(),
                        blockingStrategyAsString);
            }
            moduleConfig.withBlockingStrategy(blockingStrategy);
            List<String> repos = configurationPropertyManager.getRepositoryKeysFromProperties(ScanAsAServiceModuleProperty.BLOCKING_REPOS, ScanAsAServiceModuleProperty.BLOCKING_REPOS_CSV_PATH)
                    .stream()
                    .filter(artifactoryPAPIService::isValidRepository)
                    .collect(Collectors.toList());
            if (!repos.isEmpty()) {
                moduleConfig.withRepos(repos);
            }
        }

        return moduleConfig.build();
    }

    @Override
    public void validate(PropertyGroupReport propertyGroupReport) {
        if (isEnabled()) {
            // Only generate property report if module is enabled
            validateNotNull(propertyGroupReport, ScanAsAServiceModuleProperty.BLOCKING_STRATEGY, this.blockingStrategy);
            validateList(propertyGroupReport, ScanAsAServiceModuleProperty.BLOCKING_REPOS, this.repos,
                    String.format("No valid repositories specified. Please set the %s property with valid repositories", ScanAsAServiceModuleProperty.BLOCKING_REPOS.getKey()));
        }
    }

    public static class Builder {
        private Boolean enabled;
        private ScanAsAServiceBlockingStrategy blockingStrategy;
        private List<String> repos = new ArrayList<>();

        private Builder(Boolean enabled) {
            this.enabled = enabled;
        }

        public static Builder newInstance(Boolean enabled) {
            return new Builder(enabled);
        }

        public Builder withBlockingStrategy(ScanAsAServiceBlockingStrategy blockingStrategy) {
            this.blockingStrategy = blockingStrategy;
            return this;
        }

        public Builder withRepos(List<String> repos) {
            this.repos.addAll(repos);
            return this;
        }

        public ScanAsAServiceModuleConfig build() {
            return new ScanAsAServiceModuleConfig(this);
        }
    }
}
