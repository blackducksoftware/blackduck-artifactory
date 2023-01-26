/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model.ComposerVersion;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;

public class ComposerVersionSelector {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ExternalIdFactory externalIdFactory;

    public ComposerVersionSelector(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public Optional<ExternalId> discoverMatchingVersion(SupportedPackageType supportedPackageType, String artifactHash, Collection<ComposerVersion> composerVersions) {
        Forge forge = supportedPackageType.getForge();

        List<ExternalId> externalIdsMatchingHash = new ArrayList<>();
        for (ComposerVersion composerVersion : composerVersions) {
            String referenceHash = composerVersion.getVersionSource().getReference();
            if (referenceHash.equals(artifactHash)) {

                ExternalId foundExternalId = externalIdFactory.createNameVersionExternalId(forge, composerVersion.getName(), composerVersion.getVersion());
                externalIdsMatchingHash.add(foundExternalId);
            }
        }

        // Try to find an external id that contains version numbers since dev releases can also be in this list.
        ExternalId decidedExternalId = null;
        for (ExternalId foundExternalId : externalIdsMatchingHash) {
            boolean containsNumbers = foundExternalId.getVersion().chars()
                                          .anyMatch(Character::isDigit);
            boolean containsDev = foundExternalId.getVersion().contains("dev");
            if ((!containsDev && containsNumbers) || decidedExternalId == null) {
                decidedExternalId = foundExternalId;
            } else {
                String externalId = StringUtils.join(forge.getName(), forge.getSeparator(), foundExternalId.createExternalId());
                logger.debug("Excluding potential match for composer artifact hash {}. ExternalId: {}", artifactHash, externalId);
            }
        }

        return Optional.ofNullable(decidedExternalId);
    }
}
