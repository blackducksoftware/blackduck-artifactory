/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.resource.ResourceStreamHandle;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.ArtifactSearchService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model.ComposerVersion;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;

public class ComposerExternalIdExtractor {
    private final ArtifactSearchService artifactSearchService;
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ExternalIdFactory externalIdFactory;
    private final Gson gson;

    public ComposerExternalIdExtractor(ArtifactSearchService artifactSearchService, ArtifactoryPAPIService artifactoryPAPIService, ExternalIdFactory externalIdFactory, Gson gson) {
        this.artifactSearchService = artifactSearchService;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.externalIdFactory = externalIdFactory;
        this.gson = gson;
    }

    public Optional<ExternalId> extractExternalId(SupportedPackageType supportedPackageType, RepoPath repoPath) {
        FileNamePieces fileNamePieces = extractFileNamePieces(repoPath);
        String jsonFileName = fileNamePieces.getComponentName().toLowerCase() + ".json";
        List<RepoPath> jsonFileRepoPaths = artifactSearchService.findArtifactByName(jsonFileName, repoPath.getRepoKey());

        ExternalId externalId = null;
        for (RepoPath jsonFileRepoPath : jsonFileRepoPaths) {
            List<ComposerVersion> composerVersions = parseJson(jsonFileRepoPath);
            List<ExternalId> externalIds = new ArrayList<>();

            for (ComposerVersion composerVersion : composerVersions) {
                String referenceHash = composerVersion.getVersionSource().getReference();
                String artifactHash = fileNamePieces.getHash();
                if (referenceHash.equals(artifactHash)) {
                    Forge forge = supportedPackageType.getForge();
                    ExternalId foundExternalId = externalIdFactory.createNameVersionExternalId(forge, composerVersion.getName(), composerVersion.getVersion());
                    externalIds.add(foundExternalId);
                }
            }

            // Try to find an external id that contains version numbers since dev releases can also be in this list.
            for (ExternalId foundExternalId : externalIds) {
                String[] numbers = IntStream.rangeClosed(0, 9)
                                       .boxed()
                                       .map(String::valueOf)
                                       .toArray(String[]::new);
                if (StringUtils.containsAny(foundExternalId.getVersion(), numbers) || externalId == null) {
                    externalId = foundExternalId;
                }

            }

            if (externalId != null) {
                break;
            }
        }

        return Optional.ofNullable(externalId);
    }

    private List<ComposerVersion> parseJson(RepoPath repoPath) {
        try (ResourceStreamHandle resourceStreamHandle = artifactoryPAPIService.getArtifactContent(repoPath)) {
            InputStream inputStream = resourceStreamHandle.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            JsonElement root = JsonParser.parseReader(inputStreamReader);
            Set<Map.Entry<String, JsonElement>> rootEntries = root.getAsJsonObject().get("packages").getAsJsonObject().entrySet();
            List<ComposerVersion> composerVersions = new ArrayList<>();
            for (Map.Entry<String, JsonElement> rootEntry : rootEntries) {
                Set<Map.Entry<String, JsonElement>> versionJsonElements = rootEntry.getValue().getAsJsonObject().entrySet();

                for (Map.Entry<String, JsonElement> versionJsonElement : versionJsonElements) {
                    ComposerVersion composerVersion = gson.fromJson(versionJsonElement.getValue(), ComposerVersion.class);
                    composerVersions.add(composerVersion);
                }
            }

            return composerVersions;
        }
    }

    private FileNamePieces extractFileNamePieces(RepoPath repoPath) {
        String fullFileName = repoPath.getName();
        String[] zipExtensionPieces = fullFileName.split("\\.");
        String extension = zipExtensionPieces[zipExtensionPieces.length - 1];
        int extensionStart = fullFileName.indexOf(extension) - 1;
        String extensionRemovedFileName = fullFileName.substring(0, extensionStart);

        String[] componentNameHashPieces = extensionRemovedFileName.split("-");
        String hash = componentNameHashPieces[componentNameHashPieces.length - 1];
        int hashStart = extensionRemovedFileName.indexOf(hash) - 1;
        String componentName = extensionRemovedFileName.substring(0, hashStart).toLowerCase();

        return new FileNamePieces(componentName, hash, extension);
    }

    private class FileNamePieces {
        private final String componentName;
        private final String hash;
        private final String extension;

        private FileNamePieces(String componentName, String hash, String extension) {
            this.componentName = componentName;
            this.hash = hash;
            this.extension = extension;
        }

        public String getComponentName() {
            return componentName;
        }

        public String getHash() {
            return hash;
        }

        public String getExtension() {
            return extension;
        }
    }
}
