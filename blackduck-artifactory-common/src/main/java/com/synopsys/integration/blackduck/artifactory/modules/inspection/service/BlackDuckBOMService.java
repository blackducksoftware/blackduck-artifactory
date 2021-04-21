/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.service;

import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.blackduck.api.generated.response.ComponentsView;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionComponentView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModule;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.exception.FailedInspectionException;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.Artifact;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.ComponentViewWrapper;
import com.synopsys.integration.blackduck.exception.BlackDuckApiException;
import com.synopsys.integration.blackduck.exception.BlackDuckIntegrationException;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.blackduck.service.dataservice.ComponentService;
import com.synopsys.integration.blackduck.service.dataservice.ProjectBomService;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.rest.HttpUrl;
import com.synopsys.integration.rest.exception.IntegrationRestException;

public class BlackDuckBOMService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(BlackDuckBOMService.class));

    private final ProjectBomService projectBomService;
    private final ComponentService componentService;
    private final BlackDuckApiClient blackDuckApiClient;

    public BlackDuckBOMService(ProjectBomService projectBomService, ComponentService componentService, BlackDuckApiClient blackDuckApiClient) {
        this.projectBomService = projectBomService;
        this.componentService = componentService;
        this.blackDuckApiClient = blackDuckApiClient;
    }

    public ComponentViewWrapper addArtifactToProjectVersion(Artifact artifact, ProjectVersionView projectVersionView) throws FailedInspectionException {
        RepoPath repoPath = artifact.getRepoPath();
        ComponentViewWrapper componentViewWrapper;

        Optional<ExternalId> externalIdOptional = artifact.getExternalId();
        if (externalIdOptional.isPresent()) {
            ExternalId externalId = externalIdOptional.get();
            try {
                componentViewWrapper = addComponentToProjectVersion(repoPath, externalId, projectVersionView);
            } catch (BlackDuckIntegrationException e) {
                logger.warn(String.format("Cannot find component match for artifact at %s.", repoPath.toPath()));
                throw new FailedInspectionException(repoPath, "Failed to find component match.");
            } catch (FailedInspectionException e) {
                // If we don't catch and throw, the inspectionStatus message will be too vague.
                throw e;
            } catch (Exception e) {
                logger.warn(String.format("The Black Duck %s could not successfully inspect '%s'.", InspectionModule.class.getSimpleName(), repoPath.toPath()));
                logger.debug(e.getMessage(), e);
                throw new FailedInspectionException(repoPath, "See logs for details.");
            }
        } else {
            throw new FailedInspectionException(repoPath, "Artifactory failed to provide sufficient information to identify the artifact.");
        }

        return componentViewWrapper;
    }

    // Mostly copied from the ProjectBomService in blackduck-common. Made tweaks so the component that was attempting to be added is always returned.
    private ComponentViewWrapper addComponentToProjectVersion(RepoPath repoPath, ExternalId componentExternalId, ProjectVersionView projectVersionView) throws IntegrationException {
        ComponentViewWrapper componentViewWrapper;
        String externalIdString = String.format("%s:%s", componentExternalId.getForge().toString(), componentExternalId.createExternalId());
        ComponentsView componentsView = componentService.getFirstOrEmptyResult(componentExternalId)
                                            .orElseThrow(() -> new FailedInspectionException(repoPath, String.format("Failed to find component match for component '%s'.", externalIdString)));

        ComponentVersionView componentVersionView = componentService.getComponentVersionView(componentsView)
                                                        .orElseThrow(() -> new FailedInspectionException(repoPath, String.format("Failed to find component version for component '%s'.", externalIdString)));

        try {
            projectBomService.addComponentToProjectVersion(componentVersionView, projectVersionView);
        } catch (IntegrationRestException e) {
            handleIntegrationRestException(repoPath, e);
        } catch (BlackDuckApiException e) {
            handleIntegrationRestException(repoPath, e.getOriginalIntegrationRestException());
        }
        componentViewWrapper = getComponentViewWrapper(componentVersionView, projectVersionView);

        return componentViewWrapper;
    }

    private void handleIntegrationRestException(RepoPath repoPath, IntegrationRestException exception)
        throws FailedInspectionException {
        int statusCode = exception.getHttpStatusCode();

        if (statusCode == 412) {
            logger.info(String.format("Unable to add manual BOM component because it already exists '%s'.", repoPath.toPath()));
            logger.debug("Will attempt to grab policy status from Black Duck directly.");
        } else if (statusCode == 401) {
            logger.warn(String.format("The Black Duck %s could not successfully inspect %s because plugin is unauthorized (%d). Ensure the plugin is configured with the correct credentials", InspectionModule.class.getSimpleName(),
                repoPath.toPath(), statusCode));
            throw new FailedInspectionException(repoPath, String.format("Unauthorized (%s)", statusCode));
        } else {
            logger.warn(String.format("The Black Duck %s could not successfully inspect %s because of a %d status code", InspectionModule.class.getSimpleName(), repoPath.toPath(), statusCode));
            logger.debug(String.format(exception.getMessage(), repoPath), exception);
            throw new FailedInspectionException(repoPath, String.format("Status code: %s", statusCode));
        }
    }

    public ComponentViewWrapper getComponentViewWrapper(HttpUrl componentVersionUrl, ProjectVersionView projectVersionView) throws IntegrationException {
        ComponentVersionView componentVersionView = blackDuckApiClient.getResponse(componentVersionUrl, ComponentVersionView.class);
        return getComponentViewWrapper(componentVersionView, projectVersionView);
    }

    private ComponentViewWrapper getComponentViewWrapper(ComponentVersionView componentVersionView, ProjectVersionView projectVersionView) throws IntegrationException {
        HttpUrl projectVersionViewHref = projectVersionView.getHref();
        HttpUrl componentVersionViewHref = componentVersionView.getHref();

        // This is bad practice but...
        // The link to a VersionBomComponentView cannot be obtained without searching the BOM or manually constructing the link. So for performance in Black Duck, we manually construct the link
        final String apiComponentsLinkPrefix = "/api/components/";
        String componentHref = componentVersionViewHref.string();
        int apiComponentsStart = componentHref.indexOf(apiComponentsLinkPrefix) + apiComponentsLinkPrefix.length();
        int endingIndex = componentHref.length();
        final String originsLinkPrefix = "/origins/";
        if (componentHref.contains(originsLinkPrefix)) {
            endingIndex = componentHref.indexOf(originsLinkPrefix);
        }
        HttpUrl versionBomComponentUrl = new HttpUrl(String.format("%s/components/%s", projectVersionViewHref, componentHref.substring(apiComponentsStart, endingIndex)));
        projectVersionViewHref.appendRelativeUrl("components/" + componentHref.substring(apiComponentsStart, endingIndex));
        ProjectVersionComponentView versionBomComponentView = blackDuckApiClient.getResponse(versionBomComponentUrl, ProjectVersionComponentView.class);

        return new ComponentViewWrapper(versionBomComponentView, componentVersionView);
    }

}
