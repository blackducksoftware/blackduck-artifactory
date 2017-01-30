package com.blackducksoftware.integration.hub.artifactory.scan

import java.nio.charset.StandardCharsets

import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperties


@Component
class ArtifactoryScanConfigurer {
    @Autowired
    ConfigurationProperties configurationProperties

    void createScanPluginFile() {
        String scanFileText = this.getClass().getResource('/com/blackducksoftware/integration/hub/artifactory/scan/blackDuckScanForHub.groovy').text

        String stringPrefix = "@Field final String "
        String intPrefix = "@Field final int "

        scanFileText = replaceText(scanFileText, stringPrefix, "HUB_URL", configurationProperties.hubUrl)
        scanFileText = replaceValue(scanFileText, intPrefix, "HUB_TIMEOUT", configurationProperties.hubTimeout)
        scanFileText = replaceText(scanFileText, stringPrefix, "HUB_USERNAME", configurationProperties.hubUsername)
        scanFileText = replaceText(scanFileText, stringPrefix, "HUB_PASSWORD", configurationProperties.hubPassword)
        scanFileText = replaceText(scanFileText, stringPrefix, "HUB_PROXY_HOST", configurationProperties.hubProxyHost)
        scanFileText = replaceValue(scanFileText, intPrefix, "HUB_PROXY_PORT", configurationProperties.hubProxyPort)
        scanFileText = replaceText(scanFileText, stringPrefix, "HUB_PROXY_IGNORED_PROXY_HOSTS", configurationProperties.hubProxyIgnoredProxyHosts)
        scanFileText = replaceText(scanFileText, stringPrefix, "HUB_PROXY_USERNAME", configurationProperties.hubProxyUsername)
        scanFileText = replaceText(scanFileText, stringPrefix, "HUB_PROXY_PASSWORD", configurationProperties.hubProxyPassword)
        scanFileText = replaceText(scanFileText, stringPrefix, "ARTIFACTORY_REPOS_TO_SEARCH", configurationProperties.hubArtifactoryScanReposToSearch)
        scanFileText = replaceText(scanFileText, stringPrefix, "ARTIFACT_NAME_PATTERNS_TO_SCAN", configurationProperties.hubArtifactoryScanNamePatterns)

        FileUtils.write(new File(configurationProperties.hubArtifactoryWorkingDirectoryPath, "blackDuckScanForHub.groovy"), scanFileText, StandardCharsets.UTF_8)
    }

    private String replaceText(String s, String prefix, String label, String newValue) {
        s.replaceFirst("${prefix}${label}=.*\n", "${prefix}${label}=\"${newValue}\"\n")
    }

    private String replaceValue(String s, String prefix, String label, String newValue) {
        s.replaceFirst("${prefix}${label}=.*\n", "${prefix}${label}=${newValue}\n")
    }
}