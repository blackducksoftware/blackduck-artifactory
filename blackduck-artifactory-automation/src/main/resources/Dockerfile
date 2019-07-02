# Download base image ubuntu 18.04
FROM ubuntu:18.04

ARG DEBIAN_FRONTEND=noninteractive

# Update Ubuntu Software repository
RUN apt-get update

# Setup npm
RUN apt-get install npm -y
RUN npm install -g npm
# Example npm command: npm install lodash --registry  http://<server:port>/artifactory/api/npm/<remote-repo-key>

# Setup bower
RUN npm install -g bower
RUN npm install -g bower-art-resolver
# Example bower command: bower install lodash --config.registry=http://<server:port>/artifactory/api/bower/<remote-repo-key>

# Setup php-composer
RUN apt-get install php-cli curl -y
RUN php -r "copy('https://getcomposer.org/installer', 'composer-setup.php');"
RUN php -r "if (hash_file('sha384', 'composer-setup.php') === '48e3236262b34d30969dca3c37281b3b4bbe3221bda826ac6a9a62d6444cdb0dcd0615698a5cbe587c3f0fe57a54d8f5') { echo 'Installer verified'; } else { echo 'Installer corrupt'; unlink('composer-setup.php'); } echo PHP_EOL;"
RUN php composer-setup.php
RUN php -r "unlink('composer-setup.php');"
# Example composer command: php composer.phar install
# Example composer (packages.json) file:
#{
#  "name": "synopsys/blackduck-artifactory-automation",
#  "description": "Test for blackduck-artifactory",
#  "prefer-stable": true,
#  "repositories": [
#    {
#      "type" : "composer",
#      "url"  : "http://<server:port>/artifactory/api/composer/<remote-repo-key>"
#    },
#    {
#      "packagist": false
#    }
#  ],
#  "config": {
#    "secure-http" : false
#  },
#  "require": {
#    "psr/log": "1.1.0",
#    "psr/http-message": "1.0.1"
#  }
#}

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