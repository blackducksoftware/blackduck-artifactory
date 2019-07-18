# Download base image ubuntu 18.04
FROM ubuntu:18.04

ARG DEBIAN_FRONTEND=noninteractive

# Update Ubuntu Software repository
RUN apt-get update

# Setup npm
RUN apt-get install npm -y
RUN npm install -g npm
# Example npm command: npm install lodash --registry  http://<server:port>/artifactory/api/npm/<remote-repo-key>

# Setup CRAN (R)
FROM r-base
# Example cran command: r -e "install.packages('fortunes', repos = 'http://<server:port>/artifactory/<remote-repo-key>')"

# Setup rubygems
FROM ruby:2.6
# Example gems command: gem install packaging --version '0.99.35' --source http://<server:port>/artifactory/api/gems/<remote-repo-key>

# Setup Gradle
# Gradle is only needed if we want to test with a project.
# Gradle repositories can have artifacts requested directly through the REST API.
# FROM gradle:5.4-jdk8

# Setup Maven
# Maven is only needed if we want to test with a project.
# Maven repositories can have artifacts requested directly through the REST API.
#FROM maven:3.6-jdk-8

# Setup Nuget
FROM microsoft/dotnet:2.2-sdk
# Example Nuget commands:
#
# Clear all local cache's:
# dotnet nuget locals all -c
#
# Create empty project:
# dotnet new console --no-restore
#
# Add package to project:
# dotnet add package bootstrap -v 4.1.3 --source http://<server:port>/artifactory/api/nuget/<remote-repo-key> --no-restore
#
# Install packages:
# dotnet restore --source http://localhost:8081/artifactory/api/nuget/nuget-automation

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