# Setup Nuget
FROM microsoft/dotnet:2.2-sdk

RUN mkdir nuget
WORKDIR nuget

RUN dotnet new console --no-restore
RUN dotnet add package <dependency-name> -v <dependency-version> --source http://<server>/artifactory/api/nuget/<repo-key> --no-restore
RUN dotnet restore --source http://<server>/artifactory/api/nuget/<repo-key>
# Example Nuget commands:
#
# Clear all local cache's:
# dotnet nuget locals all -c
#
# Create empty project:
# dotnet new console --no-restore
#
# Add package to project:
# dotnet add package <dependency-name> -v <dependency-version> --source http://<server>/artifactory/api/nuget/<repo-key> --no-restore
#
# Install packages:
# dotnet restore --source http://<server>/artifactory/api/nuget/nuget-automation