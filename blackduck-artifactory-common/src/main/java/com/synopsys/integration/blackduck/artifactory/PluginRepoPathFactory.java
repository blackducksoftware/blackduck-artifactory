/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.artifactory;

import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.jfrog.client.util.PathUtils;

public class PluginRepoPathFactory {
    private final boolean useArtifactory;

    public PluginRepoPathFactory() {
        this(true);
    }

    public PluginRepoPathFactory(final boolean useArtifactory) {
        this.useArtifactory = useArtifactory;
    }

    public RepoPath create(final String repoPath) {
        if (useArtifactory) {
            return RepoPathFactory.create(repoPath);
        } else {
            String rpp = repoPath;
            if (rpp == null || rpp.length() == 0) {
                throw new IllegalArgumentException("Path cannot be empty.");
            }
            rpp = PathUtils.trimLeadingSlashes(PathUtils.formatPath(rpp));
            final int idx = rpp.indexOf('/');
            final String repoKey;
            final String path;
            if (idx < 0) {
                //Just a repo name with no rel path
                repoKey = rpp;
                path = "";
            } else {
                repoKey = rpp.substring(0, idx);
                path = rpp.substring(idx + 1);
            }
            return new PluginRepoPath(repoKey, path);
        }
    }

    public RepoPath create(final String repoKey, final String path) {
        if (useArtifactory) {
            return RepoPathFactory.create(repoKey, path);
        } else {
            return new PluginRepoPath(repoKey, path);
        }
    }
}
