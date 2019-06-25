package com.synopsys.integration.blackduck.artifactory.automation.artifactory.api.model

interface PackageType {
    val packageType: String
    val remoteUrl: String

    enum class Defaults(override val packageType: String, override val remoteUrl: String) : PackageType {
        BOWER("bower", "https://github.com/"),
        // CHEF("chef", "https://supermarket.chef.io"), Currently not supported by the plugin
        COCOAPODS("cocoapods", "https://github.com/"),
        COMPOSER("composer", "https://github.com/"),
        // CONAN("conan", "https://conan.bintray.com"), Currently not supported by the plugin
        // CONDA("conda", "https://repo.anaconda.com/pkgs/free"), Currently not supported by the plugin
        CRAN("cran", "https://cran.r-project.org/"),
        // DEBIAN("debian", "http://archive.ubuntu.com/ubuntu/"), Currently not supported by the plugin
        // DOCKER("com/synopsys/integration/blackduck/artifactory/automation/docker", "https://registry-1.docker.io/"), Currently not supported by the plugin
        GEMS("gems", "https://rubygems.org/"),
        // GENERIC("generic", "" ), Needs a remote URL. This PackageType should be created manually.
        // GITLFS("gitlfs", ""), Needs a remote URL. This PackageType should be created manually.
        // GO("go", "https://gocenter.io/"), Currently not supported by the plugin
        GRADLE("gradle", "https://jcenter.bintray.com"),
        // HELM("helm", "https://storage.googleapis.com/kubernetes-charts"), Currently not supported by the plugin
        // IVY("ivy", "https://jcenter.bintray.com"), Currently not supported by the plugin
        MAVEN("maven", "https://jcenter.bintray.com"),
        NPM("npm", "https://registry.npmjs.org"),
        NUGET("nuget", "https://www.nuget.org/"),
        // OPKG("opkg", ""), Needs a remote URL. This PackageType should be created manually.
        // P2("p2", ""), Needs a remote URL. This PackageType should be created manually.
        // PUPPET("puppet", "https://forgeapi.puppetlabs.com/"), Currently not supported by the plugin
        PYPI("pypi", "https://files.pythonhosted.org"),
        // RPM("rpm", "http://mirror.centos.org/centos/"), Currently not supported by the plugin
        // SBT("sbt", "https://jcenter.bintray.com"), Currently not supported by the plugin
        // VAGRANT("vagrant", ""), In doc, but not creatable via UI as of Artifactory-6.10.3
        // VCS("vcs", "https://github.com/"), Currently not supported by the plugin
        // YUM("yum", ""), In doc, but not creatable via UI as of Artifactory-6.10.3
    }
}