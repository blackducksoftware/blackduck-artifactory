package com.synopsys.integration.blackduck.artifactory.automation.plugin

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Response
import com.synopsys.integration.blackduck.artifactory.automation.validate

/**
 * An API service for all the endpoints created by the plugin.
 */
class BlackDuckPluginApiService(private val fuelManager: FuelManager) {
    fun reloadPlugin(): Response {
        return fuelManager.post("/api/plugins/execute/blackDuckReload")
            .response()
            .second
            .validate()
    }

    fun blackDuckInitializeRepositories(): Response {
        return fuelManager.post("/api/plugins/execute/blackDuckInitializeRepositories")
            .response()
            .second
            .validate()
    }
}