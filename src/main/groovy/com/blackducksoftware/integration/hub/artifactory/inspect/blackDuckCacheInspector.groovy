package com.blackducksoftware.integration.hub.artifactory.inspect

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.StringUtils
import org.artifactory.addon.pypi.*
import org.artifactory.fs.FileLayoutInfo
import org.artifactory.fs.ItemInfo
import org.artifactory.repo.RepoPath
import org.artifactory.repo.RepoPathFactory
import org.artifactory.repo.RepositoryConfiguration
import org.springframework.context.ApplicationContext

import com.blackducksoftware.integration.exception.IntegrationException
import com.blackducksoftware.integration.hub.api.bom.BomImportService
import com.blackducksoftware.integration.hub.artifactory.ArtifactMetaData
import com.blackducksoftware.integration.hub.artifactory.ArtifactMetaDataManager
import com.blackducksoftware.integration.hub.artifactory.BlackDuckProperty
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

@Field String propertiesFileName = "${this.getClass().getSimpleName()}.properties"
@Field Properties inspectorProperties = new Properties()
@Field File propertiesFile = new File("${ctx.artifactoryHome.etcDir}/plugins/lib/${propertiesFileName}.properties")

@Field final String RUBYGEMS_PACKAGE = 'gems'
@Field final String MAVEN_PACKAGE = 'maven'
@Field final String GRADLE_PACKAGE = 'gradle'
@Field final String PYPI_PACKAGE = 'pypi'
@Field final String NUGET_PACKAGE = 'nuget'
@Field final String NPM_PACKAGE = 'npm'

private void loadProperties() {
    if (propertiesFile.exists()) {
        propertiesFile.withInputStream { inspectorProperties.load(it) }
    } else {
        log.error("Black Duck Cache Inspector could not find its properties file. Please ensure that the following file exists: ${propertiesFile.getAbsolutePath()}")
    }
}

