package com.synopsys.integration.blackduck.artifactory.modules.scaaas;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationPropertyManager;

class ScanAsAServiceModuleConfigTest {

    @Mock
    private ConfigurationPropertyManager manager;

    @Mock
    private ArtifactoryPAPIService service;

    @Mock
    private DateTimeManager timeManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void createFromProperties() throws IOException {
        String repo = "repo-local-1";
        String blockStrategy = ScanAsAServiceBlockingStrategy.BLOCK_OFF.name();
        Mockito.when(manager.getBooleanProperty(ScanAsAServiceModuleProperty.ENABLED)).thenReturn(true);
        Mockito.when(manager.getProperty(ScanAsAServiceModuleProperty.BLOCKING_STRATEGY)).thenReturn(blockStrategy);
        Mockito.when(manager.getRepositoryKeysFromProperties(ScanAsAServiceModuleProperty.BLOCKING_REPOS, ScanAsAServiceModuleProperty.BLOCKING_REPOS_CSV_PATH))
                .thenReturn(List.of(repo));
        Mockito.when(service.isValidRepository(Mockito.anyString())).thenReturn(true);

        ScanAsAServiceModuleConfig config = ScanAsAServiceModuleConfig.createFromProperties(manager, service, timeManager);

        Assertions.assertEquals(blockStrategy, config.getBlockingStrategy().name());
        Assertions.assertEquals(List.of(repo), config.getBlockingRepos());
    }

    @Test
    void createFromPropertiesRepoBranch() throws IOException {
        List<String> repos = List.of("repo-local-1/branch", "repo-2");
        String blockStrategy = ScanAsAServiceBlockingStrategy.BLOCK_OFF.name();
        Mockito.when(manager.getBooleanProperty(ScanAsAServiceModuleProperty.ENABLED)).thenReturn(true);
        Mockito.when(manager.getProperty(ScanAsAServiceModuleProperty.BLOCKING_STRATEGY)).thenReturn(blockStrategy);
        Mockito.when(manager.getRepositoryKeysFromProperties(ScanAsAServiceModuleProperty.BLOCKING_REPOS, ScanAsAServiceModuleProperty.BLOCKING_REPOS_CSV_PATH))
                .thenReturn(repos);
        Mockito.when(service.isValidRepository(Mockito.anyString())).thenReturn(true);

        ScanAsAServiceModuleConfig config = ScanAsAServiceModuleConfig.createFromProperties(manager, service, timeManager);

        Assertions.assertEquals(repos, config.getBlockingRepos());
    }

    @Test
    void createFromPropertiesRepoBranchIllegal() throws IOException {
        String repo = "repo-local-1";
        String branch = "branch";
        String blockStrategy = ScanAsAServiceBlockingStrategy.BLOCK_OFF.name();
        Mockito.when(manager.getBooleanProperty(ScanAsAServiceModuleProperty.ENABLED)).thenReturn(true);
        Mockito.when(manager.getProperty(ScanAsAServiceModuleProperty.BLOCKING_STRATEGY)).thenReturn(blockStrategy);
        Mockito.when(manager.getRepositoryKeysFromProperties(ScanAsAServiceModuleProperty.BLOCKING_REPOS, ScanAsAServiceModuleProperty.BLOCKING_REPOS_CSV_PATH))
                .thenReturn(List.of("/" + repo + "/" + branch + "/"));
        Mockito.when(service.isValidRepository(Mockito.anyString())).thenReturn(true);

        ScanAsAServiceModuleConfig config = ScanAsAServiceModuleConfig.createFromProperties(manager, service, timeManager);

        Mockito.verify(service).isValidRepository(repo);
        Assertions.assertEquals(List.of(repo + "/" + branch), config.getBlockingRepos());
    }
}