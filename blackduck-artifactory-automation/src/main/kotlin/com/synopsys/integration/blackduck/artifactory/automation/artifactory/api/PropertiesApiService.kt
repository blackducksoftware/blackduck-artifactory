package com.synopsys.integration.blackduck.artifactory.automation.artifactory.api

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.gson.responseObject
import com.google.gson.annotations.SerializedName

class PropertiesApiService(fuelManager: FuelManager) : ArtifactoryApiService(fuelManager) {
    fun getProperties(repoPath: String): ItemProperties {
        val responseResult = fuelManager.get("/api/storage/$repoPath?properties")
            .responseObject<ItemProperties>()
        responseResult.second.validate()
        return responseResult.third.get()
    }
}

data class ItemProperties(
    @SerializedName("uri")
    val uri: String,
    @SerializedName("properties")
    val properties: Map<String, List<String>>
)