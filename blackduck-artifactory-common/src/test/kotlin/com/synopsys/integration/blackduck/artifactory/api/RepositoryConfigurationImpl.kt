package com.synopsys.integration.blackduck.artifactory.api

import org.artifactory.repo.RepositoryConfiguration

class RepositoryConfigurationImpl(
    private val repoLayoutRef: String,
    private val packageType: String
) : RepositoryConfiguration {

    override fun getRepoLayoutRef(): String {
        return this.repoLayoutRef
    }

    override fun isEnableBowerSupport(): Boolean {
        TODO("not implemented")
    }

    override fun isEnableDockerSupport(): Boolean {
        TODO("not implemented")
    }

    override fun isEnableConanSupport(): Boolean {
        TODO("not implemented")
    }

    override fun getNotes(): String {
        TODO("not implemented")
    }

    override fun getIncludesPattern(): String {
        TODO("not implemented")
    }

    override fun isEnableGitLfsSupport(): Boolean {
        TODO("not implemented")
    }

    override fun isEnabledChefSupport(): Boolean {
        TODO("not implemented")
    }

    override fun isForceNugetAuthentication(): Boolean {
        TODO("not implemented")
    }

    override fun getDescription(): String {
        TODO("not implemented")
    }

    override fun isEnableGemsSupport(): Boolean {
        TODO("not implemented")
    }

    override fun getDockerApiVersion(): String {
        TODO("not implemented")
    }

    override fun isEnableDistRepoSupport(): Boolean {
        TODO("not implemented")
    }

    override fun isEnablePypiSupport(): Boolean {
        TODO("not implemented")
    }

    override fun getType(): String {
        TODO("not implemented")
    }

    override fun isDebianTrivialLayout(): Boolean {
        TODO("not implemented")
    }

    override fun isEnablePuppetSupport(): Boolean {
        TODO("not implemented")
    }

    override fun getPackageType(): String {
        return packageType
    }

    override fun isEnableComposerSupport(): Boolean {
        TODO("not implemented")
    }

    override fun getKey(): String {
        TODO("not implemented")
    }

    override fun getExcludesPattern(): String {
        TODO("not implemented")
    }

    override fun isEnableCocoaPodsSupport(): Boolean {
        TODO("not implemented")
    }

    override fun isEnableDebianSupport(): Boolean {
        TODO("not implemented")
    }

    override fun isEnableVagrantSupport(): Boolean {
        TODO("not implemented")
    }

    override fun isEnableNpmSupport(): Boolean {
        TODO("not implemented")
    }

    override fun isEnableNuGetSupport(): Boolean {
        TODO("not implemented")
    }
}