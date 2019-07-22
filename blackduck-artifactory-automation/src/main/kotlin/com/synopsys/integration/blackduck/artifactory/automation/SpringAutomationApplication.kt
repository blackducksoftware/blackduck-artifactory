package com.synopsys.integration.blackduck.artifactory.automation

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

fun main(args: Array<String>) {
    SpringApplication.run(SpringAutomationApplication::class.java, *args)
}

@SpringBootApplication
class SpringAutomationApplication : ApplicationRunner {
    @Autowired
    lateinit var application: Application

    override fun run(args: ApplicationArguments?) {
        println(application.containerId)
    }
}