package com.synopsys.integration.blackduck.artifactory.modules.inspection.external.id;

import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;

public abstract class BaseExternalIdFactory implements ExternalIdExtractor {
    protected Optional<ExternalId> createNameVersionExternalId(final ExternalIdFactory externalIdFactory, final Forge forge, @Nullable final String name, @Nullable final String version) {
        ExternalId externalId = null;
        if (StringUtils.isNoneBlank(name, version)) {
            externalId = externalIdFactory.createNameVersionExternalId(forge, name, version);
        }
        return Optional.ofNullable(externalId);
    }
}
