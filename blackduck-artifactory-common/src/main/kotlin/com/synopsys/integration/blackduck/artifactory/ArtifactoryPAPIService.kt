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

import com.google.common.collect.HashMultimap
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

    open fun getPackageType(repoKey: String): String? {
        return getRepositoryConfiguration(repoKey)?.packageType
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
        val isValid = repositories.exists(repoPath) && getRepositoryConfiguration(repoKey) != null

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

    private fun getRepositoryConfiguration(repoKey: String): RepositoryConfiguration? {
        return repositories.getRepositoryConfiguration(repoKey)
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

    open fun hasProperty(repoPath: RepoPath, propertyName: String): Boolean {
        return repositories.hasProperty(repoPath, propertyName)
    }

    open fun getProperty(repoPath: RepoPath, propertyName: String): String? {
        return repositories.getProperty(repoPath, propertyName)
    }

    open fun setProperty(repoPath: RepoPath, propertyName: String, value: String) {
        repositories.setProperty(repoPath, propertyName, value)
    }

    open fun deleteProperty(repoPath: RepoPath, propertyName: String) {
        repositories.deleteProperty(repoPath, propertyName)
    }
    
    open fun itemsByProperties(properties: Map<String, String>, vararg repoKeys: String): List<RepoPath> {
        val setMultimap = HashMultimap.create<String, String>()
        properties.entries.forEach { setMultimap.put(it.key, it.value) }
        return searches.itemsByProperties(setMultimap, *repoKeys)
    }

    open fun itemsByName(artifactByName: String, vararg repoKeys: String): List<RepoPath> {
        return searches.artifactsByName(artifactByName, *repoKeys)
    }

    open fun getArtifactContent(repoPath: RepoPath): ResourceStreamHandle {
        return repositories.getContent(repoPath)
    }
}
