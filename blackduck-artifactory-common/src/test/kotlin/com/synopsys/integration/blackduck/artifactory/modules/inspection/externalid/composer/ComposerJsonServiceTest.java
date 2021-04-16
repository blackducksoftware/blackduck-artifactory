/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.artifactory.repo.RepoPath;
import org.artifactory.resource.ResourceStreamHandle;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.TestUtil;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model.ComposerJsonResult;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model.ComposerVersion;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model.FileNamePieces;

import lombok.SneakyThrows;

class ComposerJsonServiceTest {

    @Test
    void findJsonFiles() {
        PluginRepoPathFactory repoPathFactory = new PluginRepoPathFactory(false);
        RepoPath repoPath = repoPathFactory.create("test-repo", "console-1ba4560dbbb9fcf5ae28b61f71f49c678086cf23.zip");
        RepoPath jsonRepoPath = repoPathFactory.create("test-repo", ".composer/p/symfony/console.json");
        List<RepoPath> jsonRepoPaths = Collections.singletonList(jsonRepoPath);

        ArtifactoryPAPIService artifactoryPAPIService = Mockito.mock(ArtifactoryPAPIService.class);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ComposerJsonService composerJsonService = new ComposerJsonService(artifactoryPAPIService, gson);

        Mockito.when(artifactoryPAPIService.itemsByName("console.json", "test-repo"))
            .thenReturn(jsonRepoPaths);

        ComposerJsonResult composerJsonResult = composerJsonService.findJsonFiles(repoPath);

        FileNamePieces fileNamePieces = composerJsonResult.getFileNamePieces();
        assertEquals("console", fileNamePieces.getComponentName());
        assertEquals("1ba4560dbbb9fcf5ae28b61f71f49c678086cf23", fileNamePieces.getHash());
        assertEquals("zip", fileNamePieces.getExtension());
        assertEquals(jsonRepoPaths, composerJsonResult.getRepoPaths());
    }

    @Test
    void parseJson() {
        PluginRepoPathFactory repoPathFactory = new PluginRepoPathFactory(false);
        RepoPath jsonRepoPath = repoPathFactory.create("test-repo", ".composer/p/symfony/console.json");

        ArtifactoryPAPIService artifactoryPAPIService = Mockito.mock(ArtifactoryPAPIService.class);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ComposerJsonService composerJsonService = new ComposerJsonService(artifactoryPAPIService, gson);

        Mockito.when(artifactoryPAPIService.getArtifactContent(jsonRepoPath))
            .thenReturn(new ResourceStreamHandle() {
                final InputStream inputStream = TestUtil.INSTANCE.getResourceAsStream("/console.json");

                @Override
                public InputStream getInputStream() {
                    return inputStream;
                }

                @SneakyThrows
                @Override
                public long getSize() {
                    return inputStream.available();
                }

                @SneakyThrows
                @Override
                public void close() {
                    inputStream.close();
                }
            });

        List<ComposerVersion> composerVersions = composerJsonService.parseJson(jsonRepoPath);

        assertEquals(3, composerVersions.size());
    }
}
