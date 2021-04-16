package com.synopsys.integration.blackduck.artifactory.modules.inspection.externalid.composer.model;

import java.util.List;

import org.artifactory.repo.RepoPath;

public class ComposerJsonResult {
    private final FileNamePieces fileNamePieces;
    private final List<RepoPath> repoPaths;

    public ComposerJsonResult(FileNamePieces fileNamePieces, List<RepoPath> repoPaths) {
        this.fileNamePieces = fileNamePieces;
        this.repoPaths = repoPaths;
    }

    public FileNamePieces getFileNamePieces() {
        return fileNamePieces;
    }

    public List<RepoPath> getRepoPaths() {
        return repoPaths;
    }
}
