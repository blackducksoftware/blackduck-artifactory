package com.synopsys.integration.blackduck.artifactory.modules.inspection

import com.synopsys.integration.blackduck.artifactory.modules.inspection.model.InspectionModuleConfig
import com.synopsys.integration.builder.BuilderStatus
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InspectionModuleConfigTest {
    private InspectionModuleConfig validInspectionModuleConfig;
    private InspectionModuleConfig invalidInspectionModuleConfig;

    @BeforeEach
    void init() {
        validInspectionModuleConfig = new InspectionModuleConfig(
            true,
            "0 0/1 * 1/1 * ?", metadataBlockEnabled,
            ["*.gem"],
            ["*.jar"],
            ["*.jar"],
            ["*.whl", "*.tar.gz", "*.zip", "*.egg"],
            ["*.nupkg"],
            ["*.tgz"]
            ,
            ["repo1", "repo2"],
            5,
        )

        invalidInspectionModuleConfig = new InspectionModuleConfig(
            null,
            null, metadataBlockEnabled,
            null,
            null,
            null,
            null,
            null,
            null
            ,
            null,
            -1
        )
    }

    @Test
    void validate() {
        final BuilderStatus validBuilderStatus = new BuilderStatus()
        validInspectionModuleConfig.validate(validBuilderStatus)
        Assert.assertEquals(0, validBuilderStatus.getErrorMessages().size())

        final BuilderStatus invalidBuilderStatus = new BuilderStatus()
        invalidInspectionModuleConfig.validate(invalidBuilderStatus)
        Assert.assertEquals(12, invalidBuilderStatus.getErrorMessages().size())
    }

    @Test
    void getIdentifyArtifactsCron() {
        Assert.assertEquals("0 0/1 * 1/1 * ?", validInspectionModuleConfig.getInspectionCron())
        Assert.assertNull(invalidInspectionModuleConfig.getInspectionCron())
    }

    @Test
    void getPopulateMetadataCron() {
        Assert.assertEquals("0 0/1 * 1/1 * ?", validInspectionModuleConfig.getInspectionCron())
        Assert.assertNull(invalidInspectionModuleConfig.getInspectionCron())
    }

    @Test
    void getUpdateMetadataCron() {
        Assert.assertEquals("0 0/1 * 1/1 * ?", validInspectionModuleConfig.getInspectionCron())
        Assert.assertNull(invalidInspectionModuleConfig.getInspectionCron())
    }

    @Test
    void getRepos() {
        Assert.assertArrayEquals(["repo1", "repo2"].toArray(), validInspectionModuleConfig.getRepos().toArray())
        Assert.assertNull(invalidInspectionModuleConfig.getRepos())
    }

    @Test
    void getPatternsRubygems() {
        Assert.assertArrayEquals(["*.gem"].toArray(), validInspectionModuleConfig.getPatternsRubygems().toArray())
        Assert.assertNull(invalidInspectionModuleConfig.getRepos())
    }

    @Test
    void getPatternsMaven() {
        Assert.assertArrayEquals(["*.jar"].toArray(), validInspectionModuleConfig.getPatternsMaven().toArray())
        Assert.assertNull(invalidInspectionModuleConfig.getRepos())
    }

    @Test
    void getPatternsGradle() {
        Assert.assertArrayEquals(["*.jar"].toArray(), validInspectionModuleConfig.getPatternsGradle().toArray())
        Assert.assertNull(invalidInspectionModuleConfig.getRepos())
    }

    @Test
    void getPatternsPypi() {
        Assert.assertArrayEquals(["*.whl", "*.tar.gz", "*.zip", "*.egg"].toArray(), validInspectionModuleConfig.getPatternsPypi().toArray())
        Assert.assertNull(invalidInspectionModuleConfig.getRepos())
    }

    @Test
    void getPatternsNuget() {
        Assert.assertArrayEquals(["*.nupkg"].toArray(), validInspectionModuleConfig.getPatternsNuget().toArray())
        Assert.assertNull(invalidInspectionModuleConfig.getRepos())
    }

    @Test
    void getPatternsNpm() {
        Assert.assertArrayEquals(["*.tgz"].toArray(), validInspectionModuleConfig.getPatternsNpm().toArray())
        Assert.assertNull(invalidInspectionModuleConfig.getRepos())
    }
}