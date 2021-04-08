/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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

    public PluginRepoPathFactory(boolean useArtifactory) {
        this.useArtifactory = useArtifactory;
    }

    public RepoPath create(String repoPath) {
        if (useArtifactory) {
            return RepoPathFactory.create(repoPath);
        } else {
            String rpp = repoPath;
            if (rpp == null || rpp.length() == 0) {
                throw new IllegalArgumentException("Path cannot be empty.");
            }
            rpp = PathUtils.trimLeadingSlashes(PathUtils.formatPath(rpp));
            int idx = rpp.indexOf('/');
            String repoKey;
            String path;
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

    public RepoPath create(String repoKey, String path) {
        if (useArtifactory) {
            return RepoPathFactory.create(repoKey, path);
        } else {
            return new PluginRepoPath(repoKey, path);
        }
    }
}
