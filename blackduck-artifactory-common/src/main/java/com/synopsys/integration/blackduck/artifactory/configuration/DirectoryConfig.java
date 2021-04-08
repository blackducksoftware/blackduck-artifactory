/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.configuration;

import java.io.File;

public class DirectoryConfig {
    private final File homeDirectory;
    private final File etcDirectory;
    private final File pluginsLibDirectory;
    private final File versionFile;
    private final String thirdPartyVersion;
    private final String propertiesFilePathOverride;

    public DirectoryConfig(File homeDirectory, File etcDirectory, File pluginsLibDirectory, File versionFile, String thirdPartyVersion, String propertiesFilePathOverride) {
        this.homeDirectory = homeDirectory;
        this.etcDirectory = etcDirectory;
        this.pluginsLibDirectory = pluginsLibDirectory;
        this.versionFile = versionFile;
        this.thirdPartyVersion = thirdPartyVersion;
        this.propertiesFilePathOverride = propertiesFilePathOverride;
    }

    public static DirectoryConfig createDefault(File homeDirectory, File etcDirectory, File pluginsDirectory, String thirdPartyVersion, String propertiesFilePathOverride) {
        File pluginsLibDirectory = new File(pluginsDirectory, "lib");
        File versionFile = new File(pluginsLibDirectory, "version.txt");

        return new DirectoryConfig(homeDirectory, etcDirectory, pluginsLibDirectory, versionFile, thirdPartyVersion, propertiesFilePathOverride);
    }

    public File getHomeDirectory() {
        return homeDirectory;
    }

    public File getEtcDirectory() {
        return etcDirectory;
    }

    public File getPluginsLibDirectory() {
        return pluginsLibDirectory;
    }

    public File getVersionFile() {
        return versionFile;
    }

    public String getThirdPartyVersion() {
        return thirdPartyVersion;
    }

    public String getPropertiesFilePathOverride() {
        return propertiesFilePathOverride;
    }
}
