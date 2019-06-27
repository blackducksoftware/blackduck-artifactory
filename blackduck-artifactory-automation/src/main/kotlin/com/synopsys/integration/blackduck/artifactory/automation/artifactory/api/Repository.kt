package com.synopsys.integration.blackduck.artifactory.automation.artifactory.api

import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.repositories.RepositoryConfiguration
import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.repositories.RepositoryType

data class Repository(
    val key: String,
    val configuration: RepositoryConfiguration,
    val type: RepositoryType
)
