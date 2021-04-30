/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package modules.inspection.notifications;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.artifactory.repo.RepoPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.artifactory.PluginRepoPathFactory;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.RepositoryProjectNameLookup;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;
import com.synopsys.integration.util.NameVersion;

class RepositoryProjectNameLookupTest {

    @Test
    void fromProperties() {
        PluginRepoPathFactory repoPathFactory = new PluginRepoPathFactory(false);
        RepoPath validRepoPath = repoPathFactory.create("validRepoPath");
        RepoPath unrelatedRepoPath = repoPathFactory.create("unrelatedRepoPath");
        List<RepoPath> repoPaths = Arrays.asList(validRepoPath, unrelatedRepoPath);

        InspectionPropertyService inspectionPropertyService = Mockito.mock(InspectionPropertyService.class);
        Mockito.when(inspectionPropertyService.getProjectNameVersion(validRepoPath))
            .thenReturn(Optional.of(new NameVersion("validRepoPath", "version")));
        Mockito.when(inspectionPropertyService.getProjectNameVersion(unrelatedRepoPath))
            .thenReturn(Optional.empty());

        RepositoryProjectNameLookup repositoryProjectNameLookup = RepositoryProjectNameLookup.fromProperties(inspectionPropertyService, repoPaths);

        Optional<RepoPath> repoKeyPath = repositoryProjectNameLookup.getRepoKeyPath(new NameVersion("validRepoPath", "version"));
        Assertions.assertTrue(repoKeyPath.isPresent());
        Assertions.assertEquals(validRepoPath, repoKeyPath.get());

        Optional<RepoPath> unrelatedRepoKeyPath = repositoryProjectNameLookup.getRepoKeyPath("something", "else");
        Assertions.assertFalse(unrelatedRepoKeyPath.isPresent());
    }

}
