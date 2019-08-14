package com.synopsys.integration.blackduck.artifactory.automation.docker

import com.synopsys.integration.blackduck.artifactory.automation.artifactory.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.convertToString
import com.synopsys.integration.exception.IntegrationException
import com.synopsys.integration.log.Slf4jIntLogger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit

class DockerService(private val imageTag: String) {
    private val logger = Slf4jIntLogger(LoggerFactory.getLogger(javaClass))

    fun installAndStartArtifactory(version: String, artifactoryPort: String, imageTag: String = this.imageTag): String {
        val artifactoryInstallProcess = installArtifactory(version)
        artifactoryInstallProcess.waitFor(5, TimeUnit.MINUTES)
        if (artifactoryInstallProcess.exitValue() != 0) {
            throw IntegrationException("Failed to install artifactory. Docker returned an exit code of ${artifactoryInstallProcess.exitValue()}")
        }

        val startArtifactoryProcess = initializeArtifactory(version, artifactoryPort, inheritIO = false, containerId = imageTag)
        startArtifactoryProcess.waitFor(3, TimeUnit.MINUTES)
        if (startArtifactoryProcess.exitValue() != 0) {
            throw IntegrationException("Failed to start artifactory. Docker returned an exit code of ${startArtifactoryProcess.exitValue()}")
        }

        return startArtifactoryProcess.inputStream.convertToString().trim()
    }

    private fun installArtifactory(version: String): Process {
        return runCommand("docker", "pull", "docker.bintray.io/jfrog/artifactory-pro:$version")
    }

    private fun initializeArtifactory(version: String, artifactoryPort: String, remoteDebuggingPort: String = "8091", inheritIO: Boolean = true, containerId: String = this.imageTag): Process {
        return runCommand("docker", "run", "--name", containerId, "-d", "-p", "$artifactoryPort:$artifactoryPort", "-p", "$remoteDebuggingPort:$remoteDebuggingPort", "-e",
            "EXTRA_JAVA_OPTIONS=\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$remoteDebuggingPort\"",
            "docker.bintray.io/jfrog/artifactory-pro:$version", inheritIO = inheritIO)
    }

    fun startArtifactory(containerId: String = this.imageTag): Int {
        return runCommand("docker", "start", containerId).waitFor()
    }

    fun stopArtifactory(containerId: String = this.imageTag): Int {
        return runCommand("docker", "stop", containerId).waitFor()
    }

    fun getArtifactoryLogs(containerId: String = this.imageTag): InputStream {
        val process = runCommand("docker", "logs", containerId)
        process.waitFor()
        return process.inputStream
    }

    fun uploadFile(file: File, location: String, containerId: String = this.imageTag): Int {
        return runCommand("docker", "cp", file.canonicalPath, "$containerId:$location").waitFor()
    }

    fun downloadFile(outputFile: File, location: String, containerId: String = this.imageTag): Int {
        return runCommand("docker", "cp", "$containerId:$location", outputFile.canonicalPath).waitFor()
    }

    fun deleteFile(location: String, containerId: String = this.imageTag): Int {
        return runCommand("docker", "exec", "--user=root", containerId, "rm", "-rf", location).waitFor()
    }

    fun chownFile(owner: String, group: String, filePath: String, containerId: String = this.imageTag): Int {
        return runCommand("docker", "exec", "--user=root", containerId, "chown", "-R", "$owner:$group", filePath).waitFor()
    }

    fun chmodFile(permissions: String, filePath: String, containerId: String = this.imageTag): Int {
        return runCommand("docker", "exec", "--user=root", containerId, "chmod", "-R", permissions, filePath).waitFor()
    }

    fun buildTestDockerfile(packageType: PackageType, workingDirectory: File? = null): String {
        val resourcePath = "/${packageType.packageType.toLowerCase()}/Dockerfile"
        val resourceUri = this.javaClass.getResource(resourcePath)?.toURI() ?: throw MissingResourceException("Missing resource $resourcePath", this.javaClass.name, resourcePath)
        val dockerfile = File(resourceUri)
        val actualWorkingDirectory = workingDirectory ?: dockerfile.parentFile

        return buildDockerfile(dockerfile, actualWorkingDirectory, imageTag = packageType.dockerImageTag!!, noCache = true)
    }

    fun buildDockerfile(dockerFile: File, workingDirectory: File, imageTag: String, cleanup: Boolean = true, noCache: Boolean = false): String {
        val command = mutableListOf("docker", "build", "--network=host")
        if (cleanup) command.add("--rm")
        if (noCache) command.add("--no-cache")
        command.addAll(listOf("--tag", imageTag, "--file", dockerFile.absolutePath, workingDirectory.absolutePath))

        runCommand(*command.toTypedArray()).waitFor()
        return imageTag
    }

    fun runDockerImage(imageTag: String, vararg command: String, cleanup: Boolean = true, inheritIO: Boolean = true, directory: File? = null): Int {
        val dockerCommand = mutableListOf("docker", "run", "--network=host")
        if (cleanup) dockerCommand.add("--rm")
        dockerCommand.addAll(listOf(imageTag, *command))

        return runCommand(*dockerCommand.toTypedArray(), inheritIO = inheritIO, directory = directory).waitFor()
    }

    private fun runCommand(vararg command: String, inheritIO: Boolean = true, directory: File? = null): Process {
        logger.info("Running command: ${command.joinToString(separator = " ")}")
        val processBuilder = ProcessBuilder(*command)
        if (directory != null) {
            processBuilder.directory(directory)
        }
        if (inheritIO) {
            processBuilder.inheritIO()
        }
        return processBuilder.start()
    }
}