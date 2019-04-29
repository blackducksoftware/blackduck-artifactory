package com.synopsys.integration.blackduck.artifactory.modules.inspection.external.id.composer;

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
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.ArtifactSearchService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.external.id.BaseExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.external.id.composer.model.Version;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ComposerExternalIdFactory extends BaseExternalIdFactory {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ArtifactSearchService artifactSearchService;
    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ExternalIdFactory externalIdFactory;
    private final Gson gson;

    public ComposerExternalIdFactory(final ArtifactSearchService artifactSearchService, final ArtifactoryPAPIService artifactoryPAPIService, final ExternalIdFactory externalIdFactory, final Gson gson) {
        this.artifactSearchService = artifactSearchService;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.externalIdFactory = externalIdFactory;
        this.gson = gson;
    }

    @Override
    public Optional<ExternalId> extractExternalId(final RepoPath repoPath) {
        final String repoKey = repoPath.getRepoKey();
        final String packageType = artifactoryPAPIService.getPackageType(repoKey).orElse(null);
        final Optional<SupportedPackageType> supportedPackageType = SupportedPackageType.getAsSupportedPackageType(packageType);

        if (!supportedPackageType.isPresent()) {
            logger.warn(String.format("Package type (%s) not supported", packageType));
            return Optional.empty();
        }

        final FileNamePieces fileNamePieces = extractFileNamePieces(repoPath);
        final String jsonFileName = fileNamePieces.getComponentName().toLowerCase() + ".json";
        final List<RepoPath> jsonFileRepoPaths = artifactSearchService.findArtifactByName(jsonFileName, repoPath.getRepoKey());

        ExternalId externalId = null;
        for (final RepoPath jsonFileRepoPath : jsonFileRepoPaths) {
            final List<Version> versions = parseJson(jsonFileRepoPath);
            final List<ExternalId> externalIds = new ArrayList<>();

            for (final Version version : versions) {
                final String referenceHash = version.getVersionSource().getReference();
                final String artifactHash = fileNamePieces.getHash();
                if (referenceHash.equals(artifactHash)) {
                    final Forge forge = supportedPackageType.get().getForge();
                    final ExternalId foundExternalId = createNameVersionExternalId(externalIdFactory, forge, version.getName(), version.getVersion()).orElse(null);
                    externalIds.add(foundExternalId);
                }
            }

            // Try to find an external id that contains version numbers since dev releases can also be in this list.
            for (final ExternalId foundExternalId : externalIds) {
                final String[] numbers = IntStream.rangeClosed(0, 9)
                                             .boxed()
                                             .map(String::valueOf)
                                             .toArray(String[]::new);
                if (StringUtils.containsAny(foundExternalId.version, numbers) || externalId == null) {
                    externalId = foundExternalId;
                }

            }

            if (externalId != null) {
                break;
            }
        }

        return Optional.ofNullable(externalId);
    }

    private List<Version> parseJson(final RepoPath repoPath) {
        try (final ResourceStreamHandle resourceStreamHandle = artifactoryPAPIService.getArtifactContent(repoPath)) {
            final InputStream inputStream = resourceStreamHandle.getInputStream();
            final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            final JsonElement root = new JsonParser().parse(inputStreamReader);
            final Set<Map.Entry<String, JsonElement>> rootEntries = root.getAsJsonObject().get("packages").getAsJsonObject().entrySet();
            final List<Version> versions = new ArrayList<>();
            for (final Map.Entry<String, JsonElement> rootEntry : rootEntries) {
                final String groupModuleName = rootEntry.getKey().toLowerCase();
                if (groupModuleName.contains(groupModuleName)) {
                    final Set<Map.Entry<String, JsonElement>> versionJsonElements = rootEntry.getValue().getAsJsonObject().entrySet();

                    for (final Map.Entry<String, JsonElement> versionJsonElement : versionJsonElements) {
                        final Version version = gson.fromJson(versionJsonElement.getValue(), Version.class);
                        versions.add(version);
                    }
                } else {
                    // TODO: Logger
                }
            }

            return versions;
        }
    }

    private FileNamePieces extractFileNamePieces(final RepoPath repoPath) {
        final String fullFileName = repoPath.getName();
        final String[] zipExtensionPieces = fullFileName.split("\\.");
        final String extension = zipExtensionPieces[zipExtensionPieces.length - 1];
        final int extensionStart = fullFileName.indexOf(extension) - 1;
        final String extensionRemovedFileName = fullFileName.substring(0, extensionStart);

        final String[] componentNameHashPieces = extensionRemovedFileName.split("-");
        final String hash = componentNameHashPieces[componentNameHashPieces.length - 1];
        final int hashStart = extensionRemovedFileName.indexOf(hash) - 1;
        final String componentName = extensionRemovedFileName.substring(0, hashStart).toLowerCase();

        return new FileNamePieces(componentName, hash, extension);
    }

    private class FileNamePieces {
        private final String componentName;
        private final String hash;
        private final String extension;

        private FileNamePieces(final String componentName, final String hash, final String extension) {
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
