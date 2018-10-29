package com.synopsys.integration.blackduck.artifactory.legacy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.md.Properties;
import org.artifactory.md.PropertiesInfo;
import org.junit.Test;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.ArtifactoryExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.SupportedPackageType;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalIdFactory;

public class ArtifactoryExternalIdFactoryTest {
    @Test
    public void createNugetExternalId() {
        final Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put("nuget.id", "component");
        propertiesMap.put("nuget.version", "version");

        testNameVersionExternalIdCreation(SupportedPackageType.NUGET.getArtifactoryName(), propertiesMap);
    }

    @Test
    public void createNpmExternalId() {
        final Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put("npm.name", "component");
        propertiesMap.put("npm.version", "version");

        testNameVersionExternalIdCreation(SupportedPackageType.NPM.getArtifactoryName(), propertiesMap);
    }

    @Test
    public void createPypiExternalId() {
        final Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put("pypi.name", "component");
        propertiesMap.put("pypi.version", "version");

        testNameVersionExternalIdCreation(SupportedPackageType.PYPI.getArtifactoryName(), propertiesMap);
    }

    @Test
    public void createRubygemsExternalId() {
        final Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put("gem.name", "component");
        propertiesMap.put("gem.version", "version");

        testNameVersionExternalIdCreation(SupportedPackageType.GEMS.getArtifactoryName(), propertiesMap);
    }

    @Test
    public void createMavenExternalId() {
        testMavenDependencyCreation(SupportedPackageType.MAVEN.getArtifactoryName());
    }

    @Test
    public void createGradleExternalId() {
        testMavenDependencyCreation(SupportedPackageType.GRADLE.getArtifactoryName());
    }

    private void testNameVersionExternalIdCreation(final String packageType, final Map<String, String> propertiesMap) {
        final ArtifactoryExternalIdFactory artifactoryExternalIdFactory = new ArtifactoryExternalIdFactory(new ExternalIdFactory());
        final String organization = "group";
        final String module = "component";
        final String baseRevision = "version";
        final FileLayoutInfo fileLayoutInfo = createFileLayoutInfo(organization, module, baseRevision);
        final FileLayoutInfo missingFileLayoutInfo = createFileLayoutInfo(null, null, null);

        final Properties properties = createProperties(propertiesMap);
        final Properties missingProperties = createProperties(new HashMap<>());

        Optional<ExternalId> externalId = artifactoryExternalIdFactory.createExternalId(packageType, fileLayoutInfo, properties);

        assertTrue(externalId.isPresent());

        externalId = artifactoryExternalIdFactory.createExternalId(packageType, missingFileLayoutInfo, properties);

        assertTrue(externalId.isPresent());

        externalId = artifactoryExternalIdFactory.createExternalId(packageType, fileLayoutInfo, missingProperties);

        assertTrue(externalId.isPresent());

        externalId = artifactoryExternalIdFactory.createExternalId(packageType, missingFileLayoutInfo, missingProperties);

        assertFalse(externalId.isPresent());
    }

    private void testMavenDependencyCreation(final String packageType) {
        final ArtifactoryExternalIdFactory artifactoryExternalIdFactory = new ArtifactoryExternalIdFactory(new ExternalIdFactory());
        final String organization = "group";
        final String module = "component";
        final String baseRevision = "version";
        final FileLayoutInfo fileLayoutInfo = createFileLayoutInfo(organization, module, baseRevision);
        final FileLayoutInfo missingFileLayoutInfo = createFileLayoutInfo(null, null, null);

        Optional<ExternalId> externalId = artifactoryExternalIdFactory.createExternalId(packageType, fileLayoutInfo, null);
        assertTrue(externalId.isPresent());

        externalId = artifactoryExternalIdFactory.createExternalId(packageType, missingFileLayoutInfo, null);
        assertFalse(externalId.isPresent());
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
                return null;
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
}

