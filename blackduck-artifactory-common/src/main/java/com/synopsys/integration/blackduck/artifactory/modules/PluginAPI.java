/*
 * blackduck-artifactory-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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
import com.synopsys.integration.blackduck.artifactory.modules.analytics.Analyzable;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.analytics.collector.FeatureAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModule;
import com.synopsys.integration.blackduck.artifactory.modules.policy.PolicyModule;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModule;
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

    public PluginAPI(FeatureAnalyticsCollector featureAnalyticsCollector, ModuleManager moduleManager, ScanModule scanModule, InspectionModule inspectionModule, PolicyModule policyModule, AnalyticsModule analyticsModule) {
        this.featureAnalyticsCollector = featureAnalyticsCollector;
        this.moduleManager = moduleManager;
        this.scanModule = scanModule;
        this.inspectionModule = inspectionModule;
        this.policyModule = policyModule;
        this.analyticsModule = analyticsModule;
    }

    public void setModuleState(TriggerType triggerType, Map<String, List<String>> params) {
        final String functionName = "setModuleState";
        LogUtil.start(logger, functionName, triggerType);
        moduleManager.setModulesState(params);
        featureAnalyticsCollector.logFeatureHit(ModuleManager.class.getName(), functionName);
        LogUtil.finish(logger, functionName, triggerType);
    }

    public void performPluginUpgrades(TriggerType triggerType) {
        runMethod(inspectionModule, triggerType, inspectionModule::performUpgrades);
    }

    public void triggerScan(TriggerType triggerType) {
        runMethod(scanModule, triggerType, scanModule::triggerScan);
    }

    public void addScanPolicyStatus(TriggerType triggerType) {
        runMethod(scanModule, triggerType, scanModule::addPolicyStatus);
    }

    public void performPostScanActions(TriggerType triggerType) {
        runMethod(scanModule, triggerType, scanModule::performPostScanActions);
    }

    public void deleteScanProperties(TriggerType triggerType, Map<String, List<String>> params) {
        runMethod(scanModule, triggerType, () -> scanModule.deleteScanProperties(params));
    }

    public void deleteScanPropertiesFromFailures(TriggerType triggerType, Map<String, List<String>> params) {
        runMethod(scanModule, triggerType, () -> scanModule.deleteScanPropertiesFromFailures(params));
    }

    public void deleteScanPropertiesFromOutOfDate(TriggerType triggerType, Map<String, List<String>> params) {
        runMethod(scanModule, triggerType, () -> scanModule.deleteScanPropertiesFromOutOfDate(params));
    }

    public void reloadBlackDuckScannerDirectory(TriggerType triggerType) {
        runMethod(scanModule, triggerType, () -> scanModule.reloadScannerDirectory(triggerType));
    }

    public String getScanCron() {
        return scanModule.getModuleConfig().getCron();
    }

    public void handleAfterCreateEvent(ItemInfo itemInfo, TriggerType triggerType) {
        runMethod(inspectionModule, triggerType, itemInfo, inspectionModule::handleAfterCreateEvent);
    }

    public void handleAfterCopyEvent(RepoPath targetRepoPath, TriggerType triggerType) {
        runMethod(inspectionModule, triggerType, targetRepoPath, inspectionModule::handleAfterCopyEvent);
    }

    public void handleAfterMoveEvent(RepoPath targetRepoPath, TriggerType triggerType) {
        runMethod(inspectionModule, triggerType, targetRepoPath, inspectionModule::handleAfterMoveEvent);
    }

    public void inspectAllUnknownArtifacts(TriggerType triggerType) {
        runMethod(inspectionModule, triggerType, inspectionModule::inspectAllUnknownArtifacts);
    }

    public void updateMetadata(TriggerType triggerType) {
        runMethod(inspectionModule, triggerType, inspectionModule::updateMetadata);
    }

    public void deleteInspectionProperties(TriggerType triggerType, Map<String, List<String>> params) {
        runMethod(inspectionModule, triggerType, () -> inspectionModule.deleteInspectionProperties(params));
    }

    public void deleteInspectionPropertiesFromOutOfDate(TriggerType triggerType, Map<String, List<String>> params) {
        runMethod(inspectionModule, triggerType, () -> inspectionModule.deleteInspectionPropertiesFromOutOfDate(params));
    }

    public void reinspectFromFailures(TriggerType triggerType) {
        runMethod(inspectionModule, triggerType, (Runnable) inspectionModule::reinspectFromFailures);
    }

    public void reinspectFromFailures(TriggerType triggerType, Map<String, List<String>> params) {
        runMethod(inspectionModule, triggerType, () -> inspectionModule.reinspectFromFailures(params));
    }

    public void performPolicySeverityUpdate(TriggerType triggerType) {
        runMethod(inspectionModule, triggerType, inspectionModule::performPolicySeverityUpdate);
    }

    public void initializeRepositories(TriggerType triggerType) {
        runMethod(inspectionModule, triggerType, inspectionModule::initializeRepositories);
    }

    public String getInspectionCron() {
        return inspectionModule.getModuleConfig().getInspectionCron();
    }

    public String getReinspectCron() {
        return inspectionModule.getModuleConfig().getReinspectCron();
    }

    public void handleBeforeDownloadEventScan(TriggerType triggerType, RepoPath repoPath) {
        runMethod(scanModule, triggerType, repoPath, scanModule::handleBeforeDownloadEvent);
    }

    public void handleBeforeDownloadEventInspection(TriggerType triggerType, RepoPath repoPath) {
        runMethod(inspectionModule, triggerType, repoPath, inspectionModule::handleBeforeDownloadEvent);
    }

    public void handleBeforeDownloadEventPolicy(TriggerType triggerType, RepoPath repoPath) {
        runMethod(policyModule, triggerType, repoPath, policyModule::handleBeforeDownloadEvent);
    }

    public String submitAnalytics(TriggerType triggerType) {
        Boolean success = runMethod(analyticsModule, triggerType, analyticsModule::submitAnalytics);
        return success == null ? "AnalyticsModule disabled" : success.toString();
    }

    /**
     * Below are utility methods to help reuse the code for logging and analytics
     */

    private <T> void runMethod(Module module, TriggerType triggerType, T consumable, Consumer<T> consumer) {
        if (startMethodRun(module, triggerType)) {
            consumer.accept(consumable);
            finishMethodRun(module, triggerType, triggerType);
        }
    }

    private void runMethod(Module module, TriggerType triggerType, Runnable runnable) {
        if (startMethodRun(module, triggerType)) {
            runnable.run();
            finishMethodRun(module, triggerType, triggerType);
        }
    }

    private <T> T runMethod(Module module, TriggerType triggerType, Supplier<T> supplier) {
        T result = null;
        if (startMethodRun(module, triggerType)) {
            result = supplier.get();
            finishMethodRun(module, triggerType, result);
        }

        return result;
    }

    private boolean startMethodRun(Module module, TriggerType triggerType) {
        String methodName = getMethodName();
        String moduleName = module.getModuleConfig().getModuleName();
        boolean isModuleEnabled = module.getModuleConfig().isEnabled();

        if (isModuleEnabled) {
            LogUtil.start(logger, methodName, triggerType);
        } else if (triggerType.equals(TriggerType.REST_REQUEST)) {
            logger.warn(String.format("The %s is disabled! Re-enable it and hit the endpoint again", moduleName));
        } else {
            logger.info(String.format("The %s is disabled. Cannot execute %s", moduleName, methodName));
        }

        return isModuleEnabled;
    }

    private void finishMethodRun(Module module, TriggerType triggerType, Object result) {
        String methodName = getMethodName();
        featureAnalyticsCollector.logFeatureHit(module.getModuleConfig().getModuleName(), methodName, result);
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
