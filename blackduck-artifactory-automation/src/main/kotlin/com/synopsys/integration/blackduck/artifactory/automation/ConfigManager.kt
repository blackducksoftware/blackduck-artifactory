package com.synopsys.integration.blackduck.artifactory.automation

import org.springframework.core.env.ConfigurableEnvironment

class ConfigManager(private val configurableEnvironment: ConfigurableEnvironment) {
    fun get(property: ConfigProperty): String? {
        return configurableEnvironment.getProperty(property.propertyKey)?.ifBlank { null }
    }

    fun getRequired(property: ConfigProperty, throwable: Throwable = IllegalArgumentException("Failed to find valid configuration for property '${property.propertyKey}'")): String {
        return get(property) ?: throw throwable
    }
}

enum class ConfigProperty {
    ARTIFACTORY_BASEURL,
    ARTIFACTORY_PORT,
    ARTIFACTORY_USERNAME,
    ARTIFACTORY_PASSWORD,
    ARTIFACTORY_VERSION,
    ARTIFACTORY_LICENSE_PATH,
    BLACKDUCK_URL,
    BLACKDUCK_USERNAME,
    BLACKDUCK_PASSWORD,
    BLACKDUCK_TRUST_CERT,
    MANAGE_ARTIFACTORY,
    PLUGIN_ZIP_PATH,
    PLUGIN_LOGGING_LEVEL,
    CONFIG_IMPORT_DIRECTORY;

    val propertyKey = "automation.${this.name.toLowerCase().replace("_", ".")}"
}