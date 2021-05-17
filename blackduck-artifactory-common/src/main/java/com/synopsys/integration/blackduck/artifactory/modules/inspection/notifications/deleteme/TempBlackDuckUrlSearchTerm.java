package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.deleteme;

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
