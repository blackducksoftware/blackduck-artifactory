# Download base image ubuntu 18.04
FROM ubuntu:18.04

ARG DEBIAN_FRONTEND=noninteractive

# Update Ubuntu Software repository
RUN apt-get update

# Setup pip
RUN apt-get update
RUN apt-get install python3 -y
RUN apt-get install python3-pip -y
RUN pip3 --version
# Example pip command: pip3 install Cycler==0.10.0 --index-url=http://<server:port>/artifactory/api/pypi/<remote-repo-key>/simple