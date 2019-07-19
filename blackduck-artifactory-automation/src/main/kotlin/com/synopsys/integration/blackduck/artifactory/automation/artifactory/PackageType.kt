package com.synopsys.integration.blackduck.artifactory.automation.artifactory

interface PackageType {
    val packageType: String
    val remoteUrl: String
    val repoLayoutRef: String?
    val resolver: Resolver?
    val dockerImageTag: String?

    enum class Defaults(override val packageType: String, override val remoteUrl: String, override val repoLayoutRef: String? = null, override val resolver: Resolver? = null, override val dockerImageTag: String? = null) : PackageType {
        BOWER("bower", "https://github.com/", "bower-default", Resolvers.BOWER_RESOLVER, "artifactory-automation-bower"),
        CHEF("chef", "https://supermarket.chef.io"),
        COCOAPODS("cocoapods", "https://github.com/"),
        COMPOSER("composer", "https://github.com/", "composer-default", Resolvers.COMPOSER_RESOLVER, "artifactory-automation-composer"),
        CONAN("conan", "https://conan.bintray.com"),
        CONDA("conda", "https://repo.anaconda.com/pkgs/free"),
        CRAN("cran", "https://cran.r-project.org/", "cran-automation", Resolvers.CRAN_RESOLVER, "artifactory-automation-cran"),
        DEBIAN("debian", "http://archive.ubuntu.com/ubuntu/"),
        // DOCKER("docker", "https://registry-1.docker.io/", false), Unsupported.
        GEMS("gems", "https://rubygems.org/", "gems-automation", Resolvers.GEMS_RESOLVER, "artifactory-automation-gems"),
        // GENERIC("generic", "" ), Needs a remote URL. This PackageType should be created manually.
        // GITLFS("gitlfs", ""), Needs a remote URL. This PackageType should be created manually.
        GO("go", "https://gocenter.io/"),
        GRADLE("gradle", "https://jcenter.bintray.com"),
        HELM("helm", "https://storage.googleapis.com/kubernetes-charts"),
        IVY("ivy", "https://jcenter.bintray.com"),
        MAVEN("maven", "https://jcenter.bintray.com"),
        NPM("npm", "https://registry.npmjs.org", "npm-default", Resolvers.NPM_RESOLVER, "artifactory-automation-npm"),
        NUGET("nuget", "https://www.nuget.org/", "nuget-default", Resolvers.NUGET_RESOLVER, "artifactory-automation-nuget"),
        // OPKG("opkg", ""), Needs a remote URL. This PackageType should be created manually.
        // P2("p2", ""), Needs a remote URL. This PackageType should be created manually.
        PUPPET("puppet", "https://forgeapi.puppetlabs.com/"),
        PYPI("pypi", "https://files.pythonhosted.org", "pypi-automation", Resolvers.PYPI_RESOLVER, "artifactory-automation-pypi"),
        RPM("rpm", "http://mirror.centos.org/centos/"),
        SBT("sbt", "https://jcenter.bintray.com"),
        // VAGRANT("vagrant", ""), In doc, but not creatable via UI as of Artifactory-6.10.3
        VCS("vcs", "https://github.com/"),
        // YUM("yum", ""), In doc, but not creatable via UI as of Artifactory-6.10.3
    }
}