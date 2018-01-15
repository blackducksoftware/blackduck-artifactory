package com.blackducksoftware.integration.hub.artifactory.inspect

import static com.blackducksoftware.integration.hub.artifactory.SupportedPackageType.*

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.StringUtils
import org.artifactory.addon.pypi.*
import org.artifactory.fs.FileLayoutInfo
import org.artifactory.fs.ItemInfo
import org.artifactory.repo.RepoPath
import org.artifactory.repo.RepoPathFactory
import org.artifactory.repo.RepositoryConfiguration

import com.blackducksoftware.integration.exception.IntegrationException
import com.blackducksoftware.integration.hub.api.bom.BomImportService
import com.blackducksoftware.integration.hub.artifactory.ArtifactMetaData
import com.blackducksoftware.integration.hub.artifactory.ArtifactMetaDataManager
import com.blackducksoftware.integration.hub.artifactory.BlackDuckProperty
import com.blackducksoftware.integration.hub.artifactory.DependencyFactory
import com.blackducksoftware.integration.hub.artifactory.PackageTypePatternManager
import com.blackducksoftware.integration.hub.bdio.SimpleBdioFactory
import com.blackducksoftware.integration.hub.bdio.graph.MutableDependencyGraph
import com.blackducksoftware.integration.hub.bdio.model.Forge
import com.blackducksoftware.integration.hub.bdio.model.SimpleBdioDocument
import com.blackducksoftware.integration.hub.bdio.model.dependency.Dependency
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalId
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder
import com.blackducksoftware.integration.hub.dataservice.component.ComponentDataService
import com.blackducksoftware.integration.hub.dataservice.project.ProjectDataService
import com.blackducksoftware.integration.hub.dataservice.project.ProjectVersionWrapper
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection
import com.blackducksoftware.integration.hub.service.HubService
import com.blackducksoftware.integration.hub.service.HubServicesFactory
import com.blackducksoftware.integration.log.Slf4jIntLogger
import com.blackducksoftware.integration.util.IntegrationEscapeUtil
import com.google.common.collect.HashMultimap
import com.google.common.collect.SetMultimap

import groovy.transform.Field

@Field Properties inspectorProperties = new Properties()
@Field File propertiesFile = new File("${ctx.artifactoryHome.etcDir}/plugins/lib/${this.getClass().getSimpleName()}.properties")
@Field PackageTypePatternManager packageTypePatternManager = new PackageTypePatternManager()
@Field DependencyFactory dependencyFactory = new DependencyFactory()

loadProperties()

executions {
    inspectRepository(httpMethod: 'POST') { params ->
        if (!params.containsKey('repoKey') || !params['repoKey'][0]) {
            message = 'You must provide a repoKey'
            return
        }
        String repoKey = params['repoKey'][0]
        String patterns = packageTypePatternManager.getPattern(repositories.getRepositoryConfiguration(repoKey).getPackageType())

        createHubProject(repoKey, patterns)
    }

    updateInspectedRepository(httpMethod: 'POST') { params ->
        def repoKey = params['repoKey'][0]
        def projectName = getRepoProjectName(repoKey)
        def projectVersionName = getRepoProjectVersionName(repoKey)

        if (params.containsKey('projectName')) {
            projectName = params['projectName'][0]
        }
        if (params.containsKey('projectVersionName')) {
            projectVersionName = params['projectVersionName'][0]
        }

        updateFromHubProject(repoKey, projectName, projectVersionName);
    }

    blackDuckDeleteInspectionProperties(httpMethod: 'POST') { params ->
        log.info('Starting blackDuckDeleteInspectionProperties REST request...')

        def repoKey = params['repoKey'][0]
        deleteInspectionProperties(repoKey)

        log.info('...completed blackDuckDeleteInspectionProperties REST request.')
    }
}

storage {
    afterCreate { ItemInfo item ->
        String repoKey = item.getRepoKey()
        String packageType = repositories.getRepositoryConfiguration(repoKey).getPackageType()
        SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory()

        RepoPath repoPath = item.getRepoPath()
        String projectName = getRepoProjectName(repoKey)
        String projectVersionName = getRepoProjectVersionName(repoKey)

        try {
            String pattern = packageTypePatternManager.getPattern(packageType)
            String path = repoPath.toPath()
            if (FilenameUtils.wildcardMatch(path, pattern)) {
                Dependency dependency = createDependency(simpleBdioFactory, repoPath, packageType)
                if (null != dependency) {
                    addDependencyToProjectVersion(dependency, projectName, projectVersionName)
                    addDependencyProperties(repoPath, dependency)
                }
            }
        } catch (IntegrationException e) {
            log.error("Failed to add ${repoPath} to the project/version ${projectName}/${projectVersionName} in the hub: ${e.getMessage()}")
        } catch (Exception e) {
            log.debug("Unexpected exception: ${e.getMessage()}")
        }
    }
}

