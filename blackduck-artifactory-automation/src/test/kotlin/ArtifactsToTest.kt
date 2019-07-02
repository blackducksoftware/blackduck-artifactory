import com.synopsys.integration.bdio.model.Forge
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory
import com.synopsys.integration.blackduck.artifactory.automation.TestablePackage

object ArtifactsToTest {
    private val externalIdFactory = ExternalIdFactory()

    val PYPI_PACKAGES = listOf(
        TestablePackage(
            "f7/d2/e07d3ebb2bd7af696440ce7e754c59dd546ffe1bbe732c8ab68b9c834e61/cycler-0.10.0-py2.py3-none-any.whl",
            externalIdFactory.createNameVersionExternalId(Forge.PYPI, "Cycler", "0.10.0")
        )
    )
}