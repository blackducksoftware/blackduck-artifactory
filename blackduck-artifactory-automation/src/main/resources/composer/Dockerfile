# Download base image ubuntu 18.04
FROM ubuntu:18.04

ARG DEBIAN_FRONTEND=noninteractive

# Update Ubuntu Software repository
RUN apt-get update


# Setup php-composer
RUN apt-get install php-cli curl zip git -y
RUN php -r "copy('https://getcomposer.org/installer', 'composer-setup.php');"
RUN php -r "if (hash_file('sha384', 'composer-setup.php') === '48e3236262b34d30969dca3c37281b3b4bbe3221bda826ac6a9a62d6444cdb0dcd0615698a5cbe587c3f0fe57a54d8f5') { echo 'Installer verified'; } else { echo 'Installer corrupt'; unlink('composer-setup.php'); } echo PHP_EOL;"
RUN php composer-setup.php
RUN php -r "unlink('composer-setup.php');"

COPY composer.json /

# Example composer command: php composer.phar install
# Example composer (composer.json) file:
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