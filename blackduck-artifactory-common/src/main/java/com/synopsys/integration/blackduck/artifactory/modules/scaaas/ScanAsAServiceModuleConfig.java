/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scaaas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationPropertyManager;
import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyGroupReport;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;

public class ScanAsAServiceModuleConfig extends ModuleConfig {
    private static final Logger logger = LoggerFactory.getLogger(ScanAsAServiceModuleConfig.class);

    private final ScanAsAServiceBlockingStrategy blockingStrategy;

    private final List<String> blockingRepos;

    private final String cutoffDateString;

    private final DateTimeManager dateTimeManager;

    private final List<String> allowedFileNamePatterns;

    private final List<String> excludedFileNamePatterns;

    private final List<String>  blockingDockerRepos;


    protected ScanAsAServiceModuleConfig(Builder builder) {
        super(ScanAsAServiceModule.class.getSimpleName(), builder.enabled);
        this.blockingStrategy = builder.blockingStrategy;
        this.blockingRepos = new ArrayList<>(builder.blockingRepos);
        this.cutoffDateString = builder.cutoffDateString;
        this.dateTimeManager = builder.dateTimeManager;
        this.allowedFileNamePatterns = builder.allowedFileNamePatterns.isEmpty() ? null : new ArrayList<>(builder.allowedFileNamePatterns);
        this.excludedFileNamePatterns = builder.excludedFileNamePatterns.isEmpty() ? null : new ArrayList<>(builder.excludedFileNamePatterns);
        this.blockingDockerRepos = builder.blockingDockerRepos.isEmpty() ? null : new ArrayList<>(builder.blockingDockerRepos);
    }

