/**
 * blackduck-artifactory-common
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.artifactory.legacy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.md.Properties;
import org.artifactory.md.PropertiesInfo;
import org.artifactory.repo.RepoPath;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.ArtifactoryInfoExternalIdExtractor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.BlackDuckPropertiesExternalIdExtractor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.ExternalIdService;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.ComposerExternalIdExtractor;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.ExternalIdProperties;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.SupportedPackageType;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.service.InspectionPropertyService;

// TODO: Add composer test. Might be tricky.
public class ExternalIdServiceTest {
    @Test
    public void createNugetExternalId() {
        final Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put("nuget.id", "component");
        propertiesMap.put("nuget.version", "version");

        testNameVersionExternalIdCreation(propertiesMap, SupportedPackageType.NUGET);
    }

    @Test
    public void createNpmExternalId() {
        final Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put("npm.name", "component");
        propertiesMap.put("npm.version", "version");

        testNameVersionExternalIdCreation(propertiesMap, SupportedPackageType.NPM);
    }

    @Test
    public void createPypiExternalId() {
        final Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put("pypi.name", "component");
        propertiesMap.put("pypi.version", "version");

        testNameVersionExternalIdCreation(propertiesMap, SupportedPackageType.PYPI);
    }

    @Test
    public void createRubygemsExternalId() {
        final Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put("gem.name", "component");
        propertiesMap.put("gem.version", "version");

        testNameVersionExternalIdCreation(propertiesMap, SupportedPackageType.GEMS);
    }

    @Test
    void createMultipleSeparatorExternalId() {
        final Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put("npm.name", "component/name");
        propertiesMap.put("npm.version", "version");

        testNameVersionExternalIdCreation(propertiesMap, SupportedPackageType.NPM);
    }

    @Test
    public void createMavenExternalId() {
        testMavenDependencyCreation(SupportedPackageType.MAVEN);
    }

    @Test
    public void createGradleExternalId() {
        testMavenDependencyCreation(SupportedPackageType.GRADLE);
    }

    private void testNameVersionExternalIdCreation(final Map<String, String> propertiesMap, final SupportedPackageType supportedPackageType) {
        final MockRepoPath repoPath = createValidRepoPath(propertiesMap, supportedPackageType);
        final MockRepoPath repoPathMissingFileLayout = createRepoPathMissingFileLayout(repoPath);
        final MockRepoPath repoPathMissingProperties = createRepoPathMissingProperties(repoPath);
        final MockRepoPath repoPathMissingFileLayoutAndProperties = createRepoPathMissingFileLayoutAndProperties(repoPath);

        final ArtifactoryPAPIService artifactoryPAPIService = createArtifactoryPAPIService(repoPath);

        final InspectionPropertyService inspectionPropertyService = mock(InspectionPropertyService.class);
        when(inspectionPropertyService.getExternalIdProperties(repoPath)).thenReturn(new ExternalIdProperties(null, null));
        when(inspectionPropertyService.getExternalIdProperties(repoPathMissingFileLayout)).thenReturn(new ExternalIdProperties(null, null));
        when(inspectionPropertyService.getExternalIdProperties(repoPathMissingProperties)).thenReturn(new ExternalIdProperties(null, null));
        when(inspectionPropertyService.getExternalIdProperties(repoPathMissingFileLayoutAndProperties)).thenReturn(new ExternalIdProperties(null, null));

        final BlackDuckPropertiesExternalIdExtractor blackDuckPropertiesExternalIdExtractor = new BlackDuckPropertiesExternalIdExtractor(inspectionPropertyService, new ExternalIdFactory());
        final ArtifactoryInfoExternalIdExtractor artifactoryInfoExternalIdExtractor = new ArtifactoryInfoExternalIdExtractor(artifactoryPAPIService, new ExternalIdFactory());
        final ComposerExternalIdExtractor composerExternalIdExtractor = mock(ComposerExternalIdExtractor.class);
        when(composerExternalIdExtractor.extractExternalId(supportedPackageType, repoPath))
            .then((Answer<Optional<ExternalId>>) invocation -> Optional.empty());

        final ExternalIdService externalIdService = new ExternalIdService(artifactoryPAPIService, blackDuckPropertiesExternalIdExtractor, artifactoryInfoExternalIdExtractor,
            composerExternalIdExtractor);

        Optional<ExternalId> externalId = externalIdService.extractExternalId(repoPath);

        assertTrue(externalId.isPresent());

        externalId = externalIdService.extractExternalId(repoPathMissingFileLayout);

        assertTrue(externalId.isPresent());

        externalId = externalIdService.extractExternalId(repoPathMissingProperties);

        assertTrue(externalId.isPresent());

        externalId = externalIdService.extractExternalId(repoPathMissingFileLayoutAndProperties);

        assertFalse(externalId.isPresent());
    }

    private void testMavenDependencyCreation(final SupportedPackageType supportedPackageType) {
        final MockRepoPath repoPath = createValidRepoPath(new HashMap<>(), supportedPackageType);
        final MockRepoPath repoPathMissingFileLayout = createRepoPathMissingFileLayout(repoPath);

        final InspectionPropertyService inspectionPropertyService = mock(InspectionPropertyService.class);
        when(inspectionPropertyService.hasExternalIdProperties(repoPath)).thenReturn(false);
        when(inspectionPropertyService.getExternalIdProperties(repoPath)).thenReturn(new ExternalIdProperties(null, null));
        when(inspectionPropertyService.getExternalIdProperties(repoPathMissingFileLayout)).thenReturn(new ExternalIdProperties(null, null));

        final ArtifactoryPAPIService artifactoryPAPIService = createArtifactoryPAPIService(repoPath);
        final BlackDuckPropertiesExternalIdExtractor blackDuckPropertiesExternalIdExtractor = new BlackDuckPropertiesExternalIdExtractor(inspectionPropertyService, new ExternalIdFactory());
        final ArtifactoryInfoExternalIdExtractor artifactoryInfoExternalIdExtractor = new ArtifactoryInfoExternalIdExtractor(artifactoryPAPIService, new ExternalIdFactory());
        final ComposerExternalIdExtractor composerExternalIdExtractor = mock(ComposerExternalIdExtractor.class);
        when(composerExternalIdExtractor.extractExternalId(supportedPackageType, repoPath))
            .then((Answer<Optional<ExternalId>>) invocation -> Optional.empty());

        final ExternalIdService externalIdService = new ExternalIdService(artifactoryPAPIService, blackDuckPropertiesExternalIdExtractor, artifactoryInfoExternalIdExtractor,
            composerExternalIdExtractor);

        Optional<ExternalId> externalId = externalIdService.extractExternalId(repoPath);
        assertTrue(externalId.isPresent());

        externalId = externalIdService.extractExternalId(repoPathMissingFileLayout);
        assertFalse(externalId.isPresent());
    }

    private MockRepoPath createValidRepoPath(final Map<String, String> propertiesMap, final SupportedPackageType supportedPackageType) {
        final String organization = "group";
        final String module = "component";
        final String baseRevision = "version";
        final FileLayoutInfo fileLayoutInfo = createFileLayoutInfo(organization, module, baseRevision);
        return new MockRepoPath(module, fileLayoutInfo, createProperties(propertiesMap), supportedPackageType);
    }

    private MockRepoPath createRepoPathMissingFileLayout(final MockRepoPath validMockRepoPath) {
        return new MockRepoPath("missingLayout", createFileLayoutInfo(null, null, null), validMockRepoPath.properties, validMockRepoPath.supportedPackageType);
    }

    private MockRepoPath createRepoPathMissingProperties(final MockRepoPath validMockRepoPath) {
        return new MockRepoPath("missingProperties", validMockRepoPath.fileLayoutInfo, createProperties(new HashMap<>()), validMockRepoPath.supportedPackageType);
    }

    private MockRepoPath createRepoPathMissingFileLayoutAndProperties(final MockRepoPath validMockRepoPath) {
        final MockRepoPath repoPathMissingFileLayout = createRepoPathMissingFileLayout(validMockRepoPath);
        final MockRepoPath repoPathMissingProperties = createRepoPathMissingProperties(validMockRepoPath);
        return new MockRepoPath("missingLayoutAndProperties", repoPathMissingFileLayout.fileLayoutInfo, repoPathMissingProperties.properties, validMockRepoPath.supportedPackageType);
    }

    private MockArtifactoryPAPIService createArtifactoryPAPIService(final MockRepoPath validMockRepoPath) {
        final MockArtifactoryPAPIService mockArtifactoryPAPIService = new MockArtifactoryPAPIService();
        mockArtifactoryPAPIService.addMockRepoPath(validMockRepoPath);
        mockArtifactoryPAPIService.addMockRepoPath(createRepoPathMissingFileLayout(validMockRepoPath));
        mockArtifactoryPAPIService.addMockRepoPath(createRepoPathMissingProperties(validMockRepoPath));
        mockArtifactoryPAPIService.addMockRepoPath(createRepoPathMissingFileLayoutAndProperties(validMockRepoPath));

        return mockArtifactoryPAPIService;
    }

    private Properties createProperties(final Map<String, String> propertiesMap) {
        return new Properties() {
            @Override
            public boolean putAll(final Multimap<? extends String, ? extends String> multimap) {
                return false;
            }

            @Override
            public boolean putAll(final Map<? extends String, ? extends String> map) {
                return false;
            }

            @Override
            public boolean putAll(final PropertiesInfo properties) {
                return false;
            }

            @Override
            public Multiset<String> keys() {
                return null;
            }

            @Override
            public boolean hasMandatoryProperty() {
                return false;
            }

            @Override
            public MatchResult matchQuery(final Properties queryProperties) {
                return null;
            }

            @Override
            public boolean putAll(@Nullable final String key, final Iterable<? extends String> values) {
                return false;
            }

            @Override
            public boolean putAll(@Nullable final String key, final String[] values) {
                return false;
            }

            @Nullable
            @Override
            public Set<? extends String> replaceValues(@Nonnull final String key, final Iterable<? extends String> values) {
                return null;
            }

            @Override
            public void clear() {

            }

            @Override
            public Set<String> removeAll(@Nullable final Object key) {
                return null;
            }

            @Override
            public boolean put(final String key, final String value) {
                return false;
            }

            @Override
            public int size() {
                return 0;
            }

            @Nullable
            @Override
            public Set<String> get(@Nonnull final String key) {
                final Set<String> set = new HashSet<>();
                set.add(propertiesMap.get(key));
                return set;
            }

            @Override
            public Collection<String> values() {
                return null;
            }

            @Override
            public Set<Map.Entry<String, String>> entries() {
                return null;
            }

            @Override
            public Set<String> keySet() {
                return null;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean containsKey(final String key) {
                return false;
            }

            @Nullable
            @Override
            public String getFirst(@Nonnull final String key) {
                return propertiesMap.get(key);
            }
        };
    }

    private FileLayoutInfo createFileLayoutInfo(final String organization, final String module, final String baseRevision) {
        return new FileLayoutInfo() {
            @Override
            public String getOrganization() {
                return organization;
            }

            @Override
            public String getModule() {
                return module;
            }

            @Override
            public String getBaseRevision() {
                return baseRevision;
            }

            @Override
            public String getFolderIntegrationRevision() {
                return null;
            }

            @Override
            public String getFileIntegrationRevision() {
                return null;
            }

            @Override
            public String getClassifier() {
                return null;
            }

            @Override
            public String getExt() {
                return null;
            }

            @Override
            public String getType() {
                return null;
            }

            @Override
            public Map<String, String> getCustomFields() {
                return null;
            }

            @Override
            public String getCustomField(final String tokenName) {
                return null;
            }

            @Override
            public boolean isValid() {
                return false;
            }

            @Override
            public String getPrettyModuleId() {
                return null;
            }

            @Override
            public boolean isIntegration() {
                return false;
            }
        };
    }

    protected class MockArtifactoryPAPIService extends ArtifactoryPAPIService {
        private final Map<String, MockRepoPath> mockRepoPathMap = new HashMap<>();

        public MockArtifactoryPAPIService() {
            super(null, null);
        }

        public void addMockRepoPath(final MockRepoPath mockRepoPath) {
            // Overriding the repoKey doesn't matter since they should all have the same packageType
            mockRepoPathMap.put(mockRepoPath.getRepoKey(), mockRepoPath);
            mockRepoPathMap.put(mockRepoPath.getId(), mockRepoPath);
        }

        @Override
        public Optional<String> getPackageType(final String repoKey) {
            return Optional.ofNullable(mockRepoPathMap.get(repoKey))
                       .map(mockRepoPath -> mockRepoPath.supportedPackageType.getArtifactoryName());
        }

        @Override
        public FileLayoutInfo getLayoutInfo(final RepoPath repoPath) {
            return mockRepoPathMap.get(repoPath.getId()).fileLayoutInfo;
        }

        @Override
        public Properties getProperties(final RepoPath repoPath) {
            return mockRepoPathMap.get(repoPath.getId()).properties;
        }
    }

    protected class MockRepoPath implements RepoPath {
        public final FileLayoutInfo fileLayoutInfo;
        public final Properties properties;
        public final SupportedPackageType supportedPackageType;
        private final String key = "test";
        private final String path;

        public MockRepoPath(final String path, final FileLayoutInfo fileLayoutInfo, final Properties properties, final SupportedPackageType supportedPackageType) {
            this.fileLayoutInfo = fileLayoutInfo;
            this.properties = properties;
            this.supportedPackageType = supportedPackageType;
            this.path = path;
        }

        @Nonnull
        @Override
        public String getRepoKey() {
            return key;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public String getId() {
            return key + ":" + path;
        }

        @Override
        public String toPath() {
            return key + "/" + path;
        }

        @Override
        public String getName() {
            return path;
        }

        @Nullable
        @Override
        public RepoPath getParent() {
            return null;
        }

        @Override
        public boolean isRoot() {
            return false;
        }

        @Override
        public boolean isFile() {
            return false;
        }

        @Override
        public boolean isFolder() {
            return false;
        }
    }
}

