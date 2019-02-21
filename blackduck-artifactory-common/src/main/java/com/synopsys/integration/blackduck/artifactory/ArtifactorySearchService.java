package com.synopsys.integration.blackduck.artifactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.modules.inspection.PackageTypePatternManager;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.SupportedPackageType;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ArtifactorySearchService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ArtifactoryPAPIService artifactoryPAPIService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final PackageTypePatternManager packageTypePatternManager;

    public ArtifactorySearchService(final ArtifactoryPAPIService artifactoryPAPIService, final ArtifactoryPropertyService artifactoryPropertyService,
        final PackageTypePatternManager packageTypePatternManager) {
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.packageTypePatternManager = packageTypePatternManager;
    }

    public Set<RepoPath> searchForArtifactsWithSupportedPackageType(final List<String> repoKeys) {
        return repoKeys.stream()
                   .map(this::searchForArtifactsWithSupportedPackageType)
                   .flatMap(Collection::stream)
                   .collect(Collectors.toSet());
    }

    public Set<RepoPath> searchForArtifactsWithSupportedPackageType(final String repoKey) {
        final Set<RepoPath> identifiableArtifacts = new HashSet<>();
        final Optional<SupportedPackageType> packageType = artifactoryPAPIService.getPackageType(repoKey)
                                                               .map(SupportedPackageType::getAsSupportedPackageType)
                                                               .filter(Optional::isPresent)
                                                               .map(Optional::get);

        if (packageType.isPresent()) {
            final List<String> patterns = packageTypePatternManager.getPatterns(packageType.get());
            for (final String pattern : patterns) {
                final List<RepoPath> repoPaths = artifactoryPAPIService.searchForArtifactsByPattern(repoKey, pattern);
                identifiableArtifacts.addAll(repoPaths);
            }
        } else {
            logger.info(String.format("The package type of repo [%s] is not supported. Supported package types are %s", repoKey, Arrays.toString(SupportedPackageType.values())));
        }

        return identifiableArtifacts;
    }
}
