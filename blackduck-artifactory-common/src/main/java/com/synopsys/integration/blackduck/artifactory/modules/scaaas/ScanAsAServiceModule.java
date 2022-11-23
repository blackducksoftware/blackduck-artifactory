/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.scaaas;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.artifactory.common.StatusHolder;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.request.Request;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.modules.Module;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.cancel.CancelDecider;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ScanAsAServiceModule implements Module {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private static final String BLACKDUCK_SCAN_AS_A_SERVICE_DOWNLOAD_PARAM = "X-BD-SCAN-AS-A-SERVICE-SCANNER-REQUEST";

    private final ScanAsAServiceModuleConfig scanAsAServiceModuleConfig;

    private final ArtifactoryPropertyService artifactoryPropertyService;

    private final ArtifactoryPAPIService artifactoryPAPIService;

    private final Collection<CancelDecider> cancelDeciders;

    public ScanAsAServiceModule(ScanAsAServiceModuleConfig scanAsAServiceModuleConfig,
            ArtifactoryPropertyService artifactoryPropertyService,
            ArtifactoryPAPIService artifactoryPAPIService,
            Collection<CancelDecider> cancelDeciders) {
        this.scanAsAServiceModuleConfig = scanAsAServiceModuleConfig;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.cancelDeciders = cancelDeciders;
    }

    @Override
    public ScanAsAServiceModuleConfig getModuleConfig() {
        return scanAsAServiceModuleConfig;
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return null;
    }

    public void handleBeforeDownloadEvent(Request request, RepoPath repoPath) {
        // If the request comes from the Scan-as-a-Service Scanner, the following Header will be present in the
        // Request; Allow the download
        logger.debug(String.format("Request headers: %s", request.getHeaders().entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(";"))));
        if (!Boolean.TRUE.equals(Boolean.valueOf(request.getHeader(BLACKDUCK_SCAN_AS_A_SERVICE_DOWNLOAD_PARAM)))) {
            cancelDeciders.forEach(cancelDecider -> cancelDecider.handleBeforeDownloadEvent(repoPath));
        } else {
            logger.info(String.format("BlackDuck Header present; Allowing download: repo: %s", repoPath));
        }
    }

    public String deleteScanAsAServicePropertiesOnRepos(Map<String, List<String>> params) {
        // A list of repos should be provided in the params. If no repos provided, send error message
        if (!params.containsKey("repos")) {
            return "Unable to execute. Please check that a list of repos is provided.";
        }
        List<String> reposToCheck = params.get("repos");
        if (reposToCheck.isEmpty()) {
            return "Unable to execute; Empty repos list";
        }
        Set<RepoPath> finalRepoPathSet = new HashSet<>();
        for (String repo : reposToCheck) {
            Set<RepoPath> repoPathSet = BlackDuckArtifactoryProperty.getScanAsAServiceProperties().stream()
                    .map(property -> artifactoryPropertyService.getItemsContainingProperties(repo, property))
                    .flatMap(List::stream)
                    .collect(Collectors.toSet());
            finalRepoPathSet.addAll(repoPathSet);
        }
        if (finalRepoPathSet.isEmpty()) {
            return "Finished; No file in repo(s) with ScanAsAService properties were found";
        }
        return deleteScanAsAServiceProperties(finalRepoPathSet);
    }

    private String deleteScanAsAServiceProperties(Set<RepoPath> artifacts) {
        StringBuilder resultMsg = new StringBuilder("The following actions were taken:").append("\n");
        for (RepoPath repo : artifacts) {
            BlackDuckArtifactoryProperty.getScanAsAServiceProperties().stream()
                    .filter(property -> artifactoryPropertyService.hasProperty(repo, property))
                    .forEach(property -> {
                        if (BlackDuckArtifactoryProperty.SCAAAS_RESULTS_URL == property) {
                            Optional<String> url = artifactoryPropertyService.getProperty(repo, property);
                            logger.debug(String.format("Attempting to remove item: [%s]", url));
                            String message = url.map(this::fromUrl)
                                    .map(artifactoryPAPIService::deleteItem)
                                    .map(StatusHolder::isError)
                                    .orElse(Boolean.TRUE).toString();
                            resultMsg.append("Removed resultsUrl: [")
                                    .append(url.orElse("NULL"))
                                    .append("]; error: [")
                                    .append(message).append("]\n");
                        }
                        artifactoryPropertyService.deleteProperty(repo, property, logger);
                    });
            resultMsg.append("Remove ScanAsAService properties from repo: [").append(repo.toPath()).append("]\n");
        }
        return resultMsg.toString();
    }

    private RepoPath fromUrl(String urlString) {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            logger.debug(String.format("Could not form URL from string: [%s]", urlString), e);
        }
        logger.debug(String.format("Using URL path [%s] and context path [%s] to construct RepoPath", (url == null ? "NULL" : url.getPath()), "/artifactory/"));
        String repoPathString = null;
        if (url != null) {
            repoPathString = url.getPath();
            if (repoPathString.startsWith("/artifactory/")) {
                repoPathString = repoPathString.replaceFirst("/artifactory/", "");
            }
        }
        return (repoPathString == null ? null : RepoPathFactory.create(repoPathString));
    }
}
