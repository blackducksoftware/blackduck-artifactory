package com.synopsys.integration.blackduck.artifactory.api.model

data class RepositorySummary(val repoKey: String, val repoType: String, val foldersCount: Long, val filesCount: Long, val usedSpace: String, val itemsCount: Long, val packageType: String, val percentage: String)
data class StorageInfoSummary(val repositoriesSummaryList: List<RepositorySummary>)