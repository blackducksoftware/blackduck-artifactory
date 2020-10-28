/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2020 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.artifactory.modules;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.LogUtil;
import com.synopsys.integration.blackduck.artifactory.TriggerType;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsModule;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.AnalyticsModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.Analyzable;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.FeatureAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModule;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.policy.PolicyModule;
import com.synopsys.integration.blackduck.artifactory.modules.policy.PolicyModuleConfig;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModule;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModuleConfig;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

/**
 * This class is the public API for the blackDuckPlugin groovy script.
 * Changing this interface should be avoided if possible and any changes
 * made here must be reflected in the blackDuckPlugin.groovy file
 * in hub-artifactory
 */
public class PluginAPI implements Analyzable {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final FeatureAnalyticsCollector featureAnalyticsCollector;
    private final ModuleManager moduleManager;
    private final ScanModule scanModule;
    private final InspectionModule inspectionModule;
    private final PolicyModule policyModule;
    private final AnalyticsModule analyticsModule;
    private final ScanModuleConfig scanModuleConfig;
    private final InspectionModuleConfig inspectionModuleConfig;
    private final PolicyModuleConfig policyModuleConfig;
    private final AnalyticsModuleConfig analyticsModuleConfig;

    public PluginAPI(FeatureAnalyticsCollector featureAnalyticsCollector, ModuleManager moduleManager, ScanModule scanModule, InspectionModule inspectionModule,
        PolicyModule policyModule,
        AnalyticsModule analyticsModule, ScanModuleConfig scanModuleConfig, InspectionModuleConfig inspectionModuleConfig, PolicyModuleConfig policyModuleConfig, AnalyticsModuleConfig analyticsModuleConfig) {
        this.featureAnalyticsCollector = featureAnalyticsCollector;
        this.moduleManager = moduleManager;
        this.scanModule = scanModule;
        this.inspectionModule = inspectionModule;
        this.policyModule = policyModule;
        this.analyticsModule = analyticsModule;
        this.scanModuleConfig = scanModuleConfig;
        this.inspectionModuleConfig = inspectionModuleConfig;
        this.policyModuleConfig = policyModuleConfig;
        this.analyticsModuleConfig = analyticsModuleConfig;
    }

    public static PluginAPI createFromModules(ModuleManager moduleManager, FeatureAnalyticsCollector featureAnalyticsCollector, ScanModule scanModule, InspectionModule inspectionModule,
        PolicyModule policyModule,
        AnalyticsModule analyticsModule) {
        ScanModuleConfig scanModuleConfig = scanModule.getModuleConfig();
        InspectionModuleConfig inspectionModuleConfig = inspectionModule.getModuleConfig();
        PolicyModuleConfig policyModuleConfig = policyModule.getModuleConfig();
        AnalyticsModuleConfig analyticsModuleConfig = analyticsModule.getModuleConfig();

        return new PluginAPI(featureAnalyticsCollector, moduleManager, scanModule, inspectionModule, policyModule, analyticsModule, scanModuleConfig, inspectionModuleConfig, policyModuleConfig, analyticsModuleConfig);
    }

    public void setModuleState(TriggerType triggerType, Map<String, List<String>> params) {
        final String functionName = "setModuleState";
        LogUtil.start(logger, functionName, triggerType);
        moduleManager.setModulesState(params);
        featureAnalyticsCollector.logFeatureHit(ModuleManager.class.getName(), functionName);
        LogUtil.finish(logger, functionName, triggerType);
    }

    public void performPluginUpgrades(TriggerType triggerType) {
        runMethod(inspectionModuleConfig, triggerType, inspectionModule::performUpgrades);
    }

    public void triggerScan(TriggerType triggerType) {
        runMethod(scanModuleConfig, triggerType, scanModule::triggerScan);
    }

    public void addScanPolicyStatus(TriggerType triggerType) {
        runMethod(scanModuleConfig, triggerType, scanModule::addPolicyStatus);
    }

    public void performPostScanActions(TriggerType triggerType) {
        runMethod(scanModuleConfig, triggerType, scanModule::performPostScanActions);
    }

    public void deleteScanProperties(TriggerType triggerType, Map<String, List<String>> params) {
        runMethod(scanModuleConfig, triggerType, () -> scanModule.deleteScanProperties(params));
    }

    public void deleteScanPropertiesFromFailures(TriggerType triggerType, Map<String, List<String>> params) {
        runMethod(scanModuleConfig, triggerType, () -> scanModule.deleteScanPropertiesFromFailures(params));
    }

    public void deleteScanPropertiesFromOutOfDate(TriggerType triggerType, Map<String, List<String>> params) {
        runMethod(scanModuleConfig, triggerType, () -> scanModule.deleteScanPropertiesFromOutOfDate(params));
    }

    public String getScanCron() {
        return scanModuleConfig.getCron();
    }

    public void handleAfterCreateEvent(ItemInfo itemInfo, TriggerType triggerType) {
        runMethod(inspectionModuleConfig, triggerType, itemInfo, inspectionModule::handleAfterCreateEvent);
    }

    public void handleAfterCopyEvent(RepoPath targetRepoPath, TriggerType triggerType) {
        runMethod(inspectionModuleConfig, triggerType, targetRepoPath, inspectionModule::handleAfterCopyEvent);
    }

