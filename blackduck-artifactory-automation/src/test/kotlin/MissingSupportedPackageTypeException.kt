import com.synopsys.integration.blackduck.artifactory.automation.artifactory.PackageType
import com.synopsys.integration.exception.IntegrationException

class MissingSupportedPackageTypeException(private val packageType: PackageType, override val message: String = "Automation tests should support the ${packageType.packageType} because the plugin supports it.") : IntegrationException()