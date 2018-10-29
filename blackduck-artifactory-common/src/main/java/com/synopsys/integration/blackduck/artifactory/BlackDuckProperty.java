/**
 * hub-artifactory-common
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.artifactory;

public enum BlackDuckProperty implements ConfigurationProperty {
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
    DATE_TIME_PATTERN("date.time.pattern");

    private final String key;

    BlackDuckProperty(final String key) {
        this.key = "blackduck." + key;
    }

    @Override
    public String getKey() {
        return key;
    }
}
