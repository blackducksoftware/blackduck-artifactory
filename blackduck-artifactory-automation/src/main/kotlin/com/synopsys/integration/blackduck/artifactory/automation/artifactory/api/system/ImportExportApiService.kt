package com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.system

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.gson.jsonBody
import com.synopsys.integration.blackduck.artifactory.automation.validate

class ImportExportApiService(private val fuelManager: FuelManager) {
    fun importSettings(importSettings: ImportSettings): Response {
        return fuelManager.post("/api/import/system")
                .jsonBody(importSettings)
                .response()
                .second
                .validate()
    }

    fun exportSettings(exportSettings: ExportSettings): Response {
        return fuelManager.post("/api/export/system")
                .jsonBody(exportSettings)
                .response()
                .second
                .validate()
    }
}

data class ImportSettings(
        val importPath: String = "/import/path",
        val includeMetadata: Boolean = true,
        val verbose: Boolean = false,
        val failOnError: Boolean = true,
        val failIfEmpty: Boolean = true
)

data class ExportSettings(
        val exportPath: String = "/export/path",
        val includeMetadata: Boolean = true,
        val createArchive: Boolean = false,
        val bypassFiltering: Boolean = false,
        val verbose: Boolean = false,
        val failOnError: Boolean = true,
        val failIfEmpty: Boolean = true,
        val m2: Boolean = false,
        val incremental: Boolean = false,
        val excludeContent: Boolean = false
)