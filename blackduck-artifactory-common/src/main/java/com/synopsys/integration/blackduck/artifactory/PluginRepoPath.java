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

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.NotImplementedException;
import org.artifactory.repo.RepoPath;

public class PluginRepoPath implements RepoPath {
    private final String repoKey;
    private final String repoPath;

    public PluginRepoPath(final String key, final String path) {
        this.repoKey = key.replace("/", "");
        if (!path.startsWith("/") && !path.isEmpty()) {
            this.repoPath = "/" + path;
        } else {
            this.repoPath = path;
        }
    }

    @Nonnull
    @Override
    public String getRepoKey() {
        return repoKey;
    }

    @Override
    public String getPath() {
        return repoPath;
    }

    @Override
    public String getId() {
        return repoKey + ":" + repoPath;
    }

    @Override
    public String toPath() {
        return repoKey + repoPath;
    }

    @Override
    public String getName() {
        return new File(toPath()).toPath().getFileName().toString();
    }

    @Nullable
    @Override
    public RepoPath getParent() {
        final Path fullPath = new File(toPath()).toPath().getParent();
        if (isRoot()) {
            return new PluginRepoPath(fullPath.toString(), "");
        } else {
            return new PluginRepoPath(fullPath.getRoot().toString(), fullPath.relativize(fullPath.getRoot()).toString());
        }
    }

    @Override
    public boolean isRoot() {
        return repoPath.isEmpty();
    }

    @Override
    public boolean isFile() {
        throw new NotImplementedException("PluginRepoPath::isFile is not implemented.");
    }

    @Override
    public boolean isFolder() {
        throw new NotImplementedException("PluginRepoPath::isFolder is not implemented.");
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final PluginRepoPath that = (PluginRepoPath) o;
        return Objects.equals(getRepoKey(), that.getRepoKey()) &&
                   Objects.equals(repoPath, that.repoPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRepoKey(), repoPath);
    }
}
