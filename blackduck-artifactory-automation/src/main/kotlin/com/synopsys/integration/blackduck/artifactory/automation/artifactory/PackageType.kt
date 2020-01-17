package com.synopsys.integration.blackduck.artifactory.automation.artifactory

import com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.ArtifactoryConstants

interface PackageType {
    val packageType: String
    val remoteUrl: String
    val repoLayoutRef: String?
    val resolver: Resolver?
    val dockerImageTag: String?
    val requiresVirtual: Boolean

    enum class Defaults(
            override val packageType: String,
            override val remoteUrl: String,
            override val repoLayoutRef: String = "simple-default",
            override val resolver: Resolver? = null,
            override val dockerImageTag: String = "artifactory-automation-$packageType",
            override val requiresVirtual: Boolean = false
    ) : PackageType {
        BOWER("bower", ArtifactoryConstants.GITHUB_URL, repoLayoutRef = "bower-default", resolver = Resolvers.BOWER_RESOLVER),
        CHEF("chef", "https://supermarket.chef.io"),
        COCOAPODS("cocoapods", ArtifactoryConstants.GITHUB_URL),
        COMPOSER("composer", ArtifactoryConstants.GITHUB_URL, repoLayoutRef = "composer-default", resolver = Resolvers.COMPOSER_RESOLVER),
        CONAN("conan", "https://conan.bintray.com"),
        CONDA("conda", "https://repo.continuum.io/pkgs/main/", resolver = Resolvers.CONDA_RESOLVER),
        CRAN("cran", "https://cran.r-project.org/", repoLayoutRef = "cran-automation", resolver = Resolvers.CRAN_RESOLVER),
        DEBIAN("debian", "http://archive.ubuntu.com/ubuntu/"),
        // DOCKER("docker", "https://registry-1.docker.io/", false), Unsupported.
        GEMS("gems", "https://rubygems.org/", repoLayoutRef = "gems-automation", resolver = Resolvers.GEMS_RESOLVER),
        // GENERIC("generic", "" ), Needs a remote URL. This PackageType should be created manually.
        // GITLFS("gitlfs", ""), Needs a remote URL. This PackageType should be created manually.
        GO("go", "https://gocenter.io/", repoLayoutRef = "go-default", resolver = Resolvers.GO_RESOLVER, requiresVirtual = true),
        GRADLE("gradle", ArtifactoryConstants.JCENTER_URL, repoLayoutRef = "maven-2-default", resolver = Resolvers.GRADLE_RESOLVER),
        HELM("helm", "https://storage.googleapis.com/kubernetes-charts"),
        IVY("ivy", ArtifactoryConstants.JCENTER_URL),
        MAVEN("maven", ArtifactoryConstants.JCENTER_URL, repoLayoutRef = "maven-2-default", resolver = Resolvers.MAVEN_RESOLVER),
        NPM("npm", "https://registry.npmjs.org", repoLayoutRef = "npm-default", resolver = Resolvers.NPM_RESOLVER),
        NUGET("nuget", "https://www.nuget.org/", repoLayoutRef = "nuget-default", resolver = Resolvers.NUGET_RESOLVER),
        // OPKG("opkg", ""), Needs a remote URL. This PackageType should be created manually.
        // P2("p2", ""), Needs a remote URL. This PackageType should be created manually.
        PUPPET("puppet", "https://forgeapi.puppetlabs.com/"),
        PYPI("pypi", "https://files.pythonhosted.org", repoLayoutRef = "pypi-automation", resolver = Resolvers.PYPI_RESOLVER),
        RPM("rpm", "http://mirror.centos.org/centos/"),
        SBT("sbt", ArtifactoryConstants.JCENTER_URL),
        // VAGRANT("vagrant", ""), In doc, but not creatable via UI as of Artifactory-6.10.3
        VCS("vcs", ArtifactoryConstants.GITHUB_URL),
        // YUM("yum", ""), In doc, but not creatable via UI as of Artifactory-6.10.3
    }
}