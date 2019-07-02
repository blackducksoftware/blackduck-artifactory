package com.synopsys.integration.blackduck.artifactory.automation.artifactory.api

interface PackageType {
    val packageType: String
    val remoteUrl: String
    val automated: Boolean

    enum class Defaults(override val packageType: String, override val remoteUrl: String, override val automated: Boolean = false) : PackageType {
        BOWER("bower", "https://github.com/", true),
        CHEF("chef", "https://supermarket.chef.io"),
        COCOAPODS("cocoapods", "https://github.com/"),
        COMPOSER("composer", "https://github.com/", true),
        CONAN("conan", "https://conan.bintray.com"),
        CONDA("conda", "https://repo.anaconda.com/pkgs/free"),
        CRAN("cran", "https://cran.r-project.org/", true),
        DEBIAN("debian", "http://archive.ubuntu.com/ubuntu/"),
        // DOCKER("docker", "https://registry-1.docker.io/", false), Unsupported.
        GEMS("gems", "https://rubygems.org/", true),
        // GENERIC("generic", "" ), Needs a remote URL. This PackageType should be created manually.
        // GITLFS("gitlfs", ""), Needs a remote URL. This PackageType should be created manually.
        GO("go", "https://gocenter.io/"),
        GRADLE("gradle", "https://jcenter.bintray.com", true),
        HELM("helm", "https://storage.googleapis.com/kubernetes-charts"),
        IVY("ivy", "https://jcenter.bintray.com"),
        MAVEN("maven", "https://jcenter.bintray.com", true),
        NPM("npm", "https://registry.npmjs.org", true),
        NUGET("nuget", "https://www.nuget.org/", true),
        // OPKG("opkg", ""), Needs a remote URL. This PackageType should be created manually.
        // P2("p2", ""), Needs a remote URL. This PackageType should be created manually.
        PUPPET("puppet", "https://forgeapi.puppetlabs.com/"),
        PYPI("pypi", "https://files.pythonhosted.org", true),
        RPM("rpm", "http://mirror.centos.org/centos/"),
        SBT("sbt", "https://jcenter.bintray.com"),
        // VAGRANT("vagrant", ""), In doc, but not creatable via UI as of Artifactory-6.10.3
        VCS("vcs", "https://github.com/"),
        // YUM("yum", ""), In doc, but not creatable via UI as of Artifactory-6.10.3
    }
}