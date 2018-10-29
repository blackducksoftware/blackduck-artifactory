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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

public class BlackDuckPropertyManager {
    final Properties properties;

    public BlackDuckPropertyManager(final Properties properties) {
        this.properties = properties;
    }

    public List<String> getRepositoryKeysFromProperties(final ConfigurationProperty repositoryKeyListProperty, final ConfigurationProperty repositoryKeyCsvProperty) throws IOException {
        final List<String> repositoryKeys;

        final String repositoryKeyListString = getProperty(repositoryKeyListProperty);
        final String repositoryKeyCsvPath = getProperty(repositoryKeyCsvProperty);
        final File repositoryKeyCsvFile = new File(repositoryKeyCsvPath);

        if (repositoryKeyCsvFile.isFile()) {
            repositoryKeys = Files.readAllLines(repositoryKeyCsvFile.toPath()).stream()
                                 .map(line -> line.split(","))
                                 .flatMap(Arrays::stream)
                                 .filter(StringUtils::isNotBlank)
                                 .collect(Collectors.toList());
        } else {
            repositoryKeys = Arrays.asList(repositoryKeyListString.split(","));
        }

        return repositoryKeys;
    }

    public Properties getProperties() {
        return properties;
    }

    public String getProperty(final ConfigurationProperty property) {
        return properties.getProperty(property.getKey());
    }

    public Boolean getBooleanProperty(final ConfigurationProperty property) {
        return BooleanUtils.toBoolean(getProperty(property));
    }
}
