package com.synopsys.integration.blackduck.artifactory.automation.plugin

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Response
import com.synopsys.integration.blackduck.artifactory.automation.validate

/**
 * An API service for all the endpoints created by the plugin.
 */
class BlackDuckPluginApiService(private val fuelManager: FuelManager) {
    private val pluginsApiPrefix = "/api/plugins/execute"

    fun reloadPlugin(): Response {
        return fuelManager.post("$pluginsApiPrefix/blackDuckReload")
                .response()
                .second
                .validate()
    }

    fun blackDuckInitializeRepositories(): Response {
        return fuelManager.post("$pluginsApiPrefix/blackDuckInitializeRepositories")
                .response()
                .second
                .validate()
    }

    fun setModuleState(module: String, enabled: Boolean): Response {
        val request = fuelManager.post("$pluginsApiPrefix/blackDuckSetModuleState")
        request.parameters = listOf(
                Pair("params", "$module=$enabled")
        )
        return request
                .response()
                .second
                .validate()
    }

    fun deleteInspectionProperties(): Response {
        return fuelManager.post("$pluginsApiPrefix/blackDuckDeleteInspectionProperties")
                .response()
                .second
                .validate()
    }
}