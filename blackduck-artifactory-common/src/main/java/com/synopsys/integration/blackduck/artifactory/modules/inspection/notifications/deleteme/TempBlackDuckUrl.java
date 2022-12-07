/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications.deleteme;

import java.util.List;

import com.synopsys.integration.rest.HttpUrl;

public class TempBlackDuckUrl {
    private final HttpUrl url;

    public TempBlackDuckUrl(HttpUrl url) {
        this.url = url;
    }

    // TODO: This is the copied from the blackduck-common:55.0.0 implementation prior to its release and should be removed when possible. - JM 05/2021
    public String parseId(List<TempBlackDuckUrlSearchTerm> searchTerms) {
        String searching = url.string();
        int afterLastTermIndex = -1;
        for (TempBlackDuckUrlSearchTerm searchTerm : searchTerms) {
            afterLastTermIndex = searching.indexOf(searchTerm.getTerm(), afterLastTermIndex) + 1;
        }
        int afterFirstSlashIndex = searching.indexOf('/', afterLastTermIndex) + 1;
        int secondSlashIndex = searching.indexOf('/', afterFirstSlashIndex);
        int end = secondSlashIndex > afterFirstSlashIndex ? secondSlashIndex : searching.length();
        searching = searching.substring(afterFirstSlashIndex, end);
        return searching;
    }

}
