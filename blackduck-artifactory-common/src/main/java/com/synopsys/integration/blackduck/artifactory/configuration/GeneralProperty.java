/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.blackduck.artifactory.configuration;

public enum GeneralProperty implements ConfigurationProperty {
    URL("url"),
    USERNAME("username"),
    PASSWORD("password"),
    API_TOKEN("api.token"),
    TIMEOUT("timeout"),
    PROXY_HOST("proxy.host"),
    PROXY_PORT("proxy.port"),
    PROXY_USERNAME("proxy.username"),
    PROXY_PASSWORD("proxy.password"),
    TRUST_CERT("trust.cert"),
    DATE_TIME_PATTERN("date.time.pattern"),
    DATE_TIME_ZONE("date.time.zone");

    private final String key;

    GeneralProperty(String key) {
        this.key = "blackduck." + key;
    }

    @Override
    public String getKey() {
        return key;
    }
}
