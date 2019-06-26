package com.synopsys.integration.blackduck.artifactory.automation.docker

import com.synopsys.integration.blackduck.artifactory.automation.convertToString
import com.synopsys.integration.exception.IntegrationException
import com.synopsys.integration.log.Slf4jIntLogger
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

// TODO: Make DockerService more generic
class DockerService {
    private val logger = Slf4jIntLogger(LoggerFactory.getLogger(javaClass))

    fun installAndStartArtifactory(version: String, containerName: String, artifactoryPort: String): String {
        val artifactoryInstallProcess = installArtifactory(version)
        artifactoryInstallProcess.waitFor(5, TimeUnit.MINUTES)
        if (artifactoryInstallProcess.exitValue() != 0) {
            throw IntegrationException("Failed to install artifactory. Docker returned an exit code of ${artifactoryInstallProcess.exitValue()}")
        }

        val startArtifactoryProcess = initializeArtifactory(version, containerName, artifactoryPort, false)
        startArtifactoryProcess.waitFor(3, TimeUnit.MINUTES)
        if (startArtifactoryProcess.exitValue() != 0) {
            throw IntegrationException("Failed to start artifactory. Docker returned an exit code of ${startArtifactoryProcess.exitValue()}")
        }

        return startArtifactoryProcess.inputStream.convertToString().trim()
    }

    fun installArtifactory(version: String): Process {
        return runCommand("docker", "pull", "docker.bintray.io/jfrog/artifactory-pro:$version")
    }

    fun initializeArtifactory(version: String, containerName: String, artifactoryPort: String, inheritIO: Boolean = true): Process {
        return runCommand("docker", "run", "--name", containerName, "-d", "-p",
            "$artifactoryPort:$artifactoryPort", "docker.bintray.io/jfrog/artifactory-pro:$version", inheritIO = inheritIO)
    }

    fun startArtifactory(containerHash: String): Process {
        return runCommand("docker", "start", containerHash)
    }

    fun stopArtifactory(containerHash: String): Process {
        return runCommand("docker", "stop", containerHash)
    }

    fun getArtifactoryLogs(containerHash: String): InputStream {
        val process = runCommand("docker", "logs", containerHash)
        process.waitFor()
        return process.inputStream
    }

    fun uploadFile(containerHash: String, file: File, location: String): Process {
        return runCommand("docker", "cp", file.canonicalPath, "$containerHash:$location")
    }

    fun downloadFile(containerHash: String, outputFile: File, location: String): Process {
        return runCommand("docker", "cp", "$containerHash:$location", outputFile.canonicalPath)
    }

    fun deleteFile(containerHash: String, location: String): Process {
        return runCommand("docker", "exec", "--user=root", containerHash, "rm", "-rf", location)
    }

    fun chownFile(containerHash: String, owner: String, group: String, filePath: String): Process {
        return runCommand("docker", "exec", "--user=root", containerHash, "chown", "-R", "$owner:$group", filePath)
    }

    fun chmodFile(containerHash: String, permissions: String, filePath: String): Process {
        return runCommand("docker", "exec", "--user=root", containerHash, "chmod", "-R", permissions, filePath)
    }

    private fun runCommand(vararg command: String, inheritIO: Boolean = true): Process {
        logger.info("Running command: " + StringUtils.join(command, " "))
        val processBuilder = ProcessBuilder(*command)
        if (inheritIO) {
            processBuilder.inheritIO()
        }
        return processBuilder.start()
    }
}