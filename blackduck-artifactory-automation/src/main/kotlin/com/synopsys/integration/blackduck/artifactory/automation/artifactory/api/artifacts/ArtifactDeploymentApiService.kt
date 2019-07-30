package com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.artifacts

import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.Repository
import com.synopsys.integration.blackduck.artifactory.automation.validate
import java.io.File

class ArtifactDeploymentApiService(private val fuelManager: FuelManager) {
    fun deployArtifact(repository: Repository, file: File, path: String = file.path) {
        fuelManager.upload("${repository.key}/$path/${file.name}", Method.PUT)
            .add(FileDataPart(file))
            .response()
            .second.validate()
    }
}