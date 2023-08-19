/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scan;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationPropertyManager;
import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyGroupReport;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleProperty;

public class ScanModuleConfig extends ModuleConfig {
    private static final Logger logger = LoggerFactory.getLogger(ScanModuleConfig.class);

    private final String cron;
    @Nullable
    private final String binariesDirectoryPath;
    private final String artifactCutoffDate;
    private final Boolean dryRun;
    private final List<String> namePatterns;
    private final Integer memory;
    private final Boolean repoPathCodelocation;
    private final List<String> repos;
    private final Boolean codelocationIncludeHostname;
    private final Boolean metadataBlockEnabled;
    private final List<String> metadataBlockRepos;
    private final Boolean policyBlockedEnabled;
    private final List<String> policyRepos;
    private final List<PolicyRuleSeverityType> policySeverityTypes;
    private final String overrideProject;
    private final String overrideVersion;

    private final DateTimeManager dateTimeManager;

    // TODO: Constructing a ScanModule config is getting complicated and would likely benefit from a builder. IARTH-443 - JM 04/2021
    public ScanModuleConfig(
        Boolean enabled,
        String cron,
        @Nullable String binariesDirectoryPath,
        String artifactCutoffDate,
        Boolean dryRun,
        List<String> namePatterns,
        Integer memory,
        Boolean repoPathCodelocation,
        List<String> repos,
        Boolean codelocationIncludeHostname,
        Boolean metadataBlockEnabled,
        List<String> metadataBlockRepos,
        Boolean policyBlockedEnabled,
        List<String> policyRepos,
        List<PolicyRuleSeverityType> policySeverityTypes,
        DateTimeManager dateTimeManager,
        String overrideProject,
        String overrideVersion
    ) {
        super(ScanModule.class.getSimpleName(), enabled);
        this.cron = cron;
        this.binariesDirectoryPath = binariesDirectoryPath;
        this.artifactCutoffDate = artifactCutoffDate;
        this.dryRun = dryRun;
        this.namePatterns = namePatterns;
        this.memory = memory;
        this.repoPathCodelocation = repoPathCodelocation;
        this.repos = repos;
        this.codelocationIncludeHostname = codelocationIncludeHostname;
        this.metadataBlockEnabled = metadataBlockEnabled;
        this.metadataBlockRepos = metadataBlockRepos;
        this.policyBlockedEnabled = policyBlockedEnabled;
        this.policyRepos = policyRepos;
        this.policySeverityTypes = policySeverityTypes;
        this.dateTimeManager = dateTimeManager;
        this.overrideProject = overrideProject;
        this.overrideVersion = overrideVersion;
    }

    public static ScanModuleConfig createFromProperties(
        ConfigurationPropertyManager configurationPropertyManager,
        ArtifactoryPAPIService artifactoryPAPIService,
        DateTimeManager dateTimeManager
    ) throws IOException {
        String cron = configurationPropertyManager.getProperty(ScanModuleProperty.CRON);
        String binariesDirectoryPath = configurationPropertyManager.getProperty(ScanModuleProperty.BINARIES_DIRECTORY_PATH);
        String artifactCutoffDate = configurationPropertyManager.getProperty(ScanModuleProperty.CUTOFF_DATE);
        Boolean dryRun = configurationPropertyManager.getBooleanProperty(ScanModuleProperty.DRY_RUN);
        Boolean enabled = configurationPropertyManager.getBooleanProperty(ScanModuleProperty.ENABLED);
        List<String> namePatterns = configurationPropertyManager.getPropertyAsList(ScanModuleProperty.NAME_PATTERNS);
        Integer memory = configurationPropertyManager.getIntegerProperty(ScanModuleProperty.MEMORY);
        Boolean repoPathCodelocation = configurationPropertyManager.getBooleanProperty(ScanModuleProperty.REPO_PATH_CODELOCATION);
        List<String> repos = configurationPropertyManager.getRepositoryKeysFromProperties(ScanModuleProperty.REPOS, ScanModuleProperty.REPOS_CSV_PATH).stream()
                                 .filter(artifactoryPAPIService::isValidRepository)
                                 .collect(Collectors.toList());
        Boolean codelocationIncludeHostname = configurationPropertyManager.getBooleanProperty(ScanModuleProperty.CODELOCATION_INCLUDE_HOSTNAME);

        // Metadata blocking
        Boolean metadataBlockEnabled = configurationPropertyManager.getBooleanProperty(ScanModuleProperty.METADATA_BLOCK);
        List<String> metadataBlockRepos = configurationPropertyManager.getRepositoryKeysFromProperties(ScanModuleProperty.METADATA_BLOCK_REPOS, ScanModuleProperty.METADATA_BLOCK_REPOS_CSV_PATH)
                                              .stream()
                                              .filter(repoKey -> shouldIncludeValidRepository(repos, repoKey))
                                              .collect(Collectors.toList());
        String noReposMessage = "None of the repositories set to be blocked were a valid subset of the scanned repositories. Defaulting to all scanned repositories.";
        if (metadataBlockRepos.isEmpty()) {
            logger.info("Metadata Blocking: {}", noReposMessage);
            metadataBlockRepos = repos;
        }

        // Policy properties
        Boolean policyBlockedEnabled = configurationPropertyManager.getBooleanProperty(ScanModuleProperty.POLICY_BLOCK);
        List<String> policyRepos = configurationPropertyManager.getRepositoryKeysFromProperties(ScanModuleProperty.POLICY_REPOS, ScanModuleProperty.POLICY_REPOS_CSV_PATH)
                                       .stream()
                                       .filter(repoKey -> shouldIncludeValidRepository(repos, repoKey))
                                       .collect(Collectors.toList());
        if (policyRepos.isEmpty()) {
            logger.info("Policy Blocking: {}", noReposMessage);
            policyRepos = repos;
        }
        List<PolicyRuleSeverityType> policySeverityTypes = configurationPropertyManager.getPropertyAsList(ScanModuleProperty.POLICY_SEVERITY_TYPES)
                                                               .stream()
                                                               .map(PolicyRuleSeverityType::valueOf)
                                                               .collect(Collectors.toList());

        String overrideProject = configurationPropertyManager.getProperty(ScanModuleProperty.OVERRIDE_PROJECT);
        if (overrideProject != null && !overrideProject.isEmpty()) {
            logger.info(String.format("Found project override property for scans. Project override: %s", overrideProject));
        }

        String overrideVersion = configurationPropertyManager.getProperty(ScanModuleProperty.OVERRIDE_VERSION);
        if (overrideVersion != null && !overrideVersion.isEmpty()) {
            logger.info(String.format("Found version override property for scans. Version override: %s", overrideVersion));
        }

        return new ScanModuleConfig(
            enabled,
            cron,
            binariesDirectoryPath,
            artifactCutoffDate,
            dryRun,
            namePatterns,
            memory,
            repoPathCodelocation,
            repos,
            codelocationIncludeHostname,
            metadataBlockEnabled,
            metadataBlockRepos,
            policyBlockedEnabled,
            policyRepos,
            policySeverityTypes,
            dateTimeManager,
            overrideProject,
            overrideVersion
        );
    }