    public void handleAfterMoveEvent(RepoPath targetRepoPath, TriggerType triggerType) {
        runMethod(inspectionModuleConfig, triggerType, targetRepoPath, inspectionModule::handleAfterMoveEvent);
    }

    public void inspectAllUnknownArtifacts(TriggerType triggerType) {
        runMethod(inspectionModuleConfig, triggerType, inspectionModule::inspectAllUnknownArtifacts);
    }

    public void updateMetadata(TriggerType triggerType) {
        runMethod(inspectionModuleConfig, triggerType, inspectionModule::updateMetadata);
    }

    public void deleteInspectionProperties(TriggerType triggerType, Map<String, List<String>> params) {
        runMethod(inspectionModuleConfig, triggerType, () -> inspectionModule.deleteInspectionProperties(params));
    }

    public void deleteInspectionPropertiesFromOutOfDate(TriggerType triggerType, Map<String, List<String>> params) {
        runMethod(inspectionModuleConfig, triggerType, () -> inspectionModule.deleteInspectionPropertiesFromOutOfDate(params));
    }

    public void reinspectFromFailures(TriggerType triggerType) {
        runMethod(inspectionModuleConfig, triggerType, (Runnable) inspectionModule::reinspectFromFailures);
    }

    public void reinspectFromFailures(TriggerType triggerType, Map<String, List<String>> params) {
        runMethod(inspectionModuleConfig, triggerType, () -> inspectionModule.reinspectFromFailures(params));
    }

    public void performPolicySeverityUpdate(TriggerType triggerType) {
        runMethod(inspectionModuleConfig, triggerType, inspectionModule::performPolicySeverityUpdate);
    }

    public void initializeRepositories(TriggerType triggerType) {
        runMethod(inspectionModuleConfig, triggerType, inspectionModule::initializeRepositories);
    }

    public String getInspectionCron() {
        return inspectionModuleConfig.getInspectionCron();
    }

    public String getReinspectCron() {
        return inspectionModuleConfig.getReinspectCron();
    }

    public void handleBeforeDownloadEventScan(TriggerType triggerType, RepoPath repoPath) {
        runMethod(scanModuleConfig, triggerType, repoPath, scanModule::handleBeforeDownloadEvent);
    }

    public void handleBeforeDownloadEventInspection(TriggerType triggerType, RepoPath repoPath) {
        runMethod(inspectionModuleConfig, triggerType, repoPath, inspectionModule::handleBeforeDownloadEvent);
    }

    public void handleBeforeDownloadEventPolicy(TriggerType triggerType, RepoPath repoPath) {
        runMethod(policyModuleConfig, triggerType, repoPath, policyModule::handleBeforeDownloadEvent);
    }

    public String submitAnalytics(TriggerType triggerType) {
        Boolean success = runMethod(analyticsModuleConfig, triggerType, analyticsModule::submitAnalytics);
        return success == null ? "AnalyticsModule disabled" : success.toString();
    }

    /**
     * Below are utility methods to help reuse the code for logging and analytics
     */

    private <T> void runMethod(ModuleConfig moduleConfig, TriggerType triggerType, T consumable, Consumer<T> consumer) {
        if (startMethodRun(moduleConfig, triggerType)) {
            consumer.accept(consumable);
            finishMethodRun(moduleConfig, triggerType, triggerType);
        }
    }

    private void runMethod(ModuleConfig moduleConfig, TriggerType triggerType, Runnable runnable) {
        if (startMethodRun(moduleConfig, triggerType)) {
            runnable.run();
            finishMethodRun(moduleConfig, triggerType, triggerType);
        }
    }

    private <T> T runMethod(ModuleConfig moduleConfig, TriggerType triggerType, Supplier<T> supplier) {
        T result = null;

        if (startMethodRun(moduleConfig, triggerType)) {
            result = supplier.get();
            finishMethodRun(moduleConfig, triggerType, result);
        }

        return result;
    }

    private boolean startMethodRun(ModuleConfig moduleConfig, TriggerType triggerType) {
        String methodName = getMethodName();
        if (moduleConfig.isEnabled()) {
            LogUtil.start(logger, methodName, triggerType);
        } else if (triggerType.equals(TriggerType.REST_REQUEST)) {
            logger.warn(String.format("The %s is disabled! Re-enable it and hit the endpoint again", moduleConfig.getModuleName()));
        } else {
            logger.info(String.format("The %s is disabled. Cannot execute %s", moduleConfig.getModuleName(), methodName));
        }

        return moduleConfig.isEnabled();
    }

    private void finishMethodRun(ModuleConfig moduleConfig, TriggerType triggerType, Object result) {
        String methodName = getMethodName();
        featureAnalyticsCollector.logFeatureHit(moduleConfig.getModuleName(), methodName, result);
        LogUtil.finish(logger, methodName, triggerType);
    }

    private String getMethodName() {
        final int methodDepth = 4;
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        final String result = "UnknownMethod";

        if (stackTraceElements.length - methodDepth > 0) {
            return stackTraceElements[methodDepth].getMethodName();
        }

        return result;
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return Collections.singletonList(featureAnalyticsCollector);
    }
}
