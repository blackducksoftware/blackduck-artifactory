package com.synopsys.integration.blackduck.artifactory.modules.scaaas;

import java.io.IOException;
import java.util.Collections;
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
        String dockerRepo = "docker-repo-local-1";
        String blockStrategy = ScanAsAServiceBlockingStrategy.BLOCK_OFF.name();
        Mockito.when(manager.getBooleanProperty(ScanAsAServiceModuleProperty.ENABLED)).thenReturn(true);
        Mockito.when(manager.getProperty(ScanAsAServiceModuleProperty.BLOCKING_STRATEGY)).thenReturn(blockStrategy);
        Mockito.when(manager.getRepositoryKeysFromProperties(ScanAsAServiceModuleProperty.BLOCKING_REPOS, ScanAsAServiceModuleProperty.BLOCKING_REPOS_CSV_PATH))
                .thenReturn(List.of(repo));
        Mockito.when(manager.getRepositoryKeysFromProperties(ScanAsAServiceModuleProperty.BLOCKING_DOCKER_REPOS, ScanAsAServiceModuleProperty.BLOCKING_DOCKER_REPOS_CSV_PATH))
                .thenReturn(List.of(dockerRepo));
        Mockito.when(service.isValidRepository(Mockito.anyString())).thenReturn(true);

        ScanAsAServiceModuleConfig config = ScanAsAServiceModuleConfig.createFromProperties(manager, service, timeManager);

        Assertions.assertEquals(blockStrategy, config.getBlockingStrategy().name());
        Assertions.assertEquals(List.of(repo), config.getBlockingRepos());
        Assertions.assertEquals(List.of(dockerRepo), config.getBlockingDockerRepos().orElse(null));
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

    @Test
    void createFromPropertiesNoDockerRepos() throws IOException {
        String repo = "repo-local-1";
        String blockingStrategy = ScanAsAServiceBlockingStrategy.BLOCK_OFF.name();
        Mockito.when(manager.getBooleanProperty(ScanAsAServiceModuleProperty.ENABLED)).thenReturn(true);
        Mockito.when(manager.getProperty(ScanAsAServiceModuleProperty.BLOCKING_STRATEGY)).thenReturn(blockingStrategy);
        Mockito.when(manager.getRepositoryKeysFromProperties(ScanAsAServiceModuleProperty.BLOCKING_REPOS, ScanAsAServiceModuleProperty.BLOCKING_REPOS_CSV_PATH))
                .thenReturn(List.of(repo));
        Mockito.when(manager.getRepositoryKeysFromProperties(ScanAsAServiceModuleProperty.BLOCKING_DOCKER_REPOS, ScanAsAServiceModuleProperty.BLOCKING_DOCKER_REPOS_CSV_PATH))
                .thenReturn(Collections.emptyList());

        ScanAsAServiceModuleConfig config = ScanAsAServiceModuleConfig.createFromProperties(manager, service, timeManager);

        Assertions.assertFalse(config.getBlockingDockerRepos().isPresent());
    }
}