    private static boolean shouldIncludeValidRepository(List<String> repos, String repoKey) {
        if (!repos.contains(repoKey)) {
            logger.warn("Excluding configured repository {} from blocking because it not configured to be scanned.", repoKey);
            return false;
        }
        return true;
    }

    @Override
    public void validate(PropertyGroupReport propertyGroupReport, List<String> enabeledModules) {
        validateCronExpression(propertyGroupReport, ScanModuleProperty.CRON, cron);
        validateDate(propertyGroupReport, ScanModuleProperty.CUTOFF_DATE, artifactCutoffDate, dateTimeManager);
        validateBoolean(propertyGroupReport, ScanModuleProperty.DRY_RUN, dryRun);
        validateBoolean(propertyGroupReport, ScanModuleProperty.ENABLED, isEnabledUnverified());
        validateInteger(propertyGroupReport, ScanModuleProperty.MEMORY, memory);
        validateList(propertyGroupReport, ScanModuleProperty.NAME_PATTERNS, namePatterns, "Please provide name patterns to match against");
        validateBoolean(propertyGroupReport, ScanModuleProperty.REPO_PATH_CODELOCATION, repoPathCodelocation);
        validateList(propertyGroupReport, ScanModuleProperty.REPOS, repos,
            String.format("No valid repositories specified. Please set the %s or %s property with valid repositories", ScanModuleProperty.REPOS.getKey(), ScanModuleProperty.REPOS_CSV_PATH.getKey()));

        validateBoolean(propertyGroupReport, ScanModuleProperty.METADATA_BLOCK, metadataBlockEnabled);
        validateList(propertyGroupReport, InspectionModuleProperty.METADATA_BLOCK_REPOS, metadataBlockRepos, "No valid repositories are configured for metadata blocking.");

        validateBoolean(propertyGroupReport, ScanModuleProperty.POLICY_BLOCK, policyBlockedEnabled);
        validateList(propertyGroupReport, ScanModuleProperty.POLICY_REPOS, policyRepos, "No valid repositories are configured for policy blocking.");
        validateList(propertyGroupReport, ScanModuleProperty.POLICY_SEVERITY_TYPES, policySeverityTypes, "No severity types were provided to block on.");
    }

    private static boolean includeValidRepository(ArtifactoryPAPIService artifactoryPAPIService, List<String> repos, String repoKey) {
        if (!repos.contains(repoKey)) {
            logger.warn("Excluding configured repository {} from blocking because it not configured to be inspected.", repoKey);
            return false;
        } else if (!artifactoryPAPIService.isValidRepository(repoKey)) {
            logger.warn("Excluding configured repository {} from blocking because it is not a valid repository.", repoKey);
            return false;
        }
        return true;
    }

    public String getCron() {
        return cron;
    }

    public String getBinariesDirectoryPath() {
        return binariesDirectoryPath;
    }

    public String getArtifactCutoffDate() {
        return artifactCutoffDate;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public List<String> getNamePatterns() {
        return namePatterns;
    }

    public Integer getMemory() {
        return memory;
    }

    public Boolean getPolicyBlockedEnabled() {
        return policyBlockedEnabled;
    }

    public List<String> getPolicyRepos() {
        return policyRepos;
    }

    public List<PolicyRuleSeverityType> getPolicySeverityTypes() {
        return policySeverityTypes;
    }

    public Boolean getRepoPathCodelocation() {
        return repoPathCodelocation;
    }

    public List<String> getRepos() {
        return repos;
    }

    public Boolean getCodelocationIncludeHostname() {
        return codelocationIncludeHostname;
    }

    public Boolean isMetadataBlockEnabled() {
        return metadataBlockEnabled;
    }

    public List<String> getMetadataBlockRepos() {
        return metadataBlockRepos;
    }

    public String getOverrideProject() {
        return overrideProject;
    }

    public String getOverrideVersion() {
        return overrideVersion;
    }
}
