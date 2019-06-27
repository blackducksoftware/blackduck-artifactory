package com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.artifacts

import com.github.kittinunf.fuel.core.FuelManager
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.Repository
import com.synopsys.integration.blackduck.artifactory.automation.validate

class ArtifactRetrievalApiService(private val fuelManager: FuelManager) {
    fun retrieveArtifact(repository: Repository, path: String) {
        fuelManager.get("${repository.key}/${path.trimStart('/')}")
            .response()
            .second.validate()
    }
}