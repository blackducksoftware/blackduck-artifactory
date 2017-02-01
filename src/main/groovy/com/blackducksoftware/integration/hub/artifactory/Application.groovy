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
            if (configurationManager.needsBaseConfigUpdate()) {
                configurationManager.updateBaseConfigValues(System.console(), System.out)
            }

            if ('inspect' == configurationProperties.hubArtifactoryMode && configurationManager.needsArtifactoryInspectUpdate()) {
                configurationManager.updateArtifactoryInspectValues(System.console(), System.out)
            } else if ('config-scan' == configurationProperties.hubArtifactoryMode) {
                configurationManager.updateArtifactoryScanValues(System.console(), System.out)
            }
        }

        if (configurationManager.needsBaseConfigUpdate()) {
            logger.error('You have not provided enough configuration to run either an inspection or a scan - please edit the \'config/application.properties\' file directly, or run from a command line to configure the properties.')
        } else if ('inspect' == configurationProperties.hubArtifactoryMode && configurationManager.needsArtifactoryInspectUpdate()) {
            logger.error('You have not provided enough configuration to run an inspection - please edit the \'config/application.properties\' file directly, or run from a command line to configure the properties.')
        } else if ('config-scan' == configurationProperties.hubArtifactoryMode && configurationManager.needsArtifactoryScanUpdate()) {
            logger.error('You have not provided enough configuration to configure the scan plugin - please edit the \'config/application.properties\' file directly, or run from a command line to configure the properties.')
        } else if ('inspect' == configurationProperties.hubArtifactoryMode) {
            artifactoryInspector.performInspect()
        } else if ('config-scan' == configurationProperties.hubArtifactoryMode) {
            artifactoryScanConfigurer.createScanPluginFile()
        }
    }

    @Bean
    ExternalIdentifierBuilder externalIdentifierBuilder() {
        ExternalIdentifierBuilder.create()
    }
}