private void loadProperties() {
    if (propertiesFile.exists()) {
        propertiesFile.withInputStream { inspectorProperties.load(it) }
        packageTypePatternManager.setPattern(gems, inspectorProperties.get('hub.artifactory.inspect.patterns.rubygems'))
        packageTypePatternManager.setPattern(maven, inspectorProperties.get('hub.artifactory.inspect.patterns.maven'))
        packageTypePatternManager.setPattern(gradle, inspectorProperties.get('hub.artifactory.inspect.patterns.gradle'))
        packageTypePatternManager.setPattern(pypi, inspectorProperties.get('hub.artifactory.inspect.patterns.pypi'))
        packageTypePatternManager.setPattern(nuget, inspectorProperties.get('hub.artifactory.inspect.patterns.nuget'))
        packageTypePatternManager.setPattern(npm, inspectorProperties.get('hub.artifactory.inspect.patterns.npm'))
    } else {
        log.error("Black Duck Cache Inspector could not find its properties file. Please ensure that the following file exists: ${propertiesFile.getAbsolutePath()}")
    }
}

private void createHubProject(String repoKey, String patterns) {
    Set repoPaths = new HashSet<>()
    def patternsToFind = patterns.tokenize(',')
    patternsToFind.each {
        List<RepoPath> searchResults = searches.artifactsByName(it, repoKey)
        repoPaths.addAll(searchResults)
    }

    HubServicesFactory hubServicesFactory = createHubServicesFactory();
    RepositoryConfiguration repositoryConfiguration = repositories.getRepositoryConfiguration(repoKey);
    String packageType = repositoryConfiguration.getPackageType();
    SimpleBdioFactory simpleBdioFactory = new SimpleBdioFactory();
    MutableDependencyGraph mutableDependencyGraph = simpleBdioFactory.createMutableDependencyGraph();

    repoPaths.each { repoPath ->
        Dependency repoPathDependency = createDependency(simpleBdioFactory, repoPath, packageType);
        if (repoPathDependency != null) {
            addDependencyProperties(repoPath, repoPathDependency)
            mutableDependencyGraph.addChildToRoot(repoPathDependency)
        }
    }

    Forge artifactoryForge = new Forge('/', '/', 'artifactory');
    String projectName = getRepoProjectName(repoKey);
    String projectVersionName = getRepoProjectVersionName(repoKey);
    String codeLocationName = "${projectName}/${projectVersionName}/${packageType}"
    ExternalId projectExternalId = simpleBdioFactory.createNameVersionExternalId(artifactoryForge, projectName, projectVersionName)
    SimpleBdioDocument simpleBdioDocument = simpleBdioFactory.createSimpleBdioDocument(codeLocationName, projectName, projectVersionName, projectExternalId, mutableDependencyGraph)

    IntegrationEscapeUtil integrationEscapeUtil = new IntegrationEscapeUtil()
    File bdioFile = new File("/tmp/${integrationEscapeUtil.escapeForUri(codeLocationName)}");
    bdioFile.delete()
    simpleBdioFactory.writeSimpleBdioDocumentToFile(bdioFile, simpleBdioDocument);

    BomImportService bomImportService = hubServicesFactory.createBomImportService();
    bomImportService.importBomFile(bdioFile);
}

private void updateFromHubProject(String repoKey, String projectName, String projectVersionName) {
    HubServicesFactory hubServicesFactory = createHubServicesFactory();
    HubService hubService = hubServicesFactory.createHubService();
    ProjectDataService projectDataService = hubServicesFactory.createProjectDataService();
    ArtifactMetaDataManager artifactMetaDataManager = new ArtifactMetaDataManager();

    ProjectVersionWrapper projectVersionWrapper = projectDataService.getProjectVersion(projectName, projectVersionName);
    List<ArtifactMetaData> artifactMetaDataList = artifactMetaDataManager.getMetaData(hubService, projectVersionWrapper.getProjectVersionView());
    addOriginIdProperties(repoKey, artifactMetaDataList);
}

