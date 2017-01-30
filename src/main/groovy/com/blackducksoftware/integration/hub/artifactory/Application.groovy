package com.blackducksoftware.integration.hub.artifactory

import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean

import com.blackducksoftware.bdio.model.ExternalIdentifierBuilder
import com.blackducksoftware.integration.hub.artifactory.inspect.ArtifactoryInspector
import com.blackducksoftware.integration.hub.artifactory.scan.ArtifactoryScanConfigurer

@SpringBootApplication
class Application {
    private final Logger logger = LoggerFactory.getLogger(Application.class)

    @Autowired
    ConfigurationManager configurationManager

    @Autowired
    ConfigurationProperties configurationProperties

    @Autowired
    ArtifactoryInspector artifactoryInspector

    @Autowired
    ArtifactoryScanConfigurer artifactoryScanConfigurer

    static void main(final String[] args) {
        new SpringApplicationBuilder(Application.class).logStartupInfo(false).run(args);
    }

    @PostConstruct
    void init() {
        if (null != System.console() && null != System.out) {
            if (configurationManager.needsHubConfigUpdate()) {
                configurationManager.updateHubConfigValues(System.console(), System.out)
            }

            if (configurationManager.needsArtifactoryUpdate()) {
                configurationManager.updateArtifactoryValues(System.console(), System.out)
            }

            if (configurationManager.needsArtifactoryInspectUpdate()) {
                configurationManager.updateArtifactoryInspectValues(System.console(), System.out)
            }
        }

        if (configurationManager.needsHubConfigUpdate() || configurationManager.needsArtifactoryUpdate() || configurationManager.needsArtifactoryInspectUpdate()) {
            logger.error("You have not provided enough configuration to run either an inspection or a scan - please edit the 'config/application.properties' file directly, or run from a command line to configure the properties.")
        } else if ('inspect' == configurationProperties.hubArtifactoryMode) {
            artifactoryInspector.performInspect()
        } else if ('scan_config' == configurationProperties.hubArtifactoryMode) {
            configurationManager.updateArtifactoryScanValues(System.console, System.out)
            artifactoryScanConfigurer.createScanPluginFile()
        }
    }

    @Bean
    ExternalIdentifierBuilder externalIdentifierBuilder() {
        ExternalIdentifierBuilder.create()
    }
}
