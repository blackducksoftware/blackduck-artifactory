package com.synopsys.integration.blackduck.artifactory.automation

import com.synopsys.integration.bdio.model.externalid.ExternalId

data class TestablePackage(val artifactoryFileName: String, val externalId: ExternalId)