executions {
    inspectRepository(httpMethod: 'POST') { params ->
        if (!params.containsKey('repoKey') || !params['repoKey'][0]) {
            message = 'You must provide a repoKey'
            return
        }
        String repoKey = params['repoKey'][0]
        String patterns = getPatternsFromPackageType(repositories.getRepositoryConfiguration(repoKey).getPackageType())

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
            if (repoPathMatchesPackagePatterns(repoPath, packageType)) {
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

private void createHubProject(String repoKey, String patterns) {
    Set repoPaths = new HashSet<>()
    def patternsToFind = patterns.tokenize(',')
    patternsToFind.each {
        repoPaths.addAll(searches.artifactsByName(it, repoKey))
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

private String getPatternsFromPackageType(String packageType) {
    if (RUBYGEMS_PACKAGE.equals(packageType)) {
        return inspectorProperties.get('hub.artifactory.inspect.patterns.rubygems')
    }
    if (MAVEN_PACKAGE.equals(packageType)) {
        return inspectorProperties.get('hub.artifactory.inspect.patterns.maven')
    }
    if (GRADLE_PACKAGE.equals(packageType)) {
        return inspectorProperties.get('hub.artifactory.inspect.patterns.gradle')
    }
    if (PYPI_PACKAGE.equals(packageType)) {
        return inspectorProperties.get('hub.artifactory.inspect.patterns.pypi')
    }
    if (NUGET_PACKAGE.equals(packageType)) {
        return inspectorProperties.get('hub.artifactory.inspect.patterns.nuget')
    }
    if (NPM_PACKAGE.equals(packageType)) {
        return inspectorProperties.get('hub.artifactory.inspect.patterns.npm')
    }

    return ""
}

private boolean repoPathMatchesPackagePatterns(RepoPath repoPath, String packageType) {
    String pattern = getPatternsFromPackageType(packageType)
    String path = repoPath.toPath()

    return FilenameUtils.wildcardMatch(path, pattern)
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

private Dependency createDependency(SimpleBdioFactory simpleBdioFactory, RepoPath repoPath, String packageType) {
    try {
        if (NUGET_PACKAGE.equals(packageType)) {
            org.artifactory.md.Properties properties = repositories.getProperties(repoPath)
            return createNugetDependency(simpleBdioFactory, properties)
        }

        if (NPM_PACKAGE.equals(packageType)) {
            FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath)
            org.artifactory.md.Properties properties = repositories.getProperties(repoPath)
            return createNpmDependency(simpleBdioFactory, fileLayoutInfo, properties)
        }

        if (PYPI_PACKAGE.equals(packageType)) {
            FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath)
            org.artifactory.md.Properties properties = repositories.getProperties(repoPath)
            return createPyPiDependency(simpleBdioFactory, fileLayoutInfo, properties, repoPath)
        }

        if (RUBYGEMS_PACKAGE.equals(packageType)) {
            FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath)
            return createRubygemsDependency(simpleBdioFactory, fileLayoutInfo)
        }

        if (MAVEN_PACKAGE.equals(packageType) || GRADLE_PACKAGE.equals(packageType)) {
            FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath)
            return createMavenDependency(simpleBdioFactory, fileLayoutInfo)
        }
    } catch (Exception e) {
        log.debug("Could not resolve dependency: ${e.getMessage()}")
    }

    return null
}

private Dependency createNugetDependency(SimpleBdioFactory simpleBdioFactory, org.artifactory.md.Properties properties) {
    String name = properties['nuget.id'][0]
    String version = properties['nuget.version'][0]
    return createNameVersionDependency(simpleBdioFactory, Forge.NUGET, name, version)
}

private Dependency createNpmDependency(SimpleBdioFactory simpleBdioFactory, FileLayoutInfo fileLayoutInfo, org.artifactory.md.Properties properties) {
    String name
    String version
    if (properties['npm.name'] && properties['npm.version']) {
        name = properties['npm.name'][0]
        version = properties['npm.version'][0]
    } else {
        name = fileLayoutInfo.module
        version = fileLayoutInfo.baseRevision
    }
    return createNameVersionDependency(simpleBdioFactory, Forge.NPM, name, version)
}

private Dependency createPyPiDependency(SimpleBdioFactory simpleBdioFactory, FileLayoutInfo fileLayoutInfo, org.artifactory.md.Properties properties, RepoPath repoPath) {
    Forge forge = Forge.PYPI
    String name
    String version
    if (properties['pypi.name'] && properties['pypi.version']) {
        name = properties['pypi.name'][0]
        version = properties['pypi.version'][0]
    } else {
        name = fileLayoutInfo.module
        version = fileLayoutInfo.baseRevision
    }
    if (!name || !version) {
        try {
            def pypiService = ((ApplicationContext) ctx).getBean('pypiService')
            def pypiMetadata = pypiService.getPypiMetadata(repoPath)
            name = pypiMetadata.name
            version = pypiMetadata.version
        } catch (Exception e) {
        }
    }
    return createNameVersionDependency(simpleBdioFactory, Forge.PYPI, name, version)
}

private Dependency createRubygemsDependency(SimpleBdioFactory simpleBdioFactory, FileLayoutInfo fileLayoutInfo) {
    String name = fileLayoutInfo.module
    String version = fileLayoutInfo.baseRevision
    return createNameVersionDependency(simpleBdioFactory, Forge.RUBYGEMS, name, version)
}

private Dependency createNameVersionDependency(SimpleBdioFactory simpleBdioFactory, FileLayoutInfo fileLayoutInfo, Forge forge, String name, String version) {
    ExternalId externalId = simpleBdioFactory.createNameVersionExternalId(forge, name, version)
    if (name && version && externalId) {
        return new Dependency(name, version, externalId)
    } else {
        return null
    }
}

private Dependency createMavenDependency(SimpleBdioFactory simpleBdioFactory, FileLayoutInfo fileLayoutInfo) {
    String name = fileLayoutInfo.module
    String version = fileLayoutInfo.baseRevision
    String group = fileLayoutInfo.organization
    ExternalId externalId = simpleBdioFactory.createMavenExternalId(group, name, version)
    if (name && version && externalId) {
        return new Dependency(name, version, externalId)
    }
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

private HubServerConfig createHubServerConfig() {
    HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder()
    hubServerConfigBuilder.setHubUrl(inspectorProperties.get('hub.url'))
    hubServerConfigBuilder.setUsername(inspectorProperties.get('hub.username'))
    hubServerConfigBuilder.setPassword(inspectorProperties.get('hub.password'))
    hubServerConfigBuilder.setTimeout(inspectorProperties.get('hub.timeout'))
    hubServerConfigBuilder.setProxyHost(inspectorProperties.get('hub.proxy.host'))
    hubServerConfigBuilder.setProxyPort(inspectorProperties.get('hub.proxy.port'))
    hubServerConfigBuilder.setProxyUsername(inspectorProperties.get('hub.proxy.username'))
    hubServerConfigBuilder.setProxyPassword(inspectorProperties.get('hub.proxy.password'))
    hubServerConfigBuilder.setAlwaysTrustServerCertificate(Boolean.parseBoolean(inspectorProperties.get('hub.trust.cert')))

    return hubServerConfigBuilder.build()
}

private HubServicesFactory createHubServicesFactory() {
    HubServerConfig hubServerConfig = createHubServerConfig()
    CredentialsRestConnection credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(log))
    new HubServicesFactory(credentialsRestConnection)
}
