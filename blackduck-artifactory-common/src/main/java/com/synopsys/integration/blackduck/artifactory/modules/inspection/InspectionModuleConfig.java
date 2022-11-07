/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationPropertyManager;
import com.synopsys.integration.blackduck.artifactory.configuration.model.PropertyGroupReport;
import com.synopsys.integration.blackduck.artifactory.modules.ModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;

public class InspectionModuleConfig extends ModuleConfig {
    private static final Logger logger = LoggerFactory.getLogger(InspectionModuleConfig.class);

    private final String inspectionCron;
    private final String reinspectCron;
    private final Boolean metadataBlockEnabled;
    private final List<String> metadataBlockRepos;
    private final Map<SupportedPackageType, List<String>> patternMap;
    private final List<String> repos;
    private final Integer retryCount;
    private final Boolean policyBlockedEnabled;
    private final List<String> policyRepos;
    private final List<PolicyRuleSeverityType> policySeverityTypes;

    // TODO: Constructing a InspectionModuleConfig config is getting complicated and would likely benefit from a builder. IARTH-443 - JM 04/2021
    public InspectionModuleConfig(
        Boolean enabled,
        String blackDuckIdentifyArtifactsCron,
        String reinspectCron,
        Boolean metadataBlockEnabled,
        List<String> metadataBlockRepos,
        Map<SupportedPackageType, List<String>> patternMap,
        List<String> repos,
        int retryCount,
        Boolean policyBlockedEnabled,
        List<String> policyRepos,
        List<PolicyRuleSeverityType> policySeverityTypes
    ) {
        super(InspectionModule.class.getSimpleName(), enabled);
        this.inspectionCron = blackDuckIdentifyArtifactsCron;
        this.reinspectCron = reinspectCron;
        this.metadataBlockEnabled = metadataBlockEnabled;
        this.metadataBlockRepos = metadataBlockRepos;
        this.patternMap = patternMap;
        this.repos = repos;
        this.retryCount = retryCount;
        this.policyBlockedEnabled = policyBlockedEnabled;
        this.policyRepos = policyRepos;
        this.policySeverityTypes = policySeverityTypes;
    }

    public static InspectionModuleConfig createFromProperties(ConfigurationPropertyManager configurationPropertyManager, ArtifactoryPAPIService artifactoryPAPIService) throws IOException {
        Boolean enabled = configurationPropertyManager.getBooleanProperty(InspectionModuleProperty.ENABLED);
        String blackDuckIdentifyArtifactsCron = configurationPropertyManager.getProperty(InspectionModuleProperty.CRON);
        String reinspectCron = configurationPropertyManager.getProperty(InspectionModuleProperty.REINSPECT_CRON);

        Map<SupportedPackageType, List<String>> patternMap = Arrays.stream(SupportedPackageType.values())
                                                                 .collect(Collectors.toMap(Function.identity(), supportedPackageType -> configurationPropertyManager.getPropertyAsList(supportedPackageType.getPatternProperty())));

        List<String> repos = configurationPropertyManager.getRepositoryKeysFromProperties(InspectionModuleProperty.REPOS, InspectionModuleProperty.REPOS_CSV_PATH).stream()
                                 .filter(artifactoryPAPIService::isValidRepository)
                                 .collect(Collectors.toList());
        Integer retryCount = configurationPropertyManager.getIntegerProperty(InspectionModuleProperty.RETRY_COUNT);

        // Metadata blocking
        Boolean metadataBlockEnabled = configurationPropertyManager.getBooleanProperty(InspectionModuleProperty.METADATA_BLOCK);
        List<String> metadataBlockRepos = configurationPropertyManager.getRepositoryKeysFromProperties(InspectionModuleProperty.METADATA_BLOCK_REPOS, InspectionModuleProperty.METADATA_BLOCK_REPOS_CSV_PATH)
                                              .stream()
                                              .filter(repoKey -> shouldIncludeValidRepository(repos, repoKey))
                                              .collect(Collectors.toList());
        String noReposMessage = "None of the repositories set to be blocked were a valid subset of the inspected repositories. Defaulting to all inspected repositories.";
        if (metadataBlockRepos.isEmpty()) {
            logger.info("Metadata Blocking: {}", noReposMessage);
            metadataBlockRepos = repos;
        }

        // Policy properties
        Boolean policyBlockedEnabled = configurationPropertyManager.getBooleanProperty(InspectionModuleProperty.POLICY_BLOCK);
        List<String> policyRepos = configurationPropertyManager.getRepositoryKeysFromProperties(InspectionModuleProperty.POLICY_REPOS, InspectionModuleProperty.POLICY_REPOS_CSV_PATH)
                                       .stream()
                                       .filter(repoKey -> shouldIncludeValidRepository(repos, repoKey))
                                       .collect(Collectors.toList());
        if (policyRepos.isEmpty()) {
            logger.info("Policy Blocking: {}", noReposMessage);
            policyRepos = repos;
        }
        List<PolicyRuleSeverityType> policySeverityTypes = configurationPropertyManager.getPropertyAsList(InspectionModuleProperty.POLICY_SEVERITY_TYPES)
                                                               .stream()
                                                               .map(PolicyRuleSeverityType::valueOf)
                                                               .collect(Collectors.toList());

        return new InspectionModuleConfig(enabled, blackDuckIdentifyArtifactsCron, reinspectCron, metadataBlockEnabled, metadataBlockRepos, patternMap, repos, retryCount, policyBlockedEnabled, policyRepos, policySeverityTypes);
    }

