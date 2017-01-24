package com.blackducksoftware.integration.hub.artifactory

import javax.annotation.PostConstruct

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.client.support.BasicAuthorizationInterceptor
import org.springframework.web.client.RestTemplate

import com.blackducksoftware.bdio.model.ExternalIdentifierBuilder
import com.blackducksoftware.integration.hub.artifactory.inspect.ArtifactoryInspector

@SpringBootApplication
class Application {
    @Autowired
    ConfigurationManager configurationManager

    @Autowired
    ArtifactoryInspector artifactoryInspector

    static void main(final String[] args) {
        SpringApplication.run(Application.class, args)
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

        if ('inspect' == configurationManager.hubArtifactoryMode) {
            artifactoryInspector.performInspect()
        }
    }

    @Bean
    RestTemplate restTemplate() {
        def restTemplate = new RestTemplate()

        if (StringUtils.isNotBlank(configurationManager.artifactoryUsername) && StringUtils.isNotBlank(configurationManager.artifactoryPassword)) {
            def basicAuthorizationInterceptor = new BasicAuthorizationInterceptor(configurationManager.artifactoryUsername, configurationManager.artifactoryPassword)
            restTemplate.getInterceptors().add(basicAuthorizationInterceptor)
        }

        restTemplate
    }

    @Bean
    ExternalIdentifierBuilder externalIdentifierBuilder() {
        ExternalIdentifierBuilder.create()
    }
}
