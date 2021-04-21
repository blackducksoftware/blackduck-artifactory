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
    private static final String TEST_REPO_KEY = "test-repo";
    private static final String PACKAGE_NAME = "console";
    private static final String PACKAGE_HASH = "12345";
    private static final String PACKAGE_EXTENSION = "zip";
    private static final String JSON_FILE = PACKAGE_NAME + ".json";
    private static final String JSON_FILE_PATH = String.format(".composer/p/symfony/%s", JSON_FILE);

    @Test
    void findJsonFiles() {
        PluginRepoPathFactory repoPathFactory = new PluginRepoPathFactory(false);
        RepoPath repoPath = repoPathFactory.create(TEST_REPO_KEY, String.format("%s-%s.%s", PACKAGE_NAME, PACKAGE_HASH, PACKAGE_EXTENSION));
        RepoPath jsonRepoPath = repoPathFactory.create(TEST_REPO_KEY, JSON_FILE_PATH);
        List<RepoPath> jsonRepoPaths = Collections.singletonList(jsonRepoPath);

        ArtifactoryPAPIService artifactoryPAPIService = Mockito.mock(ArtifactoryPAPIService.class);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ComposerJsonService composerJsonService = new ComposerJsonService(artifactoryPAPIService, gson);

        Mockito.when(artifactoryPAPIService.itemsByName(JSON_FILE, TEST_REPO_KEY))
            .thenReturn(jsonRepoPaths);

        ComposerJsonResult composerJsonResult = composerJsonService.findJsonFiles(repoPath);

        FileNamePieces fileNamePieces = composerJsonResult.getFileNamePieces();
        assertEquals(PACKAGE_NAME, fileNamePieces.getComponentName());
        assertEquals(PACKAGE_HASH, fileNamePieces.getHash());
        assertEquals(PACKAGE_EXTENSION, fileNamePieces.getExtension());
        assertEquals(jsonRepoPaths, composerJsonResult.getRepoPaths());
    }

    @Test
    void parseJson() {
        PluginRepoPathFactory repoPathFactory = new PluginRepoPathFactory(false);
        RepoPath jsonRepoPath = repoPathFactory.create(TEST_REPO_KEY, JSON_FILE_PATH);

        ArtifactoryPAPIService artifactoryPAPIService = Mockito.mock(ArtifactoryPAPIService.class);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ComposerJsonService composerJsonService = new ComposerJsonService(artifactoryPAPIService, gson);

        Mockito.when(artifactoryPAPIService.getArtifactContent(jsonRepoPath))
            .thenReturn(new ResourceStreamHandle() {
                final InputStream inputStream = TestUtil.INSTANCE.getResourceAsStream("/" + JSON_FILE);

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
