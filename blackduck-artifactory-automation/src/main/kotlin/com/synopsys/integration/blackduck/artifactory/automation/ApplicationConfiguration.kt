package com.synopsys.integration.blackduck.artifactory.automation

import com.synopsys.integration.blackduck.artifactory.automation.docker.DockerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.env.ConfigurableEnvironment
import java.io.File

@SpringBootConfiguration
class ApplicationConfiguration {
    @Autowired
    lateinit var environement: ConfigurableEnvironment

    @Bean
    fun dockerService(): DockerService {
        return DockerService()
    }

    @Bean
    fun application(): Application {
        val dockerService = dockerService()
        val configManager = ConfigManager(environement)

        return Application(
            dockerService,
            configManager.getOrDefault(ConfigProperty.ARTIFACTORY_BASEURL, "http://localhost"),
            configManager.getOrDefault(ConfigProperty.ARTIFACTORY_PORT, "8081"),
            configManager.getRequired(ConfigProperty.ARTIFACTORY_USERNAME),
            configManager.getRequired(ConfigProperty.ARTIFACTORY_PASSWORD),
            configManager.getOrDefault(ConfigProperty.ARTIFACTORY_VERSION, "latest"),
            File(configManager.getOrDefault(ConfigProperty.ARTIFACTORY_LICENSE_PATH, "")),
            configManager.getRequired(ConfigProperty.BLACKDUCK_URL),
            configManager.getRequired(ConfigProperty.BLACKDUCK_USERNAME),
            configManager.getRequired(ConfigProperty.BLACKDUCK_PASSWORD),
            configManager.getOrDefault(ConfigProperty.BLACKDUCK_TRUST_CERT, "true").toBoolean(),
            configManager.getOrDefault(ConfigProperty.MANAGE_ARTIFACTORY, "true").toBoolean(),
            File(configManager.getRequired(ConfigProperty.PLUGIN_ZIP_PATH)),
            configManager.getOrDefault(ConfigProperty.PLUGIN_LOGGING_LEVEL, "DEBUG")
        )
    }
}