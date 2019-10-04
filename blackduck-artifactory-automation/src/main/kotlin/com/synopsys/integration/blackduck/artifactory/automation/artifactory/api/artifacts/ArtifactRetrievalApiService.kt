package com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.artifacts

import com.github.kittinunf.fuel.core.FuelManager
import com.synopsys.integration.blackduck.artifactory.automation.validate

class ArtifactRetrievalApiService(private val fuelManager: FuelManager) {
    fun retrieveArtifact(repositoryKey: String, path: String) {
        fuelManager.get("$repositoryKey/${path.trimStart('/')}")
                .response()
                .second.validate()
    }
}