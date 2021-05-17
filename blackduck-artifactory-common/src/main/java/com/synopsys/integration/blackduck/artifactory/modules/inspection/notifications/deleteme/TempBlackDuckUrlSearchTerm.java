/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.deleteme;

// TODO: This is the copied from the blackduck-common:55.0.0 implementation prior to its release and should be removed when possible. - JM 05/2021
public class TempBlackDuckUrlSearchTerm {
    public static final TempBlackDuckUrlSearchTerm PROJECTS = new TempBlackDuckUrlSearchTerm("projects");
    public static final TempBlackDuckUrlSearchTerm VERSIONS = new TempBlackDuckUrlSearchTerm("versions");
    public static final TempBlackDuckUrlSearchTerm COMPONENTS = new TempBlackDuckUrlSearchTerm("components");
    public static final TempBlackDuckUrlSearchTerm ORIGINS = new TempBlackDuckUrlSearchTerm("origins");

    private final String term;

    public TempBlackDuckUrlSearchTerm(String term) {
        this.term = term;
    }

    public String getTerm() {
        return term;
    }

}
