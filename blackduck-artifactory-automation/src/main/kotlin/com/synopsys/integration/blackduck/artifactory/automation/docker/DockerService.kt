package com.synopsys.integration.blackduck.artifactory.automation.docker

import com.synopsys.integration.blackduck.artifactory.automation.artifactory.PackageType
import com.synopsys.integration.blackduck.artifactory.automation.convertToString
import com.synopsys.integration.exception.IntegrationException
import com.synopsys.integration.log.Slf4jIntLogger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

// TODO: Make DockerService more generic
class DockerService {
    val packageManagerDockerImageTag = "artifactory-automation-pm"

    private val logger = Slf4jIntLogger(LoggerFactory.getLogger(javaClass))

    fun installAndStartArtifactory(version: String, containerName: String, artifactoryPort: String): String {
        val artifactoryInstallProcess = installArtifactory(version)
        artifactoryInstallProcess.waitFor(5, TimeUnit.MINUTES)
        if (artifactoryInstallProcess.exitValue() != 0) {
            throw IntegrationException("Failed to install artifactory. Docker returned an exit code of ${artifactoryInstallProcess.exitValue()}")
        }

        val startArtifactoryProcess = initializeArtifactory(version, containerName, artifactoryPort, inheritIO = false)
        startArtifactoryProcess.waitFor(3, TimeUnit.MINUTES)
        if (startArtifactoryProcess.exitValue() != 0) {
            throw IntegrationException("Failed to start artifactory. Docker returned an exit code of ${startArtifactoryProcess.exitValue()}")
        }

        return startArtifactoryProcess.inputStream.convertToString().trim()
    }

    fun installArtifactory(version: String): Process {
        return runCommand("docker", "pull", "docker.bintray.io/jfrog/artifactory-pro:$version")
    }

    fun initializeArtifactory(version: String, containerName: String, artifactoryPort: String, remoteDebuggingPort: String = "8091", inheritIO: Boolean = true): Process {
        return runCommand("docker", "run", "--name", containerName, "-d", "-p", "$artifactoryPort:$artifactoryPort", "-p", "$remoteDebuggingPort:$remoteDebuggingPort", "-e",
            "EXTRA_JAVA_OPTIONS=\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$remoteDebuggingPort\"",
            "docker.bintray.io/jfrog/artifactory-pro:$version", inheritIO = inheritIO)
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

    fun buildTestDockerfile(packageType: PackageType, workingDirectory: File = File("")): String {
        val resourcePath = "/${packageType.packageType.toLowerCase()}/Dockerfile"
        val resourceUri = this.javaClass.getResource(resourcePath).toURI()
        val dockerfile = File(resourceUri)
        return buildDockerfile(dockerfile, workingDirectory, imageTag = packageType.dockerImageTag!!)
    }

    fun buildDockerfile(dockerFile: File, workingDirectory: File, imageTag: String = packageManagerDockerImageTag, cleanup: Boolean = true): String {
        val cleanupCommand = if (cleanup) "--rm" else ""
        runCommand("docker", "build", cleanupCommand, "--tag", imageTag, "--file", dockerFile.absolutePath, workingDirectory.absolutePath).waitFor()
        return imageTag
    }

    fun runDockerImage(imageTag: String, vararg command: String, cleanup: Boolean = true, inheritIO: Boolean = true, directory: File? = null): Process {
        val cleanupCommand = if (cleanup) "--rm" else ""
        return runCommand("docker", "run", "--network=host", cleanupCommand, imageTag, *command, inheritIO = inheritIO, directory = directory)
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