# Download base image ubuntu 18.04
FROM ubuntu:18.04

ARG DEBIAN_FRONTEND=noninteractive

# Update Ubuntu Software repository
RUN apt-get update

# Setup Gradle
# Gradle is only needed if we want to test with a project.
# Gradle repositories can have artifacts requested directly through the REST API.
# FROM gradle:5.4-jdk8

# Setup Maven
# Maven is only needed if we want to test with a project.
# Maven repositories can have artifacts requested directly through the REST API.
#FROM maven:3.6-jdk-8

# Setup pip
RUN apt-get update
RUN apt-get install python3 -y
RUN apt-get install python3-pip -y
RUN pip3 --version
# Example pip command: pip3 install Cycler==0.10.0 --index-url=http://<server:port>/artifactory/api/pypi/<remote-repo-key>/simple

# Add user
#RUN useradd -ms /bin/bash artifactory
#USER nuget
#WORKDIR /home/nuget