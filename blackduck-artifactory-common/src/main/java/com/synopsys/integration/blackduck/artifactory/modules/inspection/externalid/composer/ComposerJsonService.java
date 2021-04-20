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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.artifactory.repo.RepoPath;
import org.artifactory.resource.ResourceStreamHandle;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model.ComposerJsonResult;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model.ComposerVersion;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model.FileNamePieces;

public class ComposerJsonService {
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final Gson gson;

    public ComposerJsonService(ArtifactoryPAPIService artifactoryPAPIService, Gson gson) {
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.gson = gson;
    }

    public ComposerJsonResult findJsonFiles(RepoPath repoPath) {
        FileNamePieces fileNamePieces = extractFileNamePieces(repoPath);
        String jsonFileName = fileNamePieces.getComponentName().toLowerCase() + ".json";
        List<RepoPath> foundArtifacts = artifactoryPAPIService.itemsByName(jsonFileName, repoPath.getRepoKey());
        return new ComposerJsonResult(fileNamePieces, foundArtifacts);
    }

    public List<ComposerVersion> parseJson(RepoPath repoPath) {
        try (ResourceStreamHandle resourceStreamHandle = artifactoryPAPIService.getArtifactContent(repoPath)) {
            InputStream inputStream = resourceStreamHandle.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            JsonElement root = JsonParser.parseReader(inputStreamReader);

            return streamChildren(root.getAsJsonObject().get("packages"))
                       .flatMap(this::streamChildren)
                       .map(version -> gson.fromJson(version, ComposerVersion.class))
                       .collect(Collectors.toList());
        }
    }

    private Stream<JsonElement> streamChildren(JsonElement rootEntries) {
        JsonObject rootObject = rootEntries.getAsJsonObject();
        return rootObject.entrySet()
                   .stream()
                   .map(Map.Entry::getValue);
    }

    // TODO: Create a generic filename pieces extractor. - JakeMathews 04/2021
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
}
