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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model.ComposerVersion;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model.VersionSource;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;

class ComposerVersionSelectorTest {

    @Test
    void discoverMatchingVersion() {
        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        ComposerVersionSelector composerVersionSelector = new ComposerVersionSelector(externalIdFactory);

        SupportedPackageType supportedPackageType = SupportedPackageType.COMPOSER;
        String artifactHash = "12345";
        VersionSource commonVersionSource = new VersionSource();
        commonVersionSource.reference = artifactHash;

        List<ComposerVersion> composerVersions = new ArrayList<>();

        ComposerVersion devVersion = new ComposerVersion();
        devVersion.name = "dont-pick-me";
        devVersion.version = "some-dev-version-1.X";
        devVersion.versionSource = commonVersionSource;
        composerVersions.add(devVersion);

        ComposerVersion devVersion2 = new ComposerVersion();
        devVersion2.name = "dont-pick-me-2";
        devVersion2.version = "some-dev-version-2.X";
        devVersion2.versionSource = commonVersionSource;
        composerVersions.add(devVersion2);

        ComposerVersion actualVersion = new ComposerVersion();
        actualVersion.name = "pick-me";
        actualVersion.version = "1.0.0";
        actualVersion.versionSource = commonVersionSource;
        composerVersions.add(actualVersion);

        Optional<ExternalId> externalId = composerVersionSelector.discoverMatchingVersion(supportedPackageType, artifactHash, composerVersions);

        assertTrue(externalId.isPresent());
        assertEquals("pick-me:1.0.0", externalId.get().createExternalId());
    }
}
