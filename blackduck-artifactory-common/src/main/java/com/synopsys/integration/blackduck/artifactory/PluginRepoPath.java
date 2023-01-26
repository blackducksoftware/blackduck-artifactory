/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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

    public PluginRepoPath(String key, String path) {
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
        Path fullPath = new File(toPath()).toPath().getParent();
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
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PluginRepoPath that = (PluginRepoPath) o;
        return Objects.equals(getRepoKey(), that.getRepoKey()) &&
                   Objects.equals(repoPath, that.repoPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRepoKey(), repoPath);
    }
}
