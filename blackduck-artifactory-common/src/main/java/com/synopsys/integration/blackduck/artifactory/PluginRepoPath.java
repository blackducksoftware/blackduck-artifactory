package com.synopsys.integration.blackduck.artifactory;

import java.io.File;
import java.nio.file.Path;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.artifactory.repo.RepoPath;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
        throw new NotImplementedException();
    }

    @Override
    public boolean isFolder() {
        throw new NotImplementedException();
    }
}
