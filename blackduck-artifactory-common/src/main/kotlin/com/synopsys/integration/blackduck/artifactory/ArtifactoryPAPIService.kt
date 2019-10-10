/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2019 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.artifactory

import com.google.common.collect.SetMultimap
import com.synopsys.integration.log.Slf4jIntLogger
import org.apache.commons.lang3.StringUtils
import org.artifactory.fs.FileLayoutInfo
import org.artifactory.fs.ItemInfo
import org.artifactory.md.Properties
import org.artifactory.repo.RepoPath
import org.artifactory.repo.Repositories
import org.artifactory.repo.RepositoryConfiguration
import org.artifactory.resource.ResourceStreamHandle
import org.artifactory.search.Searches
import org.slf4j.LoggerFactory
import java.util.*

open class ArtifactoryPAPIService(private val pluginRepoPathFactory: PluginRepoPathFactory, private val repositories: Repositories, private val searches: Searches) {
    private val logger = Slf4jIntLogger(LoggerFactory.getLogger(this.javaClass))

    open fun getPackageType(repoKey: String): Optional<String> {
        return getRepositoryConfiguration(repoKey).map { it.packageType }
    }

    fun getArtifactCount(repoKeys: List<String>): Long? {
        return repoKeys
                .map { pluginRepoPathFactory.create(it) }
                .map { repositories.getArtifactsCount(it) }
                .sum()
    }

    fun isValidRepository(repoKey: String): Boolean {
        if (StringUtils.isBlank(repoKey)) {
            logger.warn("A blank repo key is invalid")
            return false
        }

        val repoPath = pluginRepoPathFactory.create(repoKey)
        val isValid = repositories.exists(repoPath) && getRepositoryConfiguration(repoKey).isPresent

        if (!isValid) {
            logger.warn(String.format("Repository '%s' was not found or is not a valid repository.", repoKey))
        }

        return isValid
    }
    
    fun searchForArtifactsByPatterns(repoKey: String, patterns: List<String>): List<RepoPath> {
        val repoPaths = ArrayList<RepoPath>()

        for (pattern in patterns) {
            val foundRepoPaths = searches.artifactsByName(pattern, repoKey)
            if (foundRepoPaths.isNotEmpty()) {
                repoPaths.addAll(foundRepoPaths)
                logger.debug(String.format("Found %d artifacts matching pattern [%s]", foundRepoPaths.size, pattern))
            } else {
                logger.debug(String.format("No artifacts found that match the pattern pattern [%s]", pattern))
            }
        }

        return repoPaths
    }

    private fun getRepositoryConfiguration(repoKey: String): Optional<RepositoryConfiguration> {
        return Optional.ofNullable(repositories.getRepositoryConfiguration(repoKey))
    }

    /*
    Methods below provide low level access to the Artifactory PAPI. No additional verification should be performed.
     */

    fun getItemInfo(repoPath: RepoPath): ItemInfo {
        return repositories.getItemInfo(repoPath)
    }

    open fun getLayoutInfo(repoPath: RepoPath): FileLayoutInfo {
        return repositories.getLayoutInfo(repoPath)
    }

    fun getContent(repoPath: RepoPath): ResourceStreamHandle {
        return repositories.getContent(repoPath)
    }

    open fun getProperties(repoPath: RepoPath): Properties {
        return repositories.getProperties(repoPath)
    }

    fun hasProperty(repoPath: RepoPath, propertyName: String): Boolean {
        return repositories.hasProperty(repoPath, propertyName)
    }

    fun getProperty(repoPath: RepoPath, propertyName: String): String {
        return repositories.getProperty(repoPath, propertyName)
    }

    fun setProperty(repoPath: RepoPath, propertyName: String, value: String) {
        repositories.setProperty(repoPath, propertyName, value)
    }

    fun deleteProperty(repoPath: RepoPath, propertyName: String) {
        repositories.deleteProperty(repoPath, propertyName)
    }

    // TODO: Stop using ArtifactoryPAPIService for this. Use InspectionPropertyService
    fun itemsByProperties(properties: SetMultimap<String, String>, vararg repoKeys: String): List<RepoPath> {
        return searches.itemsByProperties(properties, *repoKeys)
    }

    fun itemsByName(artifactByName: String, vararg repoKeys: String): List<RepoPath> {
        return searches.artifactsByName(artifactByName, *repoKeys)
    }

    fun getArtifactContent(repoPath: RepoPath): ResourceStreamHandle {
        return repositories.getContent(repoPath)
    }
}
