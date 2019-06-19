package com.synopsys.integration.blackduck.artifactory.automation.artifactory.api

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.isClientError
import com.github.kittinunf.fuel.core.isServerError
import com.synopsys.integration.exception.IntegrationException

// TODO: Do away with this class.
abstract class ArtifactoryApiService(
    internal val fuelManager: FuelManager
)

fun Response.validate(): Response {
    if (this.isClientError || this.isServerError || this.statusCode < 0) {
        throw IntegrationException("Status Code: ${this.statusCode}, Content: ${this}")
    }
    return this
}