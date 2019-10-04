package com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.artifacts

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.gson.responseObject
import com.google.gson.annotations.SerializedName
import com.synopsys.integration.blackduck.artifactory.automation.validateDontThrow

class PropertiesApiService(private val fuelManager: FuelManager) {
    fun getProperties(repoPath: String): ItemProperties? {
        val responseResult = fuelManager.get("/api/storage/$repoPath?properties")
                .responseObject<ItemProperties>()

        return if (responseResult.second.validateDontThrow()) {
            responseResult.third.get()
        } else {
            null
        }
    }
}

data class ItemProperties(
        @SerializedName("uri")
        val uri: String,
        @SerializedName("properties")
        val properties: Map<String, List<String>>
)