private void deleteInspectionProperties(String repoKey) {
    BlackDuckProperty.values().each { blackDuckProperty ->
        SetMultimap<String,String> setMultimap = new HashMultimap<>();
        setMultimap.put(blackDuckProperty.getName(), '*');
        List<RepoPath> repoPathsWithProperty = searches.itemsByProperties(setMultimap, repoKey)
        repoPathsWithProperty.each { repoPath ->
            repositories.deleteProperty(repoPath, blackDuckProperty.getName())
        }
    }
}

private String getRepoProjectName(String repoKey) {
    RepoPath repoPath = RepoPathFactory.create(repoKey)
    String projectNameProperty = repositories.getProperty(repoPath, BlackDuckProperty.PROJECT_NAME.getName())
    if (StringUtils.isNotBlank(projectNameProperty)) {
        return projectNameProperty
    }
    return repoKey
}

private String getRepoProjectVersionName(String repoKey) {
    RepoPath repoPath = RepoPathFactory.create(repoKey)
    String projectVersionNameProperty = repositories.getProperty(repoPath, BlackDuckProperty.HUB_PROJECT_VERSION_NAME.getName())
    if (StringUtils.isNotBlank(projectVersionNameProperty)) {
        return projectVersionNameProperty
    }
    return InetAddress.getLocalHost().getHostName();
}

private void addDependencyToProjectVersion(Dependency dependency, String projectName, String projectVersionName) {
    HubServicesFactory hubServicesFactory = createHubServicesFactory();
    ComponentDataService componentDataService = hubServicesFactory.createComponentDataService();
    componentDataService.addComponentToProjectVersion(dependency.externalId, projectName, projectVersionName);
}

private void addDependencyProperties(RepoPath repoPath, Dependency dependency) {
    String hubOriginId = dependency.externalId.createHubOriginId()
    repositories.setProperty(repoPath, BlackDuckProperty.HUB_ORIGIN_ID.getName(), hubOriginId)
    String hubForge = dependency.externalId.forge.getName()
    repositories.setProperty(repoPath, BlackDuckProperty.HUB_FORGE.getName(), hubForge)
}

private void addOriginIdProperties(String repoKey, List<ArtifactMetaData> artifactMetaDataList) {
    artifactMetaDataList.each { artifactMetaData ->
        SetMultimap<String,String> setMultimap = new HashMultimap<>();
        setMultimap.put(BlackDuckProperty.HUB_ORIGIN_ID.getName(), artifactMetaData.originId);
        setMultimap.put(BlackDuckProperty.HUB_FORGE.getName(), artifactMetaData.forge);
        List<RepoPath> artifactsWithOriginId = searches.itemsByProperties(setMultimap, repoKey)
        artifactsWithOriginId.each { repoPath ->
            repositories.setProperty(repoPath, BlackDuckProperty.HIGH_VULNERABILITIES.getName(), Integer.toString(artifactMetaData.highSeverityCount))
            repositories.setProperty(repoPath, BlackDuckProperty.MEDIUM_VULNERABILITIES.getName(), Integer.toString(artifactMetaData.mediumSeverityCount))
            repositories.setProperty(repoPath, BlackDuckProperty.LOW_VULNERABILITIES.getName(), Integer.toString(artifactMetaData.lowSeverityCount))
            repositories.setProperty(repoPath, BlackDuckProperty.POLICY_STATUS.getName(), artifactMetaData.policyStatus)
        }
    }
}

private Dependency createDependency(RepoPath repoPath, String packageType) {
    try {
        FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath);
        org.artifactory.md.Properties properties = repositories.getProperties(repoPath);

        if (nuget.name().equals(packageType)) {
            return dependencyFactory.createNugetDependency(fileLayoutInfo, properties);
        }

        if (npm.name().equals(packageType)) {
            return dependencyFactory.createNpmDependency(fileLayoutInfo, properties);
        }

        if (pypi.name().equals(packageType)) {
            return dependencyFactory.createPyPiDependency(fileLayoutInfo, properties);
        }

        if (gems.name().equals(packageType)) {
            return dependencyFactory.createRubygemsDependency(fileLayoutInfo, properties);
        }

        if (maven.name().equals(packageType) || gradle.name().equals(packageType)) {
            return dependencyFactory.createMavenDependency(fileLayoutInfo, properties);
        }
    } catch (Exception e) {
        log.debug("Could not resolve dependency: ${e.getMessage()}");
    }

    return null
}

private HubServicesFactory createHubServicesFactory() {
    HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder()
    hubServerConfigBuilder.setFromProperties(inspectorProperties)
    HubServerConfig hubServerConfig = hubServerConfigBuilder.build()
    CredentialsRestConnection credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(log))
    new HubServicesFactory(credentialsRestConnection)
}