    private static boolean shouldIncludeValidRepository(List<String> repos, String repoKey) {
        if (!repos.contains(repoKey)) {
            logger.warn("Excluding configured repository {} from blocking because it not configured to be inspected.", repoKey);
            return false;
        }
        return true;
    }

    @Override
    public void validate(PropertyGroupReport propertyGroupReport, List<String> enabledModules) {
        validateBoolean(propertyGroupReport, InspectionModuleProperty.ENABLED, isEnabledUnverified());
        validateCronExpression(propertyGroupReport, InspectionModuleProperty.CRON, inspectionCron);
        validateCronExpression(propertyGroupReport, InspectionModuleProperty.REINSPECT_CRON, reinspectCron);
        Arrays.stream(SupportedPackageType.values())
            .forEach(packageType -> validateList(propertyGroupReport, packageType.getPatternProperty(), getPatternsForPackageType(packageType)));
        validateList(propertyGroupReport, InspectionModuleProperty.REPOS, repos,
            String.format("No valid repositories specified. Please set the %s or %s property with valid repositories", InspectionModuleProperty.REPOS.getKey(), InspectionModuleProperty.REPOS_CSV_PATH.getKey()));
        validateInteger(propertyGroupReport, InspectionModuleProperty.RETRY_COUNT, retryCount, 0, Integer.MAX_VALUE);

        validateBoolean(propertyGroupReport, InspectionModuleProperty.METADATA_BLOCK, metadataBlockEnabled);
        validateList(propertyGroupReport, InspectionModuleProperty.METADATA_BLOCK_REPOS, metadataBlockRepos, "No valid repositories are configured for metadata blocking.");

        validateBoolean(propertyGroupReport, InspectionModuleProperty.POLICY_BLOCK, policyBlockedEnabled);
        validateList(propertyGroupReport, InspectionModuleProperty.POLICY_REPOS, policyRepos, "No valid repositories are configured for policy blocking.");
        validateList(propertyGroupReport, InspectionModuleProperty.POLICY_SEVERITY_TYPES, policySeverityTypes, "No severity types were provided to block on.");
    }

    public String getInspectionCron() {
        return inspectionCron;
    }

    public String getReinspectCron() {
        return reinspectCron;
    }

    public Boolean isMetadataBlockEnabled() {
        return metadataBlockEnabled;
    }

    public List<String> getMetadataBlockRepos() {
        return metadataBlockRepos;
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

    public List<String> getRepos() {
        return repos;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public List<String> getPatternsForPackageType(String packageType) {
        return SupportedPackageType.getAsSupportedPackageType(packageType)
                   .map(patternMap::get)
                   .orElse(Collections.emptyList());
    }

    public List<String> getPatternsForPackageType(SupportedPackageType packageType) {
        return patternMap.get(packageType);
    }
}
