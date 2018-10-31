## Overview ##
The BlackDuck Artifactory solution consists a single plugin, the blackDuckPlugin.

The ScannerModule in the blackDuckPlugin, can be configured to routinely scan your artifacts for open source vulnerabilities via the Black Duck Signature Scanner.

The InspectionModuke in the blackDuckPlugin, can be configured to inspect your Artifactory remote repository caches for open source components and populate Black Duck vulnerability and policy metadata on them.

The PolicyModule  in the blackDuckPlugin, can be configured to stop downloads of artifacts that either violate policy or if the artifact is missing policy metadata.

## Build ##
[![Build Status](https://travis-ci.org/blackducksoftware/blackduck-artifactory.svg?branch=master)](https://travis-ci.org/blackducksoftware/blackduck-artifactory)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Black Duck Security Risk](https://copilot.blackducksoftware.com/github/repos/blackducksoftware/blackduck-artifactory/branches/master/badge-risk.svg)](https://copilot.blackducksoftware.com/github/repos/blackducksoftware/blackduck-artifactory/branches/master)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.blackducksoftware.integration%blackduck-artifactory&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.blackducksoftware.integration%blackduck-artifactory)

## Where can I get the latest release? ##
Download the latest from release page: https://github.com/blackducksoftware/blackduck-artifactory/releases

## Documentation ##
All documentation for blackduck-artifactory can be found on our public [Black Duck Confluence](https://blackducksoftware.atlassian.net/wiki/spaces/INTDOCS/pages/47192662/Hub+Artifactory+Plugin)
