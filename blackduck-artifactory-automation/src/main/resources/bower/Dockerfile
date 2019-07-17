# Download base image ubuntu 18.04
FROM ubuntu:18.04

ARG DEBIAN_FRONTEND=noninteractive

# Update Ubuntu Software repository
RUN apt-get update

# Setup npm
RUN apt-get install npm -y
RUN npm install -g npm
# Example npm command: npm install lodash --registry --allow-root http://<server:port>/artifactory/api/npm/<remote-repo-key>

# Setup bower
RUN npm install -g bower
RUN npm install -g bower-art-resolver

COPY .bowerrc ./