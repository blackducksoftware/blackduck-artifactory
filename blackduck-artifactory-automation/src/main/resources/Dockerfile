# Download base image ubuntu 18.04
FROM ubuntu:18.04

ARG DEBIAN_FRONTEND=noninteractive

# Update Ubuntu Software repository
RUN apt-get update

# Setup Maven
# Maven is only needed if we want to test with a project.
# Maven repositories can have artifacts requested directly through the REST API.
#FROM maven:3.6-jdk-8

# Add user
#RUN useradd -ms /bin/bash artifactory
#USER nuget
#WORKDIR /home/nuget