    public static ScanAsAServiceModuleConfig createFromProperties(ConfigurationPropertyManager configurationPropertyManager,
            ArtifactoryPAPIService artifactoryPAPIService,
            DateTimeManager dateTimeManager)
            throws IOException {
        Boolean enabled = configurationPropertyManager.getBooleanProperty(ScanAsAServiceModuleProperty.ENABLED);
        ScanAsAServiceModuleConfig.Builder moduleConfig = ScanAsAServiceModuleConfig.Builder.newInstance(enabled, dateTimeManager);
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
                    .map(repo -> cleanRepoName(repo, artifactoryPAPIService))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!repos.isEmpty()) {
                moduleConfig.withBlockingRepos(repos);
            }
            String cutoffDateString;
            if ((cutoffDateString = configurationPropertyManager.getProperty(ScanAsAServiceModuleProperty.CUTOFF_DATE)) != null) {
                moduleConfig.withCutoffDateString(cutoffDateString);
            }
            List<String> allowedFileNamePatterns = configurationPropertyManager.getPropertyAsList(ScanAsAServiceModuleProperty.ALLOWED_FILE_PATTERNS);
            if (!allowedFileNamePatterns.isEmpty()) {
                moduleConfig.withAllowedFileNamePatters(allowedFileNamePatterns);
            }
            List<String> excludedFileNamePatterns = configurationPropertyManager.getPropertyAsList(ScanAsAServiceModuleProperty.EXCLUDED_FILE_PATTERNS);
            if(!excludedFileNamePatterns.isEmpty()) {
                moduleConfig.withExcludedFileNamePatterns(excludedFileNamePatterns);
            }
            List<String> dockerRepos = configurationPropertyManager.getRepositoryKeysFromProperties(ScanAsAServiceModuleProperty.BLOCKING_DOCKER_REPOS, ScanAsAServiceModuleProperty.BLOCKING_DOCKER_REPOS_CSV_PATH)
                    .stream()
                    .map(repo -> ScanAsAServiceModuleConfig.cleanRepoName(repo, artifactoryPAPIService))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!dockerRepos.isEmpty()) {
                moduleConfig.withBlockingDockerRepos(dockerRepos);
            }
        }

        return moduleConfig.build();
    }

    public ScanAsAServiceBlockingStrategy getBlockingStrategy() {
        return this.blockingStrategy;
    }

    public List<String> getBlockingRepos() {
        return this.blockingRepos;
    }

    public Optional<String> getCutoffDateString() {
        return Optional.ofNullable(this.cutoffDateString);
    }

    public DateTimeManager getDateTimeManager() {
        return this.dateTimeManager;
    }

    public Optional<List<String>> getAllowedFileNamePatterns() {
        return Optional.ofNullable(this.allowedFileNamePatterns);
    }

    public Optional<List<String>> getExcludedFileNamePatterns() {
        return Optional.ofNullable(this.excludedFileNamePatterns);
    }

    public Optional<List<String>> getBlockingDockerRepos() {
        return Optional.ofNullable(this.blockingDockerRepos);
    }

    @Override
    public void validate(PropertyGroupReport propertyGroupReport, List<String> enabledModules) {
        if (isEnabled()) {
            // Only generate property report if module is enabled
            validateNotNull(propertyGroupReport, ScanAsAServiceModuleProperty.BLOCKING_STRATEGY, this.blockingStrategy);
            validateList(propertyGroupReport, ScanAsAServiceModuleProperty.BLOCKING_REPOS, this.blockingRepos,
                    String.format("No valid repositories specified. Please set the %s property with valid repositories", ScanAsAServiceModuleProperty.BLOCKING_REPOS.getKey()));
            getCutoffDateString().ifPresentOrElse(cutoffDateString ->
                validateDate(propertyGroupReport, ScanAsAServiceModuleProperty.CUTOFF_DATE, cutoffDateString, dateTimeManager),
                    () -> logger.info(String.format("No SCA-as-a-Service cutoff date supplied; Blocking Strategy %s applied to all items", getBlockingStrategy())));
        }
    }

    private static String cleanRepoName(String repo, ArtifactoryPAPIService artifactoryPAPIService) {
        String cleaned = StringUtils.deleteWhitespace(repo);
        if (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.lastIndexOf("/"));
        }
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }

        String[] split = cleaned.split("/");
        if (split.length == 0 || !artifactoryPAPIService.isValidRepository(split[0])) {
            return null;
        }
        return cleaned;
    }

    public static class Builder {
        private Boolean enabled;
        private ScanAsAServiceBlockingStrategy blockingStrategy;
        private List<String> blockingRepos = new ArrayList<>();
        private String cutoffDateString;
        private DateTimeManager dateTimeManager;
        private List<String> allowedFileNamePatterns = new ArrayList<>();
        private List<String> excludedFileNamePatterns = new ArrayList<>();
        private List<String> blockingDockerRepos = new ArrayList<>();

        private Builder(Boolean enabled, DateTimeManager dateTimeManager) {
            this.enabled = enabled;
            this.dateTimeManager = dateTimeManager;
        }

        public static Builder newInstance(Boolean enabled, DateTimeManager dateTimeManager) {
            return new Builder(enabled, dateTimeManager);
        }

        public Builder withBlockingStrategy(ScanAsAServiceBlockingStrategy blockingStrategy) {
            this.blockingStrategy = blockingStrategy;
            return this;
        }

        public Builder withBlockingRepos(List<String> repos) {
            this.blockingRepos.addAll(repos);
            return this;
        }

        public Builder withCutoffDateString(String cutoffDateString) {
            this.cutoffDateString = cutoffDateString;
            return this;
        }

        public Builder withAllowedFileNamePatters(List<String> allowedFileNamePatterns) {
            this.allowedFileNamePatterns.addAll(allowedFileNamePatterns);
            return this;
        }

        public Builder withExcludedFileNamePatterns(List<String> excludedFileNamePatterns) {
            this.excludedFileNamePatterns.addAll(excludedFileNamePatterns);
            return this;
        }

        public Builder withBlockingDockerRepos(List<String> repos) {
            this.blockingDockerRepos.addAll(repos);
            return this;
        }

        public ScanAsAServiceModuleConfig build() {
            return new ScanAsAServiceModuleConfig(this);
        }
    }
}
