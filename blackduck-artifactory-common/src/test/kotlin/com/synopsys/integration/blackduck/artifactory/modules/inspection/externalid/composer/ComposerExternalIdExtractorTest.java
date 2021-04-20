/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model.ComposerJsonResult;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model.ComposerVersion;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model.FileNamePieces;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model.VersionSource;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;

class ComposerExternalIdExtractorTest {
    private static final String PACKAGE_NAME = "console";
    private static final String PACKAGE_VERSION = "1.2.3";
    private static final String PACKAGE_HASH = "12345";
    private static final String PACKAGE_EXTENSION = "zip";

    @Test
    void extractExternalId() {
        ComposerVersionSelector composerVersionSelector = Mockito.mock(ComposerVersionSelector.class);
        ComposerJsonService composerJsonService = Mockito.mock(ComposerJsonService.class);
        ComposerExternalIdExtractor composerExternalIdExtractor = new ComposerExternalIdExtractor(composerVersionSelector, composerJsonService);

        PluginRepoPathFactory repoPathFactory = new PluginRepoPathFactory(false);
        RepoPath repoPath = repoPathFactory.create("test-repo", String.format("%s-%s.%s", PACKAGE_NAME, PACKAGE_HASH, PACKAGE_EXTENSION));

        Mockito.when(composerJsonService.findJsonFiles(repoPath)).thenReturn(
            new ComposerJsonResult(
                new FileNamePieces(PACKAGE_NAME, PACKAGE_HASH, PACKAGE_EXTENSION),
                Collections.singletonList(repoPath)
            )
        );

        ComposerVersion composerVersion = new ComposerVersion();
        composerVersion.name = PACKAGE_NAME;
        composerVersion.version = PACKAGE_VERSION;
        VersionSource versionSource = new VersionSource();
        versionSource.reference = PACKAGE_HASH;
        composerVersion.versionSource = versionSource;
        Mockito.when(composerJsonService.parseJson(repoPath)).thenReturn(
            Collections.singletonList(composerVersion)
        );

        ExternalId externalId = new ExternalIdFactory().createNameVersionExternalId(Forge.PACKAGIST, PACKAGE_NAME, PACKAGE_VERSION);
        Mockito.when(composerVersionSelector.discoverMatchingVersion(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(
            Optional.of(externalId)
        );

        Optional<ExternalId> actualExternalId = composerExternalIdExtractor.extractExternalId(SupportedPackageType.COMPOSER, repoPath);

        assertTrue(actualExternalId.isPresent());
        assertEquals(externalId.createExternalId(), actualExternalId.get().createExternalId());
    }
}
